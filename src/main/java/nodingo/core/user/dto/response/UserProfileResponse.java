package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.dto.result.UserProfileResult;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Integer level;
    private Integer xp;
    private Integer xpNeeded;
    private String tier;
    private Integer streak;
    private String nickname;
    private String profileImageUrl;

    public static UserProfileResponse from(UserProfileResult result) {
        return UserProfileResponse.builder()
                .level(result.getLevel())
                .xp(result.getXp())
                .xpNeeded(result.getXpNeeded())
                .tier(result.getTier())
                .streak(result.getStreak())
                .nickname(result.getNickname())
                .profileImageUrl(result.getProfileImageUrl())
                .build();
    }
}