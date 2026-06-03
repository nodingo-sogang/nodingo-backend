package nodingo.core.friendship.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.friendship.domain.Friendship;
import nodingo.core.friendship.dto.result.FriendListResult;
import nodingo.core.friendship.dto.result.FriendProfileResult;
import nodingo.core.friendship.repository.FriendshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipQueryService {

    private final FriendshipRepository friendshipRepository;

    public FriendListResult getPendingRequests(Long userId) {
        List<Friendship> pendingRequests = friendshipRepository.fetchPendingRequests(userId);

        List<FriendProfileResult> results = pendingRequests.stream()
                .map(f -> FriendProfileResult.from(f.getRequester()))
                .toList();

        return new FriendListResult(results);
    }

    public FriendListResult getMyAcceptedFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.fetchAcceptedFriends(userId);

        List<FriendProfileResult> results = friendships.stream()
                .map(f -> f.getRequester().getId().equals(userId)
                        ? FriendProfileResult.from(f.getReceiver())
                        : FriendProfileResult.from(f.getRequester()))
                .toList();

        return new FriendListResult(results);
    }
}
