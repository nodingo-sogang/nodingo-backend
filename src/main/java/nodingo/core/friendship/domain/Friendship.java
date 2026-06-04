package nodingo.core.friendship.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.global.domain.BaseTimeEntity;
import nodingo.core.user.domain.User;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "friendships",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"low_user_id", "high_user_id"})
        }
)
public class Friendship extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "low_user_id", nullable = false)
    private Long lowUserId;

    @Column(name = "high_user_id", nullable = false)
    private Long highUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status = FriendStatus.PENDING;

    public static Friendship createRequest(User requester, User receiver) {
        Friendship friendship = new Friendship();
        friendship.requester = requester;
        friendship.receiver = receiver;
        friendship.lowUserId = Math.min(requester.getId(), receiver.getId());
        friendship.highUserId = Math.max(requester.getId(), receiver.getId());
        friendship.status = FriendStatus.PENDING;
        return friendship;
    }

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
    }
}