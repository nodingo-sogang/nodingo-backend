package nodingo.core.user.scheduler;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.user.service.command.UserRankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRankingScheduler {

    private final UserRankingService userRankingService;
    private final MonitoringMetrics metrics;

    @Scheduled(cron = "0 0 0 * * SUN")
    public void runWeeklyRankingReset() {
        log.info(">>>> [Scheduler] Weekly ranking reset start.");
        Timer.Sample sample = metrics.startTimer();
        try {
            userRankingService.resetWeeklyLeaderboard();
            metrics.recordSchedulerSuccess("weeklyRankingReset");
            metrics.stopTimer(sample, "weeklyRankingReset", "success");
        } catch (Exception e) {
            metrics.recordSchedulerFailure("weeklyRankingReset");
            metrics.stopTimer(sample, "weeklyRankingReset", "failure");
            log.error(">>>> [Scheduler] Weekly ranking reset failed", e);
        }
    }
}
