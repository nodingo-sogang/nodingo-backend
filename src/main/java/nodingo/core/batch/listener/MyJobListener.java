package nodingo.core.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyJobListener implements JobExecutionListener {

    private final CacheManager cacheManager;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("""
            ============================================================
            >>>> [Batch Job Start] : {}
            >>>> Parameters : {}
            >>>> Start Time : {}
            ============================================================""",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters(),
                jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("============================================================");

        switch (jobExecution.getStatus()) {
            case COMPLETED -> log.info(">>>> [Batch Job Completed]");
            case FAILED -> log.error(">>>> [Batch Job Failed]");
            default -> log.warn(">>>> [Batch Job Finished] Status: {}", jobExecution.getStatus());
        }

        Optional.ofNullable(jobExecution.getStartTime())
                .ifPresent(start -> Optional.ofNullable(jobExecution.getEndTime())
                        .ifPresent(end -> {
                            Duration duration = Duration.between(start, end);
                            log.info(">>>> Duration : {} min {} sec ({} ms)",
                                    duration.toMinutes(),
                                    duration.toSecondsPart(),
                                    duration.toMillis());
                        }));

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            jobExecution.getAllFailureExceptions().forEach(e ->
                    log.error(">>>> [ERROR] {} : {}", e.getClass().getSimpleName(), e.getMessage(), e)
            );
        }

        if (jobExecution.getStatus() == BatchStatus.COMPLETED
                && "dailyNewsJob".equals(jobExecution.getJobInstance().getJobName())) {
            Cache graphCache = cacheManager.getCache("batch:graph");
            if (graphCache != null) {
                graphCache.clear();
                log.info(">>>> [Cache] batch:graph all cache are deleted");
            }
        }

        log.info(">>>> End Time : {}", jobExecution.getEndTime());
        log.info("============================================================");
    }
}