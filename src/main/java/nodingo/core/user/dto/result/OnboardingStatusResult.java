package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nodingo.core.user.domain.OnboardingStatus;

@Getter
@AllArgsConstructor
public class OnboardingStatusResult {
    private OnboardingStatus status;

    public static OnboardingStatusResult of(OnboardingStatus status) {
        return new OnboardingStatusResult(status);
    }
}