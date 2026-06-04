package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileResult {
    private Integer level;
    private Integer xp;
    private Integer xpNeeded;
    private String tier;
    private Integer streak;
    private String nickname;
    private String profileImageUrl;

    public static UserProfileResult from(User user) {
        return UserProfileResult.builder()
                .level(user.getLevel())
                .xp(user.getXp())
                .xpNeeded(user.getXpNeededForNextLevel())
                .tier(user.getTier())
                .streak(user.getConsecutiveAttendanceDays())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
