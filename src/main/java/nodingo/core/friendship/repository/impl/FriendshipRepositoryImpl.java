package nodingo.core.friendship.repository.impl;

import nodingo.core.friendship.domain.FriendStatus;
import nodingo.core.friendship.domain.Friendship;

import java.util.List;
import java.util.Optional;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.friendship.domain.QFriendship;
import nodingo.core.friendship.repository.custom.FriendshipRepositoryCustom;
import nodingo.core.user.domain.QUser;

@RequiredArgsConstructor
public class FriendshipRepositoryImpl implements FriendshipRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QFriendship friendship = QFriendship.friendship;
    private final QUser requester = new QUser("requester");
    private final QUser receiver = new QUser("receiver");

    @Override
    public boolean existsRelation(Long userAId, Long userBId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(friendship)
                .where(
                        (friendship.requester.id.eq(userAId).and(friendship.receiver.id.eq(userBId)))
                                .or(friendship.requester.id.eq(userBId).and(friendship.receiver.id.eq(userAId)))
                )
                .fetchFirst();

        return fetchOne != null;
    }

    @Override
    public Optional<Friendship> findPendingRelation(Long requesterId, Long receiverId) {
        Friendship result = queryFactory
                .selectFrom(friendship)
                .where(
                        friendship.requester.id.eq(requesterId),
                        friendship.receiver.id.eq(receiverId),
                        friendship.status.eq(FriendStatus.PENDING)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Friendship> fetchPendingRequests(Long userId) {
        return queryFactory
                .selectFrom(friendship)
                .join(friendship.requester, requester).fetchJoin()
                .join(requester.personas).fetchJoin()
                .where(
                        friendship.receiver.id.eq(userId),
                        friendship.status.eq(FriendStatus.PENDING)
                )
                .fetch();
    }

    @Override
    public List<Friendship> fetchAcceptedFriends(Long userId) {
        return queryFactory
                .selectFrom(friendship)
                .join(friendship.requester, requester).fetchJoin()
                .join(requester.personas).fetchJoin()
                .join(friendship.receiver, receiver).fetchJoin()
                .join(receiver.personas).fetchJoin()
                .where(
                        (friendship.requester.id.eq(userId).or(friendship.receiver.id.eq(userId))),
                        friendship.status.eq(FriendStatus.ACCEPTED)
                )
                .fetch();
    }
}