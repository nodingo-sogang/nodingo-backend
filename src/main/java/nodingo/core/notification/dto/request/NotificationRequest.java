package nodingo.core.notification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {

    @Getter
    public static class TimeRequest {
        @NotNull(message = "알림 시간(1~24)은 필수입니다.")
        @Min(1) @Max(24)
        private Integer notifyHour;
    }

    @Getter
    public static class TokenRequest {
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        private String fcmToken;
    }
}