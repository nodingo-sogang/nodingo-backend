
package nodingo.core.user.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.friendship.repository.FriendshipRepository;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.request.RankingRequest;
import nodingo.core.user.dto.result.CachedUserInfo;
import nodingo.core.user.dto.result.RankingEntryResult;
import nodingo.core.user.dto.result.RankingListResult;
import nodingo.core.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRankingQueryService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String USER_INFO_HASH_KEY = "user:ranking:info";

    public RankingListResult getRankingLeaderboard(Long loginUserId, RankingRequest request) {
        User loginUser = userRepository.findAllByIdWithPersonas(List.of(loginUserId)).stream()
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return switch (request.getScope()) {
            case PERSONA -> {
                String p = loginUser.getPersonas().isEmpty() ? "NONE" : loginUser.getPersonas().get(0).name();
                yield getPersonaLeaderboard(loginUser, "ranking:weekly:" + p);
            }
            case FRIENDS -> getFriendLeaderboard(loginUser);
        };
    }

    private RankingListResult getPersonaLeaderboard(User loginUser, String redisKey) {
        Long myUserId = loginUser.getId();
        List<CachedUserInfo> targetUsers = new ArrayList<>();
        boolean isRedisDataExist = false;

        Set<ZSetOperations.TypedTuple<String>> rankedSet = redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, -1);

        if (rankedSet != null && !rankedSet.isEmpty()) {
            isRedisDataExist = true;
            List<String> userIdStrs = rankedSet.stream().map(t -> Objects.requireNonNull(t.getValue())).toList();

            List<Object> cachedJsons = redisTemplate.opsForHash().multiGet(USER_INFO_HASH_KEY, new ArrayList<>(userIdStrs));

            List<Long> missingUserIds = new ArrayList<>();
            Map<Long, CachedUserInfo> cachedUserMap = new HashMap<>();

            for (int i = 0; i < userIdStrs.size(); i++) {
                Long uid = Long.parseLong(userIdStrs.get(i));
                if (cachedJsons.get(i) != null) {
                    try { cachedUserMap.put(uid, objectMapper.readValue(cachedJsons.get(i).toString(), CachedUserInfo.class)); }
                    catch (JsonProcessingException e) { missingUserIds.add(uid); }
                } else { missingUserIds.add(uid); }
            }

            if (!missingUserIds.isEmpty()) {
                List<User> dbUsers = userRepository.findAllByIdWithPersonas(missingUserIds);
                Map<String, String> dataToCache = new HashMap<>();
                for (User u : dbUsers) {
                    CachedUserInfo info = CachedUserInfo.from(u);
                    cachedUserMap.put(u.getId(), info);
                    try { dataToCache.put(u.getId().toString(), objectMapper.writeValueAsString(info)); } catch (Exception ignored) {}
                }
                if (!dataToCache.isEmpty()) redisTemplate.opsForHash().putAll(USER_INFO_HASH_KEY, dataToCache);
            }
            for (String uid : userIdStrs) targetUsers.add(cachedUserMap.get(Long.parseLong(uid)));
        } else {
            targetUsers = userRepository.fetchLeaderboardByPersona(loginUser.getPersonas().get(0), 100, 0)
                    .stream().map(CachedUserInfo::from).toList();
        }

        List<RankingEntryResult> allEntries = generateRankingEntries(targetUsers, myUserId);

        final boolean finalIsRedisDataExist = isRedisDataExist;

        return new RankingListResult("persona", "weekly", allEntries.stream().limit(10).toList(),
                allEntries.stream().filter(RankingEntryResult::isMe).findFirst()
                        .orElseGet(() -> finalIsRedisDataExist ? fetchMyStickyEntryFromRedis(loginUser, redisKey) : fetchMyStickyEntryFromDbFallback(loginUser)));
    }

    private RankingListResult getFriendLeaderboard(User loginUser) {
        List<Long> friendIds = new ArrayList<>(friendshipRepository.fetchFriendUserIds(loginUser.getId()));
        friendIds.add(loginUser.getId());
        List<CachedUserInfo> targetUsers = userRepository.findAllByIdWithPersonas(friendIds).stream()
                .map(CachedUserInfo::from)
                .sorted((u1, u2) -> Integer.compare(u2.getWeeklyXp(), u1.getWeeklyXp()))
                .toList();
        List<RankingEntryResult> allEntries = generateRankingEntries(targetUsers, loginUser.getId());
        return new RankingListResult("friends", "weekly", allEntries.stream().limit(10).toList(),
                allEntries.stream().filter(RankingEntryResult::isMe).findFirst().orElseThrow());
    }

    private List<RankingEntryResult> generateRankingEntries(List<CachedUserInfo> users, Long myUserId) {
        List<RankingEntryResult> entries = new ArrayList<>();
        int cur = 1;
        for (int i = 0; i < users.size(); i++) {
            if (i > 0 && users.get(i).getWeeklyXp() < users.get(i - 1).getWeeklyXp()) cur = i + 1;
            entries.add(RankingEntryResult.from(users.get(i), cur, myUserId));
        }
        return entries;
    }

    private RankingEntryResult fetchMyStickyEntryFromRedis(User loginUser, String redisKey) {
        Long rank = redisTemplate.opsForZSet().reverseRank(redisKey, loginUser.getId().toString());
        return RankingEntryResult.from(CachedUserInfo.from(loginUser), (rank != null ? (int)(rank + 1) : 999), loginUser.getId());
    }

    private RankingEntryResult fetchMyStickyEntryFromDbFallback(User loginUser) {
        UserPersona persona = loginUser.getPersonas().isEmpty() ? null : loginUser.getPersonas().get(0);
        if (persona == null) {
            return RankingEntryResult.ofFallback(loginUser);
        }
        long higherRankedCount = userRepository.countHigherRankedByPersona(
                persona, loginUser.getWeeklyXp(), loginUser.getId());
        int actualRank = (int) (higherRankedCount + 1);
        return RankingEntryResult.from(CachedUserInfo.from(loginUser), actualRank, loginUser.getId());
    }
}