package nodingo.core.batch.edge.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.quiz.repository.QuizRepository;
import nodingo.core.quiz.service.command.QuizGenerationService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NeighborKeywordQuizTasklet implements Tasklet {

    private final KeywordRepository keywordRepository;
    private final QuizRepository quizRepository;
    private final QuizGenerationService quizGenerationService;
    private final AiClient aiClient;
    private final NewsKeywordRepository newsKeywordRepository;

    private final ThreadPoolTaskExecutor batchQuizExecutor;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate targetDate = LocalTime.now().isBefore(LocalTime.of(5, 0))
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();

        List<Keyword> neighborKeywords = keywordRepository.findNeighborKeywordsWithNews(targetDate);
        log.info(">>>> [NeighborQuiz] 병렬 처리 엔진 가동 - 총 이웃 키워드 개수: {}", neighborKeywords.size());

        if (neighborKeywords.isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        List<CompletableFuture<Void>> futures = neighborKeywords.stream()
                .map(keyword -> CompletableFuture.runAsync(() -> {
                    try {
                        if (quizRepository.existsByKeywordId(keyword.getId())) {
                            return;
                        }

                        List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(keyword.getId(), 3);
                        if (topNewsKeywords.isEmpty()) return;

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
                        String summary = aiResponse.getSummary();

                        quizGenerationService.generateAndSaveQuizzes(keyword.getId(), summary);

                        log.info(">>>> [NeighborQuiz] 퀴즈 생성 완료! Keyword: '{}' (Thread: {})",
                                keyword.getWord(), Thread.currentThread().getName());

                    } catch (Exception e) {
                        log.error(">>>> [NeighborQuiz] 키워드 처리 중 예외 발생. Keyword: {}", keyword.getWord(), e);
                    }
                }, batchQuizExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info(">>>> [NeighborQuiz] 모든 이웃 키워드 퀴즈 생성 병렬 처리 완료!");
        return RepeatStatus.FINISHED;
    }
}