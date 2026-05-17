package nodingo.core.notification.service.command;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.notification.dto.command.NotificationCommand;
import nodingo.core.notification.repository.NotificationSettingRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public void updateNotifyHour(NotificationCommand command) {
        User user = getOrElseThrow(command.getUserId());
        NotificationSetting setting = getSetting(command.getUserId(), user);

        setting.updateHour(command.getNotifyHour());
    }

    public void updateFcmToken(NotificationCommand command) {
        User user = getOrElseThrow(command.getUserId());
        NotificationSetting setting = getSetting(command.getUserId(), user);

        setting.updateToken(command.getFcmToken());
    }

    private User getOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다.")); // 예외 클래스명은 프로젝트에 맞게 확인해주세요!
    }

    private NotificationSetting getSetting(Long userId, User user) {
        return notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSetting newSetting = NotificationSetting.create(user);
                    return notificationSettingRepository.save(newSetting);
                });
    }
}