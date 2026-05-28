package nodingo.core.game.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class GameProfileResult {
    private final UserGameResult userGame;
    private final DailyGoalsResult dailyGoals;

    public static GameProfileResult from(User user) {
        return GameProfileResult.builder()
                .userGame(UserGameResult.from(user))
                .dailyGoals(DailyGoalsResult.from(user))
                .build();
    }
}
