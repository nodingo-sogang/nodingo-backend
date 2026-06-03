package nodingo.core.batch.recommend.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
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

    @Bean
    @StepScope
    public ItemProcessor<RecommendKeyword, RecommendKeyword> recommendSummaryItemProcessor() {
        return recommendKeyword -> {
            if (recommendKeyword.getSummary() != null && !recommendKeyword.getSummary().isBlank()) {
                return null;
            }

            List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(
                    recommendKeyword.getKeyword().getId(),
                    3
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

            KeywordSummary.SummaryKeywordInput keywordInput = KeywordSummary.SummaryKeywordInput.builder()
                    .keywordId(recommendKeyword.getKeyword().getId())
                    .word(recommendKeyword.getKeyword().getWord())
                    .build();

            KeywordSummary.Request aiRequest = KeywordSummary.Request.builder()
                    .keyword(keywordInput)
                    .relatedNews(newsInputs)
                    .relatedKeywords(Collections.emptyList())
                    .targetDate(recommendKeyword.getTargetDate())
                    .build();

            KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);

            recommendKeyword.updateSummary(aiResponse.getSummary());

            log.info(">>>> [Batch-Processor] keyword '{}' AI briefing created successfully.", recommendKeyword.getKeyword().getWord());

            try {
                Long keywordId = recommendKeyword.getKeyword().getId();
                if (!quizRepository.existsByKeywordId(keywordId)) {
                    log.info(">>>> [Batch-Processor] Generating quizzes for keyword '{}'", recommendKeyword.getKeyword().getWord());
                    quizGenerationService.generateAndSaveQuizzes(keywordId, aiResponse.getSummary());
                }
            } catch (Exception e) {
                log.error(">>>> [Batch-Processor] Quiz generation failed for keyword '{}'", recommendKeyword.getKeyword().getWord(), e);
            }

            return recommendKeyword;
        };
    }
}