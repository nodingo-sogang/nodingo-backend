package nodingo.core.notification.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.notification.domain.NotificationSetting;
import nodingo.core.notification.dto.result.NotificationResult;
import nodingo.core.notification.repository.NotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {
    private final NotificationSettingRepository notificationSettingRepository;

    public List<NotificationSetting> getTargetSettings(int hour) {
        List<NotificationSetting> settings = notificationSettingRepository.findSettingsByHour(hour);
        log.info(">>>> [Notification Query] getTargetSettings. hour={}, count={}", hour, settings.size());
        return settings;
    }

    public NotificationResult getNotificationSetting(Long userId) {
        log.info(">>>> [Notification Query] getNotificationSetting. userId={}", userId);
        return notificationSettingRepository.findByUserId(userId)
                .map(NotificationResult::from)
                .orElse(null);
    }
}