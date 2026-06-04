package nodingo.core.user.repository;

import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.custom.UserScrapRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserScrapRepository extends JpaRepository<UserScrap, Long>, UserScrapRepositoryCustom {
    boolean existsByUserIdAndNewsId(Long userId, Long newsId);

    Optional<UserScrap> findByUserIdAndNewsId(Long userId, Long newsId);

    @Modifying
    @Query("delete from UserQuizResult u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
