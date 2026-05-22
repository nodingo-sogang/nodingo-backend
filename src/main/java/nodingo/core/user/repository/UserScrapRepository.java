package nodingo.core.user.repository;

import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.custom.UserScrapRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserScrapRepository extends JpaRepository<UserScrap, Long>, UserScrapRepositoryCustom {
    boolean existsByUserIdAndNewsId(Long userId, Long newsId);

    Optional<UserScrap> findByUserIdAndNewsId(Long userId, Long newsId);

    @Query("""
    SELECT us FROM UserScrap us
    JOIN FETCH us.recommendKeyword rk
    JOIN FETCH rk.keyword
    WHERE us.user.id = :userId
    AND us.recommendKeyword IS NOT NULL
    ORDER BY us.id DESC
    """)
    List<UserScrap> findKeywordScrapsByUserId(@Param("userId") Long userId, Pageable pageable);
}
