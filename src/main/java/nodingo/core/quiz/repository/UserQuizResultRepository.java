package nodingo.core.quiz.repository;

import nodingo.core.quiz.domain.UserQuizResult;
import nodingo.core.quiz.repository.custom.UserQuizResultRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizResultRepository extends JpaRepository<UserQuizResult, Long>, UserQuizResultRepositoryCustom {
    boolean existsByUserIdAndQuizId(Long userId, Long quizId);
}
