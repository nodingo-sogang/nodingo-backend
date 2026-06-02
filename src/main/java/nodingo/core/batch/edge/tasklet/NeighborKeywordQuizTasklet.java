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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
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

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate targetDate = LocalTime.now().isBefore(LocalTime.of(5, 0))
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();

        // 오늘 날짜 키워드 중 뉴스 있고 관계 있는 것들 조회
        List<Keyword> neighborKeywords = keywordRepository.findNeighborKeywordsWithNews(targetDate);

        log.info(">>>> [NeighborQuiz] Total neighbor keywords to process: {}", neighborKeywords.size());

        for (Keyword keyword : neighborKeywords) {
            try {
                // 퀴즈 이미 있으면 스킵
                if (quizRepository.existsByKeywordId(keyword.getId())) {
                    continue;
                }

                // summary 생성
                List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(keyword.getId(), 3);
                if (topNewsKeywords.isEmpty()) continue;

                List<KeywordSummary.SummaryNewsInput> newsInputs = topNewsKeywords.stream()
                        .map(nk -> KeywordSummary.SummaryNewsInput.builder()
                                .newsId(nk.getNews().getId())
                                .title(nk.getNews().getTitle())
                                .body(nk.getNews().getBody())
                                .build())
                        .collect(Collectors.toList());

                KeywordSummary.Request aiRequest = KeywordSummary.Request.builder()
                        .userId(null)
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

                // 퀴즈 생성
                quizGenerationService.generateAndSaveQuizzes(keyword.getId(), summary);
                log.info(">>>> [NeighborQuiz] Generated quizzes for keyword: {}", keyword.getWord());

            } catch (Exception e) {
                log.error(">>>> [NeighborQuiz] Failed for keyword: {}", keyword.getWord(), e);
            }
        }

        return RepeatStatus.FINISHED;
    }
}