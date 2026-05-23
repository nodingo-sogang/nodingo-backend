package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nodingo.core.user.domain.OnboardingStatus;
import nodingo.core.user.dto.result.OnboardingStatusResult;

@Getter
@AllArgsConstructor
public class OnboardingStatusResponse {
    private final OnboardingStatus status;

    public static OnboardingStatusResponse from(OnboardingStatusResult result) {
        return new OnboardingStatusResponse(result.getStatus());
    }
}