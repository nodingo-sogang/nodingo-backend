package nodingo.core.notification.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nodingo.core.notification.dto.request.NotificationRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationCommand {
    private final Long userId;
    private final Integer notifyHour;
    private final String fcmToken;

    // 시간 수정 전용 빌더
    public static NotificationCommand ofTime(Long userId, NotificationRequest.TimeRequest request) {
        return new NotificationCommand(userId, request.getNotifyHour(), null);
    }

    // 토큰 수정 전용 빌더
    public static NotificationCommand ofToken(Long userId, NotificationRequest.TokenRequest request) {
        return new NotificationCommand(userId, null, request.getFcmToken());
    }
}