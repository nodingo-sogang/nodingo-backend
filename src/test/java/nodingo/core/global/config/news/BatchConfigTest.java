package nodingo.core.global.config.news;

import com.google.firebase.messaging.Message;
import nodingo.core.batch.dto.article.NewsApiItem;
import nodingo.core.batch.listener.MyJobListener;
import nodingo.core.global.config.batch.BatchConfig;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.news.domain.News;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; // 🌟 Executor 임포트 빼고 ThreadPoolTaskExecutor 추가!
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BatchConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private MyJobListener myJobListener;

    // 🌟 Mock 가짜 객체 타입도 ThreadPoolTaskExecutor로 완벽 호환되게 변경!
    @Mock private ThreadPoolTaskExecutor batchQuizExecutor;

    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ResourcelessTransactionManager noDbTransactionManager;
    @Mock private Tasklet relationTasklet;
    @Mock private Tasklet neighborKeywordQuizTasklet;

    @Mock private ItemReader<NewsApiItem> newsApiReader;
    @Mock private ItemProcessor<NewsApiItem, News> newsProcessor;
    @Mock private ItemWriter<News> newsAiWriter;

    @Mock private ItemReader<User> userReader;
    @Mock private ItemProcessor<User, List<RecommendKeyword>> recommendProcessor;
    @Mock private ItemWriter<List<RecommendKeyword>> recommendWriter;

    @Mock private ItemReader<RecommendKeyword> recommendSummaryReader;
    @Mock private ItemProcessor<RecommendKeyword, RecommendKeyword> recommendSummaryProcessor;
    @Mock private ItemWriter<RecommendKeyword> recommendSummaryWriter;

    @Mock private ItemReader<NotificationSetting> notificationReader;
    @Mock private ItemProcessor<NotificationSetting, Message> notificationProcessor;
    @Mock private ItemWriter<Message> fcmBatchWriter;

    private BatchConfig config;

    @BeforeEach
    void setUp() {
        // 이제 대형 스레드 풀이 완벽한 타입으로 주입됩니다.
        config = new BatchConfig(jobRepository, myJobListener, batchQuizExecutor);
    }

    @Test
    @DisplayName("일배치와 시배치 Job이 각각 정상적으로 분리되어 생성된다")
    void jobAndStepCreationTest() {
        Step newsStep = config.newsStep(newsApiReader, newsProcessor, newsAiWriter, noDbTransactionManager);
        Step relationStep = config.relationStep(relationTasklet, transactionManager);
        Step recommendStep = config.recommendStep(userReader, recommendProcessor, recommendWriter, transactionManager);
        Step summaryStep = config.recommendSummaryStep(recommendSummaryReader, recommendSummaryProcessor, recommendSummaryWriter, transactionManager);
        Step neighborKeywordQuizStep = config.neighborKeywordQuizStep(neighborKeywordQuizTasklet, transactionManager);
        Step notificationStep = config.notificationStep(notificationReader, notificationProcessor, fcmBatchWriter, transactionManager);

        Job dailyJob = config.dailyNewsJob(newsStep, relationStep, recommendStep, summaryStep, neighborKeywordQuizStep);
        Job hourlyJob = config.hourlyNotificationJob(notificationStep);

        assertThat(dailyJob.getName()).isEqualTo("dailyNewsJob");
        SimpleJob simpleDailyJob = (SimpleJob) dailyJob;
        assertThat(simpleDailyJob.getStepNames()).containsExactly("newsStep", "relationStep", "recommendStep", "recommendSummaryStep", "neighborKeywordQuizStep");

        assertThat(hourlyJob.getName()).isEqualTo("hourlyNotificationJob");
        SimpleJob simpleHourlyJob = (SimpleJob) hourlyJob;
        assertThat(simpleHourlyJob.getStepNames()).containsExactly("notificationStep");
    }
}