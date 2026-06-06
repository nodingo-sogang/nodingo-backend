package nodingo.core.batch.notification.reader;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.notification.domain.NotificationSetting;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class NotificationItemReader extends JpaPagingItemReader<NotificationSetting> {

    private static final int USER_CHUNK_SIZE=100;
    private final EntityManagerFactory entityManagerFactory;


    @PostConstruct
    public void init() {
        int currentHour = LocalDateTime.now().getHour();
        log.info(">>>> [Notification Reader] Initialized. targetHour={}", currentHour);

        this.setEntityManagerFactory(entityManagerFactory);
        this.setQueryString("SELECT ns FROM NotificationSetting ns JOIN FETCH ns.user WHERE ns.notifyHour = :hour");
        this.setParameterValues(Collections.singletonMap("hour", currentHour));
        this.setPageSize(USER_CHUNK_SIZE);
    }
}