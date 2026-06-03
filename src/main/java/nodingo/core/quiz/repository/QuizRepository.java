package nodingo.core.quiz.repository;

import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.repository.custom.QuizRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long>, QuizRepositoryCustom {
    boolean existsByKeywordId(Long keywordId);
}
