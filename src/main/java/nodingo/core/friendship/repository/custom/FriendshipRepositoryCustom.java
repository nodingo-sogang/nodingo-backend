package nodingo.core.friendship.repository.custom;

import nodingo.core.friendship.domain.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepositoryCustom {

    boolean existsRelation(Long userAId, Long userBId);
    Optional<Friendship> findPendingRelation(Long requesterId, Long receiverId);

    List<Friendship> fetchPendingRequests(Long userId);

    List<Friendship> fetchAcceptedFriends(Long userId);
}
