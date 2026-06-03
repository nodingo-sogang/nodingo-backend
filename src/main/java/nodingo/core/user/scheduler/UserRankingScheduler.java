package nodingo.core.user.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.user.service.command.UserRankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRankingScheduler {

    private final UserRankingService userRankingService;

    @Scheduled(cron = "0 0 0 * * SUN")
    public void runWeeklyRankingReset() {
        log.info(">>>> [Scheduler Engine] Trigger weekly ranking reset batch job at midnight Sunday.");
        userRankingService.resetWeeklyLeaderboard();
    }
}
