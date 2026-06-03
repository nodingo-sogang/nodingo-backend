package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.dto.result.UserGameResult;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGameResponse {
    private Integer level;
    private Integer xp;
    private Integer xpNeeded;
    private String tier;
    private Integer streak;
    private String nickname;
    private Integer totalQuizzesSolved;

    public static UserGameResponse from(UserGameResult result) {
        return UserGameResponse.builder()
                .level(result.getLevel())
                .xp(result.getXp())
                .xpNeeded(result.getXpNeeded())
                .tier(result.getTier())
                .streak(result.getStreak())
                .nickname(result.getNickname())
                .totalQuizzesSolved(result.getTotalQuizzesSolved())
                .build();
    }
}