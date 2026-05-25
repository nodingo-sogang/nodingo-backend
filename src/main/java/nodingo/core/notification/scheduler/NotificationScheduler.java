package nodingo.core.notification.scheduler;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.notification.service.query.NotificationQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        log.info("⏰Every hour on the hour! Starting the notification batch process.");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("requestTime", LocalDateTime.now())
                    .addString("runId", UUID.randomUUID().toString())
                    .addString("targetDate", LocalDateTime.now().toLocalDate().toString())
                    .toJobParameters();

            jobLauncher.run(hourlyNotificationJob, jobParameters);

        } catch (Exception e) {
            log.error("❌ An error occurred while executing the HourlyNotificationJob batch : {}", e.getMessage(), e);
        }
    }
}
