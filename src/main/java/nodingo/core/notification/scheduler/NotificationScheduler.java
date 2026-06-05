package nodingo.core.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("hourlyNotificationJob")
    private final Job hourlyNotificationJob;

    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyNotificationBatch() {
        log.info(">>>> [Scheduler] Starting hourly notification batch.");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters();
            jobLauncher.run(hourlyNotificationJob, jobParameters);
            log.info(">>>> [Scheduler] Hourly notification batch submitted.");
        } catch (Exception e) {
            log.error(">>>> [Scheduler] Failed to launch hourlyNotificationJob.", e);
        }
    }
}