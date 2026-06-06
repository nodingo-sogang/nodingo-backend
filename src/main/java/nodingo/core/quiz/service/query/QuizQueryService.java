package nodingo.core.quiz.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.dto.result.QuizListResult;
import nodingo.core.quiz.dto.result.QuizResult;
import nodingo.core.quiz.repository.QuizRepository;
import nodingo.core.quiz.repository.UserQuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizQueryService {

    private final QuizRepository quizRepository;
    private final UserQuizResultRepository userQuizResultRepository;

    public QuizListResult getQuizzesByKeyword(Long userId, Long keywordId) {
        log.info(">>>> [Quiz Query] getQuizzesByKeyword. userId={}, keywordId={}", userId, keywordId);
        List<Quiz> quizzes = quizRepository.findRecentQuizzes(keywordId);
        List<Long> quizIds = quizzes.stream().map(Quiz::getId).toList();
        List<Long> solvedQuizIds = userQuizResultRepository.findSolvedQuizIds(userId, quizIds);

        List<QuizResult> quizResults = quizzes.stream()
                .map(quiz -> {
                    boolean isSolved = solvedQuizIds.contains(quiz.getId());
                    return QuizResult.from(quiz, isSolved);
                })
                .toList();

        log.info(">>>> [Quiz Query] getQuizzesByKeyword result. userId={}, keywordId={}, total={}, solved={}",
                userId, keywordId, quizzes.size(), solvedQuizIds.size());
        return new QuizListResult(quizResults);
    }
}