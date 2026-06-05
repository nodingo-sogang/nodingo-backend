package nodingo.core.global.exception.ai;

public class AiRateLimitException extends RuntimeException {
    public AiRateLimitException(String message) {
        super(message);
    }
}