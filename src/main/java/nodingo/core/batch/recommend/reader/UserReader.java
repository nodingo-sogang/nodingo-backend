package nodingo.core.batch.recommend.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.user.domain.User;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserReader {
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public JpaPagingItemReader<User> userItemReader() {
        log.info(">>>> [User Reader] Initialized. pageSize=100");
        return new JpaPagingItemReaderBuilder<User>()
                .name("userReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u WHERE u.embedding IS NOT NULL")
                .pageSize(100)
                .build();
    }
}
