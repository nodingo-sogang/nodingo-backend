
package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.friendship.repository.FriendshipRepository;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.RankingScope;
import nodingo.core.user.domain.User;
import nodingo.core.user.dto.request.RankingQuery;
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

    public RankingListResult getRankingLeaderboard(RankingQuery query) {
        if (query.getScope() == RankingScope.PERSONA) {
            return getPersonaLeaderboard(query);
        }
        return getFriendLeaderboard(query);
    }

    private RankingListResult getPersonaLeaderboard(RankingQuery query) {
        Long myUserId = query.getUserId();
        String redisKey = "ranking:weekly:" + query.getUserOwnPersona().name();

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
            targetUsers = userRepository.fetchLeaderboardByPersona(query.getUserOwnPersona(), 100, 0);
        }

        List<RankingEntryResult> allEntries = generateRankingEntries(targetUsers, myUserId);
        List<RankingEntryResult> top10Entries = allEntries.stream().limit(10).toList();

        final boolean finalIsRedisDataExist = isRedisDataExist;
        RankingEntryResult myEntry = allEntries.stream()
                .filter(RankingEntryResult::isMe)
                .findFirst()
                .orElseGet(() -> finalIsRedisDataExist
                        ? fetchMyStickyEntryFromRedis(myUserId, redisKey)
                        : fetchMyStickyEntryFromDbFallback(myUserId));

        return new RankingListResult("persona", "weekly", top10Entries, myEntry);
    }

    private RankingEntryResult fetchMyStickyEntryFromDbFallback(Long myUserId) {
        User user = userRepository.findById(myUserId)
                .orElseThrow(() -> new UserNotFoundException("내 사용자 정보를 찾을 수 없습니다."));

        return RankingEntryResult.ofFallback(user);
    }

    private RankingListResult getFriendLeaderboard(RankingQuery query) {
        Long myUserId = query.getUserId();

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

    private RankingEntryResult fetchMyStickyEntryFromRedis(Long myUserId, String redisKey) {
        User user = userRepository.findById(myUserId)
                .orElseThrow(() -> new UserNotFoundException("내 사용자 정보를 찾을 수 없습니다."));

        Long redisRank = redisTemplate.opsForZSet().reverseRank(redisKey, myUserId.toString());
        int realRank = (redisRank != null) ? (int) (redisRank + 1) : 999;

        return RankingEntryResult.from(user, realRank, myUserId);
    }
}