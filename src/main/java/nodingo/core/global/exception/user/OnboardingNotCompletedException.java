package nodingo.core.global.exception.user;

public class OnboardingNotCompletedException extends RuntimeException {
    public OnboardingNotCompletedException(String message) {
        super(message);
    }
}