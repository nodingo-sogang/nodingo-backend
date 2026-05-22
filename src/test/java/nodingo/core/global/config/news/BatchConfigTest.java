package nodingo.core.global.config.news;

import com.google.firebase.messaging.Message;
import jakarta.persistence.EntityManagerFactory;
import nodingo.core.batch.dto.article.NewsApiItem;
import nodingo.core.batch.listener.MyJobListener;

import nodingo.core.global.config.batch.BatchConfig;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.news.domain.News;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
// ... (나머지 import 동일)

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BatchConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ResourcelessTransactionManager noDbTransactionManager; // ✅ 추가
    @Mock private MyJobListener myJobListener;
    @Mock private Tasklet relationTasklet;

    @Mock private ItemReader<NewsApiItem> newsApiReader;
    @Mock private ItemProcessor<NewsApiItem, News> newsProcessor;
    @Mock private ItemWriter<News> newsAiWriter;

    @Mock private ItemReader<User> userReader;
    @Mock private ItemProcessor<User, List<RecommendKeyword>> recommendProcessor;
    @Mock private ItemWriter<List<RecommendKeyword>> recommendWriter;

    @Mock private ItemReader<RecommendKeyword> recommendSummaryReader;
    @Mock private ItemProcessor<RecommendKeyword, RecommendKeyword> recommendSummaryProcessor;
    @Mock private ItemWriter<RecommendKeyword> recommendSummaryWriter;

    @Mock private ItemReader<NotificationSetting> notificationReader;
    @Mock private ItemProcessor<NotificationSetting, Message> notificationProcessor;
    @Mock private ItemWriter<Message> fcmBatchWriter;

    private BatchConfig config;

    @BeforeEach
    void setUp() {
        config = new BatchConfig(jobRepository, myJobListener);
    }

    @Test
    @DisplayName("일배치와 시배치 Job이 각각 정상적으로 분리되어 생성된다")
    void jobAndStepCreationTest() {

        // when
        Step newsStep = config.newsStep(
                newsApiReader,
                newsProcessor,
                newsAiWriter,
                noDbTransactionManager  // ✅ 추가
        );

        Step relationStep = config.relationStep(
                relationTasklet,
                transactionManager
        );

        Step recommendStep = config.recommendStep(
                userReader,
                recommendProcessor,
                recommendWriter,
                transactionManager
        );

        Step summaryStep = config.recommendSummaryStep(
                recommendSummaryReader,
                recommendSummaryProcessor,
                recommendSummaryWriter,
                transactionManager
        );

        Step notificationStep = config.notificationStep(
                notificationReader,
                notificationProcessor,
                fcmBatchWriter,
                transactionManager
        );

        Job dailyJob = config.dailyNewsJob(
                newsStep,
                relationStep,
                recommendStep,
                summaryStep
        );

        Job hourlyJob = config.hourlyNotificationJob(notificationStep);

        // then
        assertThat(dailyJob.getName()).isEqualTo("dailyNewsJob");

        SimpleJob simpleDailyJob = (SimpleJob) dailyJob;

        assertThat(simpleDailyJob.getStepNames())
                .containsExactly(
                        "newsStep",
                        "relationStep",
                        "recommendStep",
                        "recommendSummaryStep"
                );

        assertThat(hourlyJob.getName()).isEqualTo("hourlyNotificationJob");

        SimpleJob simpleHourlyJob = (SimpleJob) hourlyJob;

        assertThat(simpleHourlyJob.getStepNames())
                .containsExactly("notificationStep");
    }
}