package nodingo.core.notification.service.command;

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
        log.info(">>>> [Notification] updateNotifyHour. userId={}, hour={}", command.getUserId(), command.getNotifyHour());
        User user = getOrElseThrow(command.getUserId());
        NotificationSetting setting = getSetting(command.getUserId(), user);
        setting.updateHour(command.getNotifyHour());
        log.info(">>>> [Notification] NotifyHour updated. userId={}, hour={}", command.getUserId(), command.getNotifyHour());
    }

    public void updateFcmToken(NotificationCommand command) {
        log.info(">>>> [Notification] updateFcmToken. userId={}", command.getUserId());
        User user = getOrElseThrow(command.getUserId());
        NotificationSetting setting = getSetting(command.getUserId(), user);
        setting.updateToken(command.getFcmToken());
        log.info(">>>> [Notification] FCM token updated. userId={}", command.getUserId());
    }

    private User getOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private NotificationSetting getSetting(Long userId, User user) {
        return notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info(">>>> [Notification] NotificationSetting not found, creating new one. userId={}", userId);
                    NotificationSetting newSetting = NotificationSetting.create(user);
                    return notificationSettingRepository.save(newSetting);
                });
    }
}