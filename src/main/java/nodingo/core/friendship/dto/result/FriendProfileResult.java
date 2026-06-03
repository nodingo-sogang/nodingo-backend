package nodingo.core.friendship.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class FriendProfileResult {
    private Long userId;
    private String name;
    private Integer level;
    private String persona;

    public static FriendProfileResult from(User user) {
        String personaStr = (user.getPersonas() != null && !user.getPersonas().isEmpty())
                ? user.getPersonas().get(0).name()
                : "NONE";

        return FriendProfileResult.builder()
                .userId(user.getId())
                .name(user.getName())
                .level(user.getLevel())
                .persona(personaStr)
                .build();
    }
}