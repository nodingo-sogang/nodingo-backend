package nodingo.core.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.quiz.dto.result.QuizRewardResult;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRewardResponse {
    private Boolean correct;
    private Integer correctAnswerIndex;
    private Integer earnedXp;
    private Integer totalXp;
    private Integer level;
    private Boolean levelUp;
    private List<String> newBadges;
    private List<Long> unlockedNodes;

    public static QuizRewardResponse from(QuizRewardResult result) {
        return QuizRewardResponse.builder()
                .correct(result.getCorrect())
                .correctAnswerIndex(result.getCorrectAnswerIndex())
                .earnedXp(result.getEarnedXp())
                .totalXp(result.getTotalXp())
                .level(result.getLevel())
                .levelUp(result.getLevelUp())
                .newBadges(result.getNewBadges())
                .unlockedNodes(result.getUnlockedNodes())
                .build();
    }
}