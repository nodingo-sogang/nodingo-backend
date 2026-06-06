package nodingo.core.batch.notification.writer;

import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.notification.service.command.FcmService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmBatchWriter implements ItemWriter<Message> {

    private final FcmService fcmService;

    @Override
    public void write(Chunk<? extends Message> chunk) {
        List<Message> messages = new ArrayList<>(chunk.getItems());
        if (messages.isEmpty()) return;
        log.info(">>>> [FCM Writer] Sending {} FCM messages.", messages.size());
        try {
            fcmService.sendMessages(messages);
            log.info(">>>> [FCM Writer] Sent successfully. count={}", messages.size());
        } catch (Exception e) {
            log.error(">>>> [FCM Writer] Failed to send FCM messages. count={}, error: {}", messages.size(), e.getMessage(), e);
            throw e;
        }
    }
}
