package nodingo.core.batch.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.springframework.batch.core.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class MyJobListenerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache graphCache;

    private MyJobListener listener;

    @BeforeEach
    void setUp() {
        listener = new MyJobListener(cacheManager);
    }

    private JobExecution createJobExecution() {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        return jobExecution;
    }

    private JobExecution createDailyJobExecution() {
        JobInstance jobInstance = new JobInstance(1L, "dailyNewsJob");
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
        return jobExecution;
    }

    @Test
    void beforeJob_shouldNotThrow() {
        JobExecution jobExecution = createJobExecution();
        jobExecution.setStartTime(LocalDateTime.now());

        listener.beforeJob(jobExecution);

        assertThat(jobExecution).isNotNull();
    }

    @Test
    void afterJob_completed() {
        JobExecution jobExecution = createJobExecution();
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now().minusSeconds(5));
        jobExecution.setEndTime(LocalDateTime.now());

        listener.afterJob(jobExecution);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void afterJob_failed() {
        JobExecution jobExecution = createJobExecution();
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.setStartTime(LocalDateTime.now().minusSeconds(5));
        jobExecution.setEndTime(LocalDateTime.now());

        jobExecution.addFailureException(new RuntimeException("테스트 에러"));

        listener.afterJob(jobExecution);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jobExecution.getAllFailureExceptions()).isNotEmpty();
    }

    @Test
    void afterJob_dailyNewsJob_completed_shouldClearCache() {
        when(cacheManager.getCache("batch:graph")).thenReturn(graphCache);

        JobExecution jobExecution = createDailyJobExecution();
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now().minusSeconds(5));
        jobExecution.setEndTime(LocalDateTime.now());

        listener.afterJob(jobExecution);

        verify(graphCache).clear();
    }

    @Test
    void afterJob_dailyNewsJob_failed_shouldNotClearCache() {
        JobExecution jobExecution = createDailyJobExecution();
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.setStartTime(LocalDateTime.now().minusSeconds(5));
        jobExecution.setEndTime(LocalDateTime.now());

        listener.afterJob(jobExecution);

        verify(cacheManager, never()).getCache(any());
    }
}