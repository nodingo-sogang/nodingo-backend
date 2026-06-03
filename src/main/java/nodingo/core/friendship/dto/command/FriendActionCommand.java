package nodingo.core.friendship.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendActionCommand {
    private Long myUserId;
    private Long targetUserId;

    public static FriendActionCommand of(Long myUserId, Long targetUserId) {
        return new FriendActionCommand(myUserId, targetUserId);
    }
}