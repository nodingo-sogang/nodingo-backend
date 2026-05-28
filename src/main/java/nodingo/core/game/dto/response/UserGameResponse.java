package nodingo.core.game.dto.response;

import lombok.Builder;
import lombok.Getter;
import nodingo.core.game.dto.result.UserGameResult;


@Getter
@Builder
public class UserGameResponse {
    private final Integer level;
    private final Integer xp;
    private final Integer xpNeeded;
    private final String tier;
    private final Integer streak;

    public static UserGameResponse from(UserGameResult result) {
        return UserGameResponse.builder()
                .level(result.getLevel())
                .xp(result.getXp())
                .xpNeeded(result.getXpNeeded())
                .tier(result.getTier())
                .streak(result.getStreak())
                .build();
    }
}