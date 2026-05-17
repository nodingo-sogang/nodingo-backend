package nodingo.core.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import nodingo.core.notification.dto.result.NotificationResult;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Integer notifyHour;
    private boolean isConfigured;

    public static NotificationResponse from(NotificationResult result) {
        boolean configured = (result != null && result.getNotifyHour() != null);

        return new NotificationResponse(
                result != null ? result.getNotifyHour() : null,
                configured
        );
    }
}
