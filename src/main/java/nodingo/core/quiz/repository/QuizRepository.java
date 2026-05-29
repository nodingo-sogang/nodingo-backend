package nodingo.core.quiz.repository;

import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.repository.custom.QuizRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long>, QuizRepositoryCustom {
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.news WHERE q.keyword.id = :keywordId ORDER BY q.id DESC")
    List<Quiz> findRecentQuizzes(@Param("keywordId") Long keywordId);

    boolean existsByKeywordId(Long keywordId);
}
