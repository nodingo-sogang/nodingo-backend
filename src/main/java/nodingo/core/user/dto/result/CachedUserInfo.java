package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.domain.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserInfo {
    private Long userId;
    private String nickname;
    private int level;
    private int weeklyXp;
    private String persona;
    private String profileImageUrl;

    public static CachedUserInfo from(User user) {
        String displayPersona = user.getPersonas().isEmpty() ? "NONE" : user.getPersonas().get(0).name();
        String displayNickname = user.getNickname() != null ? user.getNickname() : user.getName();

        return CachedUserInfo.builder()
                .userId(user.getId())
                .nickname(displayNickname)
                .level(user.getLevel() != null ? user.getLevel() : 1)
                .weeklyXp(user.getWeeklyXp()) // User 엔티티의 int weeklyXp와 매칭
                .persona(displayPersona)
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}