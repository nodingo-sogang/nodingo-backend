package nodingo.core.quiz.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.quiz.dto.request.QuizSubmitRequest;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitCommand {
    private Long userId;
    private Long quizId;
    private Integer selectedOptionIndex;

    public static QuizSubmitCommand of(Long userId, Long quizId, QuizSubmitRequest request) {
        return QuizSubmitCommand.builder()
                .userId(userId)
                .quizId(quizId)
                .selectedOptionIndex(request.getSelectedOptionIndex())
                .build();
    }
}