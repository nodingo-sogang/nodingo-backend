package nodingo.core.global.exception.friendship;

public class SelfFriendRequestException extends RuntimeException {
    public SelfFriendRequestException(String message) {
        super(message);
    }
}
