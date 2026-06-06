package nodingo.core.batch.recommend.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.quiz.repository.QuizRepository;
import nodingo.core.quiz.service.command.QuizGenerationService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendSummaryProcessor {

    private final NewsKeywordRepository newsKeywordRepository;
    private final AiClient aiClient;
    private final QuizGenerationService quizGenerationService;
    private final QuizRepository quizRepository;
    private final MonitoringMetrics metrics;

    @Bean
    @StepScope
    public ItemProcessor<RecommendKeyword, RecommendKeyword> recommendSummaryItemProcessor() {
        return recommendKeyword -> {
            if (recommendKeyword.getSummary() != null && !recommendKeyword.getSummary().isBlank()) {
                log.debug(">>>> [Recommend Summary Processor] Skip: summary already exists. keyword={}",
                        recommendKeyword.getKeyword().getWord());
                return null;
            }

            List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(
                    recommendKeyword.getKeyword().getId(), 3
            );

            if (topNewsKeywords.isEmpty()) {
                recommendKeyword.updateSummary("관련 뉴스가 부족하여 요약할 수 없습니다.");
                return recommendKeyword;
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
                            .keywordId(recommendKeyword.getKeyword().getId())
                            .word(recommendKeyword.getKeyword().getWord())
                            .build())
                    .relatedNews(newsInputs)
                    .relatedKeywords(Collections.emptyList())
                    .targetDate(recommendKeyword.getTargetDate())
                    .build();

            try {
                metrics.recordAiCall("batch.recommendSummary");
                KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);
                recommendKeyword.updateSummary(aiResponse.getSummary());
                log.info(">>>> [Batch Processor] Summary created. keyword={}", recommendKeyword.getKeyword().getWord());

                try {
                    Long keywordId = recommendKeyword.getKeyword().getId();
                    if (!quizRepository.existsByKeywordId(keywordId)) {
                        quizGenerationService.generateAndSaveQuizzes(keywordId, aiResponse.getSummary());
                        log.info(">>>> [Batch Processor] Quiz generated. keyword={}", recommendKeyword.getKeyword().getWord());
                    }
                } catch (AiRateLimitException e) {
                    metrics.recordAiFailure("batch.recommendQuiz", "RateLimitError");
                    log.error(">>>> [Batch Processor] OpenAI rate limit exceeded (429) on quiz. keyword={}", recommendKeyword.getKeyword().getWord(), e);
                } catch (Exception e) {
                    log.error(">>>> [Batch Processor] Quiz generation failed. keyword={}", recommendKeyword.getKeyword().getWord(), e);
                }

            } catch (AiRateLimitException e) {
                metrics.recordAiFailure("batch.recommendSummary", "RateLimitError");
                log.error(">>>> [Batch Processor] OpenAI rate limit exceeded (429). keyword={}", recommendKeyword.getKeyword().getWord(), e);
                return null;
            } catch (Exception e) {
                metrics.recordAiFailure("batch.recommendSummary", e.getClass().getSimpleName());
                log.error(">>>> [Batch Processor] AI summary failed. keyword={}", recommendKeyword.getKeyword().getWord(), e);
                return null;
            }

            return recommendKeyword;
        };
    }
}