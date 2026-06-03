package nodingo.core.friendship.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendActionRequest {

    @NotNull(message = "친구 추가 또는 수락할 상대방의 유저 ID는 필수값입니다.")
    private Long targetUserId;
}