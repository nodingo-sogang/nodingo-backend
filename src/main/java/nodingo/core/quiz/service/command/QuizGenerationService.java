package nodingo.core.quiz.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.quiz.QuizGenerate;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGenerationService {

    private final AiClient aiClient;
    private final KeywordRepository keywordRepository;
    private final NewsKeywordRepository newsKeywordRepository;
    private final QuizRepository quizRepository;

    @Transactional
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

        log.info(">>>> [Quiz Gen] Requesting AI to generate quizzes for Keyword: {}", keyword.getWord());
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
        log.info(">>>> [Quiz Gen] Successfully saved {} quizzes for Keyword: {}", quizzes.size(), keyword.getWord());
    }

    private Keyword getKeywordOrElseThrow(Long keywordId) {
        return keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("키워드를 찾을 수 없습니다."));
    }
}