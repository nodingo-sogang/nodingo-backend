package nodingo.core.batch.edge.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.global.util.BatchDateUtil;
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
    private final MonitoringMetrics metrics;

    private final ThreadPoolTaskExecutor batchQuizExecutor;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate targetDate = BatchDateUtil.getTargetDate();

        List<Keyword> neighborKeywords = keywordRepository.findNeighborKeywordsWithNews(targetDate);
        log.info(">>>> [NeighborQuiz] Started. total neighbor keywords={}", neighborKeywords.size());

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

                        metrics.recordAiCall("batch.neighborQuiz.summarize");
                        KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(aiRequest);
                        String summary = aiResponse.getSummary();

                        quizGenerationService.generateAndSaveQuizzes(keyword.getId(), summary);
                        log.info(">>>> [NeighborQuiz] Quiz generated. keyword={}, thread={}",
                                keyword.getWord(), Thread.currentThread().getName());

                    } catch (AiRateLimitException e) {
                        metrics.recordAiFailure("batch.neighborQuiz.summarize", "RateLimitError");
                        log.error(">>>> [NeighborQuiz] OpenAI rate limit exceeded (429). keyword={}", keyword.getWord(), e);
                    } catch (Exception e) {
                        log.error(">>>> [NeighborQuiz] Failed. keyword={}", keyword.getWord(), e);
                    }
                }, batchQuizExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info(">>>> [NeighborQuiz] All parallel processing completed.");
        return RepeatStatus.FINISHED;
    }
}