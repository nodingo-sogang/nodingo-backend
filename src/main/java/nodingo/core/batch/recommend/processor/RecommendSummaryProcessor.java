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
            // 1. 이미 요약본이 있으면 스킵
            if (recommendKeyword.getSummary() != null && !recommendKeyword.getSummary().isBlank()) {
                return null;
            }

            // 2. 해당 키워드와 연관도 높은 뉴스 Top 3 가져오기
            List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(
                    recommendKeyword.getKeyword().getId(),
                    3
            );

            if (topNewsKeywords.isEmpty()) {
                recommendKeyword.updateSummary("관련 뉴스가 부족하여 요약할 수 없습니다.");
                return recommendKeyword;
            }

            // 3. DTO 규격에 맞게 뉴스 리스트(SummaryNewsInput) 변환
            List<KeywordSummary.SummaryNewsInput> newsInputs = topNewsKeywords.stream()
                    .map(nk -> KeywordSummary.SummaryNewsInput.builder()
                            .newsId(nk.getNews().getId())
                            .title(nk.getNews().getTitle())
                            .body(nk.getNews().getBody())
                            .build())
                    .collect(Collectors.toList());

            // 4. DTO 규격에 맞게 대상 키워드(SummaryKeywordInput) 조립
            KeywordSummary.SummaryKeywordInput keywordInput = KeywordSummary.SummaryKeywordInput.builder()
                    .keywordId(recommendKeyword.getKeyword().getId())
                    .word(recommendKeyword.getKeyword().getWord())
                    .build();

            // 5. 최종 Request DTO 조립
            KeywordSummary.Request aiRequest = KeywordSummary.Request.builder()
                    .userId(recommendKeyword.getUser().getId())
                    .keyword(keywordInput)
                    .relatedNews(newsInputs)
                    .relatedKeywords(Collections.emptyList())
                    .targetDate(recommendKeyword.getTargetDate())
                    .build();

            // 6. AI 브리핑 생성
            KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);

            // 7. 엔티티에 최종 요약본 세팅
            recommendKeyword.updateSummary(aiResponse.getSummary());

            log.info(">>>> [Batch-Processor] keyword '{}' AI briefing created successfully.", recommendKeyword.getKeyword().getWord());

            //  8. 퀴즈 자동 생성
            try {
                Long keywordId = recommendKeyword.getKeyword().getId();

                // 해당 키워드에 대한 퀴즈가 아직 DB에 없는 경우에만 AI에 생성 요청
                if (!quizRepository.existsByKeywordId(keywordId)) {
                    log.info(">>>> [Batch-Processor] Generating quizzes for keyword '{}'", recommendKeyword.getKeyword().getWord());
                    quizGenerationService.generateAndSaveQuizzes(keywordId, aiResponse.getSummary());
                }
            } catch (Exception e) {
                // 퀴즈 생성이 실패하더라도 전체 추천 요약 배치가 중단되지 않도록 방어
                log.error(">>>> [Batch-Processor] Quiz generation failed for keyword '{}'", recommendKeyword.getKeyword().getWord(), e);
            }

            return recommendKeyword;
        };
    }
}