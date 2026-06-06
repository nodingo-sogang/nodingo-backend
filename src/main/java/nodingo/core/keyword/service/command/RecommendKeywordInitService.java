package nodingo.core.keyword.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordRecommend;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.global.util.BatchDateUtil;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.keyword.service.query.KeywordRecommendQueryService;
import nodingo.core.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendKeywordInitService {

    private final KeywordRecommendQueryService queryService;
    private final KeywordRecommendService commandService;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final NewsKeywordRepository newsKeywordRepository;
    private final AiClient aiClient;
    private final MonitoringMetrics metrics;
    private final ThreadPoolTaskExecutor onboardingExecutor;

    @Transactional
    public void initForNewUser(User user) {
        LocalDate targetDate = BatchDateUtil.getTargetDate();

        if (recommendKeywordRepository.existsByUserIdAndTargetDate(user.getId(), targetDate)) {
            log.info(">>>> [Onboarding] Recommend keywords already exist, skipping. userId={}", user.getId());
            return;
        }

        List<KeywordRecommend.CandidateKeyword> candidates = queryService.getDailyCandidateKeywords(targetDate);

        if (candidates.isEmpty()) {
            log.warn(">>>> [Onboarding] No candidate keywords found for today. userId={}", user.getId());
            return;
        }

        List<RecommendKeyword> recommendations = commandService.generateRecommendationForUser(user, candidates, targetDate);

        log.info(">>>> [Onboarding] Starting parallel summary generation. count={}", recommendations.size());

        List<CompletableFuture<Void>> futures = recommendations.stream()
                .map(rk -> {
                    List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(
                            rk.getKeyword().getId(), 3);

                    if (topNewsKeywords.isEmpty()) {
                        rk.updateSummary("관련 뉴스가 부족하여 요약할 수 없습니다.");
                        return CompletableFuture.completedFuture((Void) null);
                    }

                    List<KeywordSummary.SummaryNewsInput> newsInputs = topNewsKeywords.stream()
                            .map(nk -> KeywordSummary.SummaryNewsInput.builder()
                                    .newsId(nk.getNews().getId())
                                    .title(nk.getNews().getTitle())
                                    .body(nk.getNews().getBody())
                                    .build())
                            .collect(Collectors.toList());

                    KeywordSummary.Request aiRequest = KeywordSummary.Request.builder()
                            .keyword(KeywordSummary.SummaryKeywordInput.builder()
                                    .keywordId(rk.getKeyword().getId())
                                    .word(rk.getKeyword().getWord())
                                    .build())
                            .relatedNews(newsInputs)
                            .relatedKeywords(Collections.emptyList())
                            .targetDate(targetDate)
                            .build();

                    return CompletableFuture.runAsync(() -> {
                        try {
                            metrics.recordAiCall("onboarding.recommendSummary");
                            KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);
                            rk.updateSummary(aiResponse.getSummary());
                            log.info(">>>> [Onboarding] Summary created. keyword={}, thread={}",
                                    rk.getKeyword().getWord(), Thread.currentThread().getName());

                        } catch (AiRateLimitException e) {
                            metrics.recordAiFailure("onboarding.recommendSummary", "RateLimitError");
                            log.error(">>>> [Onboarding] OpenAI rate limit exceeded (429). keyword={}", rk.getKeyword().getWord(), e);
                            rk.updateSummary("요약 생성 중 오류가 발생했습니다.");
                        } catch (Exception e) {
                            log.error(">>>> [Onboarding] Summary failed. keyword={}, error: {}", rk.getKeyword().getWord(), e.getMessage(), e);
                            rk.updateSummary("요약 생성 중 오류가 발생했습니다.");
                        }
                    }, onboardingExecutor);
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        recommendKeywordRepository.saveAll(recommendations);
        log.info(">>>> [Onboarding] Recommend keyword init completed. userId={}, count={}", user.getId(), recommendations.size());
    }
}