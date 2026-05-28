package nodingo.core.quiz.repository.custom;

import nodingo.core.quiz.domain.Quiz;

import java.util.List;

public interface QuizRepositoryCustom {
    List<Quiz> findRecentQuizzes(Long keywordId);
}
