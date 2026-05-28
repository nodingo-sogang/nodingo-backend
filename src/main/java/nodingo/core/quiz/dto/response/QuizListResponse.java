package nodingo.core.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.quiz.dto.result.QuizListResult;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizListResponse {
    private List<QuizResponse> quizzes;

    public static QuizListResponse from(QuizListResult result) {
        return QuizListResponse.builder()
                .quizzes(result.getQuizzes().stream()
                        .map(QuizResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}