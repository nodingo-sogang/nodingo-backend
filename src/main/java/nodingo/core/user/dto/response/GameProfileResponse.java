package nodingo.core.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.dto.result.GameProfileResult;

@Getter
@Builder
public class GameProfileResponse {
    private final UserGameResponse userGame;
    private final DailyGoalsResponse dailyGoals;

    public static GameProfileResponse from(GameProfileResult result) {
        return GameProfileResponse.builder()
                .userGame(UserGameResponse.from(result.getUserGame()))
                .dailyGoals(DailyGoalsResponse.from(result.getDailyGoals()))
                .build();
    }
}