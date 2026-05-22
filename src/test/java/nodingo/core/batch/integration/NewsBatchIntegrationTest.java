package nodingo.core.batch.integration;

import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.ai.dto.newsBatch.NewsBatch;
import nodingo.core.ai.dto.relation.NewsRelationAnalysis;
import nodingo.core.batch.dto.article.ArticleWrapper;
import nodingo.core.batch.dto.article.NewsApiItem;
import nodingo.core.batch.dto.article.NewsApiResponse;
import nodingo.core.batch.service.query.NewsFetchService;
import nodingo.core.global.util.NewsSummarizer;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.service.command.KeywordRecommendService;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.notification.repository.NotificationSettingRepository;
import nodingo.core.notification.service.command.FcmService;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import org.junit.jupiter.api.DisplayName;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;

import org.springframework.data.redis.connection.RedisConnectionFactory;

@Slf4j
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Sql(
        scripts = "classpath:org/springframework/batch/core/schema-postgresql.sql",
        config = @SqlConfig(errorMode = SqlConfig.ErrorMode.CONTINUE_ON_ERROR)
)
class NewsBatchIntegrationTest {

    private static final int EMBEDDING_DIMENSION = 1536;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("dailyNewsJob")
    private Job dailyNewsJob;

    @Autowired
    @Qualifier("hourlyNotificationJob")
    private Job hourlyNotificationJob;

    @MockitoBean
    private NewsFetchService newsFetchService;

    @MockitoBean
    private AiClient aiClient;

    @MockitoBean
    private NewsSummarizer newsSummarizer;

    @MockitoBean
    private KeywordRecommendService keywordRecommendService;

    @MockitoBean
    private FcmService fcmService;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE
                notification_setting,
                recommend_keywords,
                user_interests,
                user_personas,
                users,
                news_relations,
                keyword_relations,
                news_keywords,
                keyword_alias,
                keywords,
                news
            RESTART IDENTITY CASCADE
        """);

        redisConnectionFactory.getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("일일 뉴스 수집(Daily) 후 정각 알림(Hourly) 배치가 순서대로 완벽히 동작한다")
    void dailyAndHourlyJobsShouldCompleteSuccessfully() throws Exception {
        // ==========================================
        // 1. Mocking: 외부 뉴스 API 응답
        // ==========================================
        NewsApiItem news1 = createMockArticle("uri-1", "테슬라 실적 발표", "테슬라의 1분기 실적이...");
        NewsApiItem news2 = createMockArticle("uri-2", "AI 반도체 시장", "엔비디아와 테슬라가 AI 반도체를...");

        NewsApiResponse response = new NewsApiResponse();
        ArticleWrapper wrapper = new ArticleWrapper();
        ReflectionTestUtils.setField(wrapper, "results", List.of(news1, news2));
        ReflectionTestUtils.setField(wrapper, "pages", 1);
        ReflectionTestUtils.setField(response, "articles", wrapper);

        given(newsFetchService.fetchNews(any(), anyInt())).willReturn(response);

        // ==========================================
        // 2. Mocking: AI 서버 - 뉴스 임베딩 & 관계 분석 (Step 1, 2)
        // ==========================================
        given(aiClient.analyzeNewsBatch(any(NewsBatch.Request.class)))
                .willAnswer(invocation -> {
                    NewsBatch.Request request = invocation.getArgument(0);
                    NewsBatch.KeywordAiResult kw1 = NewsBatch.KeywordAiResult.builder()
                            .keywordId(null).word("테슬라").normalizedWord("테슬라")
                            .weight(0.9).isNew(true).embedding(mockEmbedding()).build();

                    List<NewsBatch.NewsAnalysisResult> newsResults = request.getNews().stream()
                            .map(reqNews -> NewsBatch.NewsAnalysisResult.builder()
                                    .newsId(reqNews.getNewsId())
                                    .embedding(mockEmbedding())
                                    .keywords(List.of(kw1))
                                    .build())
                            .toList();

                    return NewsBatch.Response.builder()
                            .newsResults(newsResults)
                            .keywordRelations(List.of(new NewsBatch.KeywordRelationResult(1L, 2L, "키워드A", "키워드B", 0.85)))
                            .build();
                });

        given(newsSummarizer.summarize(any(News.class))).willReturn("AI 요약 본문");
        given(aiClient.buildNewsRelations(any())).willReturn(new NewsRelationAnalysis.Response(List.of(new NewsRelationAnalysis.RelationResult(1L, 2L, 0.92))));

        // ==========================================
        // 3. Data Setup & Mocking: 유저 세팅 (Step 3, 4, 5)
        // ==========================================
        User testUser = User.create("naver", "provider-123", "tester", "테스터", "test@test.com");
        userRepository.save(testUser);

        // 현재 시간에 맞춰 알림 세팅
        NotificationSetting setting = NotificationSetting.create(testUser);
        ReflectionTestUtils.setField(setting, "fcmToken", "test-token");
        ReflectionTestUtils.setField(setting, "notifyHour", LocalDateTime.now().getHour());
        notificationSettingRepository.save(setting);

        given(keywordRecommendService.generateRecommendationForUser(any(), any(), any())).willReturn(Collections.emptyList());
        given(aiClient.summarizeKeywords(any())).willReturn(new KeywordSummary.Response(testUser.getId(), 1L, LocalDate.now(), "브리핑 완료"));

        // ==========================================
        // 4. Execute Job 1: Daily 뉴스 배치 (새벽 5시)
        // ==========================================
        JobParameters dailyParams = new JobParametersBuilder()
                .addLocalDateTime("requestTime", LocalDateTime.now().minusHours(1))
                .addString("runId", UUID.randomUUID().toString())
                .addString("targetDate", LocalDate.now().toString())
                .toJobParameters();

        JobExecution dailyExecution = jobLauncher.run(dailyNewsJob, dailyParams);

        assertThat(dailyExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(newsRepository.count()).isEqualTo(2);

        // ==========================================
        // 5. Execute Job 2: Hourly 알림 배치 (현재 시간 정각)
        // ==========================================
        JobParameters hourlyParams = new JobParametersBuilder()
                .addLocalDateTime("requestTime", LocalDateTime.now())
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters();

        JobExecution hourlyExecution = jobLauncher.run(hourlyNotificationJob, hourlyParams);

        assertThat(hourlyExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // ==========================================
        // 6. Verification: 최종 알림 발송 검증
        // ==========================================
        verify(fcmService, atLeastOnce()).sendMessages(anyList());

        log.info(">>>> [Integration Test] 레디스 초기화 완료. Daily -> Hourly 연동 테스트 성공!");
    }

    private NewsApiItem createMockArticle(String uri, String title, String body) {
        NewsApiItem articleItem = new NewsApiItem();
        ReflectionTestUtils.setField(articleItem, "uri", uri);
        ReflectionTestUtils.setField(articleItem, "url", "https://news.com/" + uri);
        ReflectionTestUtils.setField(articleItem, "lang", "kor");
        ReflectionTestUtils.setField(articleItem, "dateTimePub", LocalDate.now().minusDays(1).atTime(LocalTime.NOON).atOffset(ZoneOffset.UTC).toString());
        ReflectionTestUtils.setField(articleItem, "title", title);
        ReflectionTestUtils.setField(articleItem, "body", body);
        return articleItem;
    }

    private float[] mockEmbedding() {
        float[] embedding = new float[EMBEDDING_DIMENSION];
        embedding[0] = 0.1f;
        embedding[1] = 0.2f;
        embedding[2] = 0.3f;
        return embedding;
    }
}