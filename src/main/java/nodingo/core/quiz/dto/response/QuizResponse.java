package nodingo.core.quiz.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.quiz.dto.result.QuizResult;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Long quizId;
    private String question;
    private List<String> options;
    private String sourceOutlet;

    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDate sourceDate;
    private String sourceUrl;
    private boolean solved;

    public static QuizResponse from(QuizResult result) {
        return QuizResponse.builder()
                .quizId(result.getQuizId())
                .question(result.getQuestion())
                .options(result.getOptions())
                .sourceOutlet(result.getSourceOutlet())
                .sourceDate(result.getSourceDate())
                .sourceUrl(result.getSourceUrl())
                .solved(result.isSolved())
                .build();
    }
}