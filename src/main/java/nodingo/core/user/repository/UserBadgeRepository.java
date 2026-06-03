package nodingo.core.user.repository;

import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
    List<UserBadge> findAllByUserId(Long userId);
}