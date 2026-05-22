package nodingo.core.batch.recommend.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.NewsKeywordRepository;
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

            return recommendKeyword;
        };
    }
}