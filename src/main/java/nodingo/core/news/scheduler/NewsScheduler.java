package nodingo.core.news.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class NewsScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyNewsJob;

    public NewsScheduler(JobLauncher jobLauncher,
                         @Qualifier("dailyNewsJob") Job dailyNewsJob) {
        this.jobLauncher = jobLauncher;
        this.dailyNewsJob = dailyNewsJob;
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void runDailyNewsJob() {
        log.info(">>>> [Scheduler] Starting news collection batch at 5 AM.");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("runId", UUID.randomUUID().toString())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(dailyNewsJob, jobParameters);
            log.info(">>>> [Scheduler] Batch finished. status={}", jobExecution.getStatus());
        } catch (Exception e) {
            log.error(">>>> [Scheduler] Exception occurred during batch execution", e);
        }
    }
}