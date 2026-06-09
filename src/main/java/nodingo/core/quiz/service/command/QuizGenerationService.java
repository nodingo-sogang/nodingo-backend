package nodingo.core.quiz.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.ai.dto.quiz.QuizGenerate;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.global.util.DateUtil;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QuizGenerationService {

    private final AiClient aiClient;
    private final KeywordRepository keywordRepository;
    private final NewsKeywordRepository newsKeywordRepository;
    private final QuizRepository quizRepository;
    private final MonitoringMetrics metrics;

    public void generateForOnboarding(Long keywordId, Long userId) {
        if (quizRepository.existsByKeywordId(keywordId)) {
            log.info(">>>> [Quiz Gen] Quiz already exists, skipping onboarding generation. keywordId={}", keywordId);
            return;
        }

        Keyword keyword = getKeywordOrElseThrow(keywordId);

        List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(keywordId, 3);
        if (topNewsKeywords.isEmpty()) {
            log.warn(">>>> [Quiz Gen] Not enough news to generate quiz. keyword={}", keyword.getWord());
            return;
        }

        List<KeywordSummary.SummaryNewsInput> newsInputs = topNewsKeywords.stream()
                .map(nk -> KeywordSummary.SummaryNewsInput.builder()
                        .newsId(nk.getNews().getId())
                        .title(nk.getNews().getTitle())
                        .body(nk.getNews().getBody())
                        .build())
                .collect(Collectors.toList());

        KeywordSummary.Request summaryRequest = KeywordSummary.Request.builder()
                .keyword(KeywordSummary.SummaryKeywordInput.builder()
                        .keywordId(keyword.getId())
                        .word(keyword.getWord())
                        .build())
                .relatedNews(newsInputs)
                .relatedKeywords(Collections.emptyList())
                .targetDate(DateUtil.getApiTargetDate())
                .persona(keyword.getPersona() != null ? keyword.getPersona().name() : null)
                .category(keyword.getParent() != null ? keyword.getParent().getWord() : null)
                .build();

        try {
            log.info(">>>> [Quiz Gen] Requesting AI summary for onboarding. keyword={}", keyword.getWord());
            metrics.recordAiCall("quiz.summarize");
            KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(summaryRequest);
            generateAndSaveQuizzes(keywordId, aiResponse.getSummary());
        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("quiz.summarize", "RateLimitError");
            log.error(">>>> [Quiz Gen] OpenAI rate limit exceeded (429). keywordId={}", keywordId, e);
        } catch (Exception e) {
            metrics.recordAiFailure("quiz.summarize", e.getClass().getSimpleName());
            log.error(">>>> [Quiz Gen] AI summary failed. keywordId={}", keywordId, e);
        }
    }

    public void generateAndSaveQuizzes(Long keywordId, String keywordSummary) {
        Keyword keyword = getKeywordOrElseThrow(keywordId);

        List<News> relatedNews = newsKeywordRepository.findNewsEntitiesByKeywordId(keywordId);

        List<QuizGenerate.RelatedNews> newsInputs = relatedNews.stream()
                .map(news -> QuizGenerate.RelatedNews.builder()
                        .newsId(news.getId())
                        .title(news.getTitle())
                        .body(news.getBody())
                        .url(news.getUrl())
                        .build())
                .toList();

        QuizGenerate.Request aiRequest = QuizGenerate.Request.builder()
                .keywordId(keyword.getId())
                .word(keyword.getWord())
                .summary(keywordSummary)
                .relatedNews(newsInputs)
                .numQuestions(3)
                .build();

        try {
            log.info(">>>> [Quiz Gen] Requesting AI to generate quizzes. keyword={}", keyword.getWord());
            metrics.recordAiCall("quiz.generate");
            QuizGenerate.Response aiResponse = aiClient.generateQuizzes(aiRequest);

            List<Quiz> quizzes = aiResponse.getQuizzes().stream().map(quizInfo -> {
                News sourceNews = null;
                if (quizInfo.getSourceNewsIds() != null && !quizInfo.getSourceNewsIds().isEmpty()) {
                    Long sourceNewsId = quizInfo.getSourceNewsIds().get(0);
                    sourceNews = relatedNews.stream()
                            .filter(n -> n.getId().equals(sourceNewsId))
                            .findFirst().orElse(null);
                }
                return Quiz.create(
                        keyword,
                        sourceNews,
                        quizInfo.getQuestion(),
                        quizInfo.getOptions().get(0),
                        quizInfo.getOptions().get(1),
                        quizInfo.getOptions().get(2),
                        quizInfo.getOptions().get(3),
                        quizInfo.getAnswerIndex()
                );
            }).collect(Collectors.toList());

            quizRepository.saveAll(quizzes);
            log.info(">>>> [Quiz Gen] Successfully saved {} quizzes. keyword={}", quizzes.size(), keyword.getWord());

        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("quiz.generate", "RateLimitError");
            log.error(">>>> [Quiz Gen] OpenAI rate limit exceeded (429). keywordId={}", keywordId, e);
        } catch (Exception e) {
            metrics.recordAiFailure("quiz.generate", e.getClass().getSimpleName());
            log.error(">>>> [Quiz Gen] Quiz generation failed. keywordId={}", keywordId, e);
        }
    }

    private Keyword getKeywordOrElseThrow(Long keywordId) {
        return keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("Keyword not found. keywordId=" + keywordId));
    }
}