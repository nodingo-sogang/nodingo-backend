package nodingo.core.global.config.batch;

import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.batch.dto.article.NewsApiItem;
import nodingo.core.batch.listener.MyJobListener;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.news.domain.News;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.user.domain.User;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private static final int NEWS_CHUNK_SIZE = 10;
    private static final int USER_CHUNK_SIZE = 10;
    private static final int SUMMARY_CHUNK_SIZE=10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MyJobListener myJobListener;

    @Bean
    public Job dailyNewsJob(Step newsStep, Step relationStep, Step recommendStep, Step recommendSummaryStep) {
        return new JobBuilder("dailyNewsJob", jobRepository)
                .listener(myJobListener)
                .start(newsStep)
                .next(relationStep)
                .next(recommendStep)
                .next(recommendSummaryStep)
                .build();
    }

    @Bean
    public Job hourlyNotificationJob(Step notificationStep) {
        return new JobBuilder("hourlyNotificationJob", jobRepository)
                .start(notificationStep)
                .build();
    }

    @Bean
    public Step newsStep(ItemReader<NewsApiItem> newsApiReader,
                         ItemProcessor<NewsApiItem, News> newsProcessor,
                         ItemWriter<News> newsAiWriter) {

        return new StepBuilder("newsStep", jobRepository)
                .<NewsApiItem, News>chunk(NEWS_CHUNK_SIZE, transactionManager)
                .reader(newsApiReader)
                .processor(newsProcessor)
                .writer(newsAiWriter)
                .build();
    }

    @Bean
    public Step relationStep(Tasklet newsRelationTasklet) {
        return new StepBuilder("relationStep", jobRepository)
                .tasklet(newsRelationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step recommendStep(ItemReader<User> userReader,
                              ItemProcessor<User, List<RecommendKeyword>> recommendProcessor,
                              ItemWriter<List<RecommendKeyword>> recommendWriter) {
        return new StepBuilder("recommendStep", jobRepository)
                .<User, List<RecommendKeyword>>chunk(USER_CHUNK_SIZE, transactionManager)
                .reader(userReader)
                .processor(recommendProcessor)
                .writer(recommendWriter)
                .build();
    }

    @Bean
    public Step recommendSummaryStep(ItemReader<RecommendKeyword> recommendSummaryItemReader,
                                     ItemProcessor<RecommendKeyword, RecommendKeyword> recommendSummaryItemProcessor,
                                     ItemWriter<RecommendKeyword> recommendSummaryItemWriter) {
        return new StepBuilder("recommendSummaryStep", jobRepository)
                .<RecommendKeyword, RecommendKeyword>chunk(SUMMARY_CHUNK_SIZE, transactionManager)
                .reader(recommendSummaryItemReader)
                .processor(recommendSummaryItemProcessor)
                .writer(recommendSummaryItemWriter)
                .build();
    }

    @Bean
    public Step notificationStep(ItemReader<NotificationSetting> notificationReader,
                                 ItemProcessor<NotificationSetting, Message> notificationProcessor,
                                 ItemWriter<Message> fcmBatchWriter) {
        return new StepBuilder("notificationStep", jobRepository)
                .<NotificationSetting, Message>chunk(USER_CHUNK_SIZE, transactionManager)
                .reader(notificationReader)
                .processor(notificationProcessor)
                .writer(fcmBatchWriter)
                .build();
    }
}