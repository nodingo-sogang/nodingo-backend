package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class UserSearchResult {
    private Long userId;
    private String nickname;
    private Integer level;
    private String persona;

    public static UserSearchResult from(User user) {
        String personaStr = (user.getPersonas() != null && !user.getPersonas().isEmpty())
                ? user.getPersonas().get(0).name()
                : "NONE";

        return UserSearchResult.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .persona(personaStr)
                .build();
    }
}