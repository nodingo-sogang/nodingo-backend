package nodingo.core.user.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.user.domain.User;
import nodingo.core.user.event.UserXpChangedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String USER_INFO_HASH_KEY = "user:ranking:info";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleXpChanged(UserXpChangedEvent event) {
        User user = event.getUser();

        try {
            String persona = user.getPersonas().isEmpty() ? "NONE" : user.getPersonas().get(0).name();
            String redisKey = "ranking:weekly:" + persona;
            redisTemplate.opsForZSet().add(redisKey, user.getId().toString(), user.getWeeklyXp());

            redisTemplate.opsForHash().delete(USER_INFO_HASH_KEY, user.getId().toString());

            log.info(">>>> [Redis Sync] 랭킹 동기화 완료. userId={}, weeklyXp={}", user.getId(), user.getWeeklyXp());
        } catch (Exception e) {
            log.error(">>>> [Redis Sync Error] Redis 랭킹 데이터 동기화 실패. userId={}", user.getId(), e);
        }
    }
}