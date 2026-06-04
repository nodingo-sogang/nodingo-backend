package nodingo.core.user.repository;

import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);

    List<UserBadge> findAllByUserId(Long userId);

    @Modifying
    @Query("delete from UserBadge u where u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}