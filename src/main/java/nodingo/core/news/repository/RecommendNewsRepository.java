package nodingo.core.news.repository;

import nodingo.core.news.domain.RecommendNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendNewsRepository extends JpaRepository<RecommendNews, Long> {
    @Modifying
    @Query("delete from UserQuizResult u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
