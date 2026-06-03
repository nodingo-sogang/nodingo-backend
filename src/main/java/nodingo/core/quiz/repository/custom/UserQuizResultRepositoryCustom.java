package nodingo.core.quiz.repository.custom;

import java.util.List;

public interface UserQuizResultRepositoryCustom {
    List<Long> findSolvedQuizIds(Long userId, List<Long> quizIds);
}
