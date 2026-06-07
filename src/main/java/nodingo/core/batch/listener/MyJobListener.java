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

    private static final String DAILY_NEWS_JOB = "dailyNewsJob";
    private static final String GRAPH_CACHE_NAME = "batch:graph";

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
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();

        log.info("============================================================");

        switch (status) {
            case COMPLETED -> log.info(">>>> [Batch Job Completed] - Job Name: {}", jobName);
            case FAILED -> log.error(">>>> [Batch Job Failed] - Job Name: {}", jobName);
            default -> log.warn(">>>> [Batch Job Finished] Status: {}, Job Name: {}", status, jobName);
        }

        Optional.ofNullable(jobExecution.getStartTime())
                .ifPresent(start -> Optional.ofNullable(jobExecution.getEndTime())
                        .ifPresent(end -> {
                            Duration duration = Duration.between(start, end);
                            log.info(">>>> End Time : {}", end);
                            log.info(">>>> Duration : {} min {} sec ({} ms)",
                                    duration.toMinutes(),
                                    duration.toSecondsPart(),
                                    duration.toMillis());
                        }));

        if (status == BatchStatus.FAILED) {
            jobExecution.getAllFailureExceptions().forEach(e ->
                    log.error(">>>> [Batch Job ERROR StackTrace]", e)
            );
        }

        if (status == BatchStatus.COMPLETED && DAILY_NEWS_JOB.equals(jobName)) {
            Cache graphCache = cacheManager.getCache(GRAPH_CACHE_NAME);
            if (graphCache != null) {
                graphCache.clear();
                log.info(">>>> [Cache] '{}' all cache are deleted successfully.", GRAPH_CACHE_NAME);
            }
        }

        log.info("============================================================");
    }
}