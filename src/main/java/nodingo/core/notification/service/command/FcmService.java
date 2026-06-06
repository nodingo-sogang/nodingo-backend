package nodingo.core.notification.service.command;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class FcmService {

    public void sendTestMessage(String targetToken) {
        try {
            Notification notification = Notification.builder()
                    .setTitle("노딩고 테스트 알림 🚀")
                    .setBody("성민님, 서버에서 보낸 알림이 완벽하게 도착했습니다!")
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info(">>>> [FCM] Test message sent. response={}", response);

        } catch (Exception e) {
            log.error(">>>> [FCM] Test message failed. error: {}", e.getMessage(), e);
        }
    }

    public void sendMessages(List<? extends Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            List<Message> messageList = (List<Message>) messages;
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messageList);

            if (response.getFailureCount() > 0) {
                log.warn(">>>> [FCM] Batch partially failed. success={}, failure={}", response.getSuccessCount(), response.getFailureCount());

                response.getResponses().forEach(res -> {
                    if (!res.isSuccessful()) {
                        log.error(">>>> [FCM] Individual push failed. errorCode={}, error: {}", res.getException().getMessagingErrorCode(), res.getException().getMessage());
                    }
                });
            } else {
                log.info(">>>> [FCM] Batch sent successfully. count={}", response.getSuccessCount());
            }
        } catch (Exception e) {
            log.error(">>>> [FCM] Critical error during batch send. error: {}", e.getMessage(), e);
        }
    }
}
