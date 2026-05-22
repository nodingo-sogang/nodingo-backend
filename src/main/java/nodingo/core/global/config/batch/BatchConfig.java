package nodingo.core.global.config.batch;

import com.google.firebase.messaging.Message;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private static final int NEWS_CHUNK_SIZE = 10;
    private static final int USER_CHUNK_SIZE = 30;
    private static final int SUMMARY_CHUNK_SIZE = 30;

    private final JobRepository jobRepository;
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
                         ItemWriter<News> newsAiWriter,
                         @Qualifier("noDbTransactionManager") ResourcelessTransactionManager noDbTransactionManager) {
        return new StepBuilder("newsStep", jobRepository)
                .<NewsApiItem, News>chunk(NEWS_CHUNK_SIZE, noDbTransactionManager)  // ✅ 직접 호출 말고 파라미터로
                .reader(newsApiReader)
                .processor(newsProcessor)
                .writer(newsAiWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step relationStep(Tasklet newsRelationTasklet,
                             PlatformTransactionManager transactionManager) {  // ✅ 생성자 말고 파라미터로
        return new StepBuilder("relationStep", jobRepository)
                .tasklet(newsRelationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step recommendStep(ItemReader<User> userReader,
                              ItemProcessor<User, List<RecommendKeyword>> recommendProcessor,
                              ItemWriter<List<RecommendKeyword>> recommendWriter,
                              PlatformTransactionManager transactionManager) {  // ✅ 파라미터로
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
                                     ItemWriter<RecommendKeyword> recommendSummaryItemWriter,
                                     PlatformTransactionManager transactionManager) {  // ✅ 파라미터로
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
                                 ItemWriter<Message> fcmBatchWriter,
                                 PlatformTransactionManager transactionManager) {  // ✅ 파라미터로
        return new StepBuilder("notificationStep", jobRepository)
                .<NotificationSetting, Message>chunk(USER_CHUNK_SIZE, transactionManager)
                .reader(notificationReader)
                .processor(notificationProcessor)
                .writer(fcmBatchWriter)
                .build();
    }
}