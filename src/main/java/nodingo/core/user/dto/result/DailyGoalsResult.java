package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class DailyGoalsResult {
    private final Integer quizzesCompleted;
    private final Integer quizzesRequired;
    private final Boolean completed;

    public static DailyGoalsResult from(User user) {
        int required = 2;
        int completedCount = user.getDailyQuizzesCompleted();

        return DailyGoalsResult.builder()
                .quizzesCompleted(completedCount)
                .quizzesRequired(required)
                .completed(completedCount >= required)
                .build();
    }
}