package nodingo.core.game.dto.response;

import lombok.Builder;
import lombok.Getter;
import nodingo.core.game.dto.result.DailyGoalsResult;

@Getter
@Builder
public class DailyGoalsResponse {
    private final Integer quizzesCompleted;
    private final Integer quizzesRequired;
    private final Boolean completed;

    public static DailyGoalsResponse from(DailyGoalsResult result) {
        return DailyGoalsResponse.builder()
                .quizzesCompleted(result.getQuizzesCompleted())
                .quizzesRequired(result.getQuizzesRequired())
                .completed(result.getCompleted())
                .build();
    }
}