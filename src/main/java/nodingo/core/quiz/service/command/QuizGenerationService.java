package nodingo.core.quiz.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.keyword.KeywordSummary;
import nodingo.core.ai.dto.quiz.QuizGenerate;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.NewsKeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
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

    // 🌟 [추가] 온보딩 전용: 요약본을 먼저 생성하고 기존 퀴즈 생성 로직을 호출합니다.
    @Transactional
    public void generateForOnboarding(Long keywordId, Long userId) {
        // 1. 이미 해당 키워드의 퀴즈가 존재하면 불필요한 AI 호출 방지
        if (quizRepository.existsByKeywordId(keywordId)) {
            log.info(">>>> [Quiz Gen] 퀴즈가 이미 존재하여 온보딩 생성을 스킵합니다. KeywordId: {}", keywordId);
            return;
        }

        Keyword keyword = getKeywordOrElseThrow(keywordId);

        // 2. 키워드 요약 생성 (연관된 최상위 뉴스 3개 추출)
        List<NewsKeyword> topNewsKeywords = newsKeywordRepository.findTopByKeywordId(keywordId, 3);
        if (topNewsKeywords.isEmpty()) {
            log.warn(">>>> [Quiz Gen] 관련 뉴스가 부족하여 요약/퀴즈 생성을 중단합니다. Keyword: {}", keyword.getWord());
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
                .targetDate(LocalDate.now())
                .persona(keyword.getPersona() != null ? keyword.getPersona().name() : null)
                .category(keyword.getParent() != null ? keyword.getParent().getWord() : null)
                .build();

        log.info(">>>> [Quiz Gen] Requesting AI to summarize Keyword for Onboarding: {}", keyword.getWord());
        KeywordSummary.Response aiResponse = aiClient.summarizeKeywords(summaryRequest);
        String summary = aiResponse.getSummary();

        // 3. 받아온 요약본으로 기존 퀴즈 생성 로직 재사용
        generateAndSaveQuizzes(keywordId, summary);
    }

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