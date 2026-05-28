package nodingo.core.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.global.domain.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_badges",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "badge_type"})
        }
)
public class UserBadge extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false)
    private BadgeType badgeType;

    public static UserBadge create(User user, BadgeType badgeType) {
        UserBadge userBadge = new UserBadge();
        userBadge.user = user;
        userBadge.badgeType = badgeType;
        return userBadge;
    }
}