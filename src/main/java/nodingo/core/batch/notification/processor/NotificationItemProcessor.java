package nodingo.core.batch.notification.processor;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.user.domain.User;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationItemProcessor implements ItemProcessor<NotificationSetting, Message> {

    @Override
    public Message process(NotificationSetting setting) {
        User user = setting.getUser();

        if (user == null || setting.getFcmToken() == null) {
            log.warn(">>>> [Notification Processor] Skip: user or fcmToken is null. settingId={}", setting.getId());
            return null;
        }

        log.debug(">>>> [Notification Processor] Building FCM message. userId={}", user.getId());

        return Message.builder()
                .setToken(setting.getFcmToken())
                .setNotification(Notification.builder()
                        .setTitle("🚀 이슈 맵 업데이트 완료!")
                        .setBody(user.getName() + "님, 지금 바로 최신 뉴스 탭을 확인해보세요.")
                        .build())
                .putData("click_action", "OPEN_TABS_PAGE")
                .putData("path", "/graph/tabs")
                .build();
    }
}