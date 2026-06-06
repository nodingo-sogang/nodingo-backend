package nodingo.core.batch.recommend.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.util.BatchDateUtil;
import nodingo.core.keyword.domain.RecommendKeyword;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RecommendSummaryReader {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaPagingItemReader<RecommendKeyword> recommendSummaryItemReader() {
        LocalDate targetDate = BatchDateUtil.getTargetDate();
        log.info(">>>> [Recommend Summary Reader] Initialized. targetDate={}", targetDate);

        return new JpaPagingItemReaderBuilder<RecommendKeyword>()
                .name("recommendSummaryItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT r FROM RecommendKeyword r WHERE r.targetDate = :targetDate")
                .parameterValues(Map.of("targetDate", targetDate))
                .pageSize(100)
                .saveState(false)
                .build();
    }
}