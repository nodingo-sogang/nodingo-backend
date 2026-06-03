package nodingo.core.friendship.dto.response;

import nodingo.core.friendship.dto.result.FriendProfileResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendProfileResponse {
    private Long userId;
    private String name;
    private Integer level;
    private String persona;

    public static FriendProfileResponse from(FriendProfileResult result) {
        return FriendProfileResponse.builder()
                .userId(result.getUserId())
                .name(result.getName())
                .level(result.getLevel())
                .persona(result.getPersona())
                .build();
    }
}