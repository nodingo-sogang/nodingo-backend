package nodingo.core.quiz.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import nodingo.core.user.domain.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRewardResult {
    private Boolean correct;
    private Integer correctAnswerIndex;
    private Integer earnedXp;
    private Integer totalXp;
    private Integer level;
    private Boolean levelUp;
    private List<String> newBadges;
    private List<Long> unlockedNodes;

    public static QuizRewardResult of(boolean isCorrect, Integer answerIndex, int earnedXp, User user, List<String> badges) {
        return QuizRewardResult.builder()
                .correct(isCorrect)
                .correctAnswerIndex(answerIndex)
                .earnedXp(earnedXp)
                .totalXp(user.getXp())
                .level(user.getLevel())
                .levelUp(user.getXp() >= user.getXpNeededForNextLevel())
                .newBadges(badges)
                .unlockedNodes(new ArrayList<>())
                .build();
    }
}