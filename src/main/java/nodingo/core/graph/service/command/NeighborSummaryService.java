package nodingo.core.graph.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.global.util.BatchDateUtil;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.quiz.repository.QuizRepository;
import nodingo.core.quiz.service.command.QuizGenerationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NeighborSummaryService {

    private final KeywordRepository keywordRepository;
    private final NewsKeywordRepository newsKeywordRepository;
    private final QuizGenerationService quizGenerationService;
    private final QuizRepository quizRepository;
    private final AiClient aiClient;
    private final RecommendKeywordRepository recommendKeywordRepository;

    public Map<Long, String> generateSummarySync(List<Long> keywordIds) {
        LocalDate targetDate = BatchDateUtil.getTargetDate();

        Map<Long, String> summaryMap = new HashMap<>();

        for (Long keywordId : keywordIds) {
            try {
                Keyword keyword = keywordRepository.findById(keywordId).orElse(null);
                if (keyword == null) continue;

                String summary = null;

                Optional<RecommendKeyword> recommendKeyword = recommendKeywordRepository
                        .findByKeywordIdAndTargetDate(keywordId, targetDate);

                if (recommendKeyword.isPresent() && recommendKeyword.get().getSummary() != null
                        && !recommendKeyword.get().getSummary().isBlank()) {
                    summary = recommendKeyword.get().getSummary();
                } else {
                    List<NewsKeyword> topNewsKeywords = newsKeywordRepository
                            .findTopByKeywordId(keywordId, 3);
                    if (topNewsKeywords.isEmpty()) continue;

                    List<KeywordSummary.SummaryNewsInput> newsInputs = topNewsKeywords.stream()
                            .map(nk -> KeywordSummary.SummaryNewsInput.builder()
                                    .newsId(nk.getNews().getId())
                                    .title(nk.getNews().getTitle())
                                    .body(nk.getNews().getBody())
                                    .build())
                            .collect(Collectors.toList());

                    KeywordSummary.Request aiRequest = KeywordSummary.Request.builder()
                            .keyword(KeywordSummary.SummaryKeywordInput.builder()
                                    .keywordId(keyword.getId())
                                    .word(keyword.getWord())
                                    .build())
                            .relatedNews(newsInputs)
                            .relatedKeywords(Collections.emptyList())
                            .targetDate(targetDate)
                            .persona(keyword.getPersona() != null ? keyword.getPersona().name() : null)
                            .category(keyword.getParent() != null ? keyword.getParent().getWord() : null)
                            .build();

                    KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);
                    summary = aiResponse.getSummary();
                }

                if (summary != null) {
                    summaryMap.put(keywordId, summary);
                }

            } catch (Exception e) {
                log.error(">>>> [NeighborSync] Summary failed for keywordId: {}", keywordId, e);
            }
        }

        return summaryMap;
    }

    @Async
    @Transactional
    public void generateQuizAsync(List<Long> keywordIds, Map<Long, String> summaryMap) {
        for (Long keywordId : keywordIds) {
            try {
                String summary = summaryMap.get(keywordId);
                if (summary == null) continue;

                if (!quizRepository.existsByKeywordId(keywordId)) {
                    quizGenerationService.generateAndSaveQuizzes(keywordId, summary);
                    log.info(">>>> [NeighborAsync] Quiz generated for keywordId: {}", keywordId);
                }
            } catch (Exception e) {
                log.error(">>>> [NeighborAsync] Quiz failed for keywordId: {}", keywordId, e);
            }
        }
    }
}