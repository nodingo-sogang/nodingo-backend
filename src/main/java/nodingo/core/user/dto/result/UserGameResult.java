package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.User;

@Getter
@Builder
@AllArgsConstructor
public class UserGameResult {
    private final Integer level;
    private final Integer xp;
    private final Integer xpNeeded;
    private final String tier;
    private final Integer streak;
    private final String name;
    private final Integer totalQuizzesSolved;

    public static UserGameResult from(User user) {
        return UserGameResult.builder()
                .level(user.getLevel())
                .xp(user.getXp())
                .xpNeeded(user.getXpNeededForNextLevel())
                .tier(user.getTier())
                .streak(user.getConsecutiveAttendanceDays())
                .name(user.getName())
                .totalQuizzesSolved(user.getTotalQuizzesCompleted())
                .build();
    }
}