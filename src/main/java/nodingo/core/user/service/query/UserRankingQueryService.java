
package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.friendship.repository.FriendshipRepository;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.dto.request.RankingRequest;
import nodingo.core.user.dto.result.RankingEntryResult;
import nodingo.core.user.dto.result.RankingListResult;
import nodingo.core.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRankingQueryService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public RankingListResult getRankingLeaderboard(Long loginUserId, RankingRequest request) {
        User loginUser = userRepository.findAllByIdWithPersonas(List.of(loginUserId)).stream()
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return switch (request.getScope()) {
            case PERSONA -> {
                String userOwnPersonaStr = loginUser.getPersonas().isEmpty() ? "NONE" : loginUser.getPersonas().get(0).name();
                String redisKey = "ranking:weekly:" + userOwnPersonaStr;
                yield getPersonaLeaderboard(loginUser, redisKey);
            }
            case FRIENDS -> getFriendLeaderboard(loginUser);
        };
    }

    private RankingListResult getPersonaLeaderboard(User loginUser, String redisKey) {
        Long myUserId = loginUser.getId();
        List<User> targetUsers;
        boolean isRedisDataExist = false;

        Set<ZSetOperations.TypedTuple<String>> rankedSet = redisTemplate.opsForZSet()
                .reverseRangeWithScores(redisKey, 0, -1);

        if (rankedSet != null && !rankedSet.isEmpty()) {
            isRedisDataExist = true;
            List<Long> userIds = rankedSet.stream()
                    .map(tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())))
                    .toList();

            List<User> dbUsers = userRepository.findAllByIdWithPersonas(userIds);

            Map<Long, User> userMap = new HashMap<>();
            dbUsers.forEach(u -> userMap.put(u.getId(), u));
            targetUsers = userIds.stream().map(userMap::get).filter(Objects::nonNull).toList();
        } else {
            targetUsers = userRepository.fetchLeaderboardByPersona(loginUser.getPersonas().get(0), 100, 0);
        }

        List<RankingEntryResult> allEntries = generateRankingEntries(targetUsers, myUserId);
        List<RankingEntryResult> top10Entries = allEntries.stream().limit(10).toList();

        final boolean finalIsRedisDataExist = isRedisDataExist;
        RankingEntryResult myEntry = allEntries.stream()
                .filter(RankingEntryResult::isMe)
                .findFirst()
                .orElseGet(() -> finalIsRedisDataExist
                        ? fetchMyStickyEntryFromRedis(loginUser, redisKey)
                        : fetchMyStickyEntryFromDbFallback(loginUser));

        return new RankingListResult("persona", "weekly", top10Entries, myEntry);
    }

    private RankingListResult getFriendLeaderboard(User loginUser) {
        Long myUserId = loginUser.getId();

        List<Long> friendIds = new ArrayList<>(friendshipRepository.fetchFriendUserIds(myUserId));
        friendIds.add(myUserId);

        List<User> targetUsers = userRepository.findAllByIdWithPersonas(friendIds).stream()
                .sorted((u1, u2) -> Integer.compare(u2.getWeeklyXp(), u1.getWeeklyXp()))
                .toList();

        List<RankingEntryResult> allEntries = generateRankingEntries(targetUsers, myUserId);
        List<RankingEntryResult> top10Entries = allEntries.stream().limit(10).toList();

        RankingEntryResult myEntry = allEntries.stream()
                .filter(RankingEntryResult::isMe)
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("내 사용자 정보를 찾을 수 없습니다."));

        return new RankingListResult("friends", "weekly", top10Entries, myEntry);
    }

    private List<RankingEntryResult> generateRankingEntries(List<User> users, Long myUserId) {
        List<RankingEntryResult> entries = new ArrayList<>();
        int currentRank = 1;

        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);

            if (i > 0 && u.getWeeklyXp() < users.get(i - 1).getWeeklyXp()) {
                currentRank = i + 1;
            }

            entries.add(RankingEntryResult.from(u, currentRank, myUserId));
        }
        return entries;
    }

    private RankingEntryResult fetchMyStickyEntryFromRedis(User loginUser, String redisKey) {
        Long redisRank = redisTemplate.opsForZSet().reverseRank(redisKey, loginUser.getId().toString());
        int realRank = (redisRank != null) ? (int) (redisRank + 1) : 999;

        return RankingEntryResult.from(loginUser, realRank, loginUser.getId());
    }

    private RankingEntryResult fetchMyStickyEntryFromDbFallback(User loginUser) {
        return RankingEntryResult.ofFallback(loginUser);
    }
}