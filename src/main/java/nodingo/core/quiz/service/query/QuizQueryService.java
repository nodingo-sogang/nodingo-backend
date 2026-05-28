package nodingo.core.quiz.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.dto.result.QuizListResult;
import nodingo.core.quiz.dto.result.QuizResult;
import nodingo.core.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizQueryService {

    private final QuizRepository quizRepository;

    public QuizListResult getQuizzesByKeyword(Long keywordId) {
        List<Quiz> quizzes = quizRepository.findRecentQuizzes(keywordId);
        return new QuizListResult(quizzes.stream().map(QuizResult::from).toList());
    }
}