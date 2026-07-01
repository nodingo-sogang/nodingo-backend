package nodingo.core.user.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserRankingService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public void resetWeeklyLeaderboard() {
        log.info(">>>> [Ranking Engine] Start weekly leaderboard reset batch job.");

        userRepository.resetAllWeeklyXp();

        Set<String> keys = redisTemplate.keys("ranking:weekly:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info(">>>> [Ranking] Redis keys deleted. count={}", keys.size());
        }

        log.info(">>>> [Ranking Engine] Successfully cleared DB weekly XP and Redis Sorted Sets.");
    }
}
