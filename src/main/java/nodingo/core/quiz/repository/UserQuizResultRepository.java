package nodingo.core.quiz.repository;

import nodingo.core.quiz.domain.UserQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizResultRepository extends JpaRepository<UserQuizResult, Long> {
    boolean existsByUserIdAndQuizId(Long userId, Long quizId);
}
