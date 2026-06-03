package nodingo.core.quiz.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.quiz.domain.Quiz;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResult {
    private Long quizId;
    private String question;
    private List<String> options;
    private String sourceOutlet;
    private LocalDate sourceDate;
    private String sourceUrl;
    private boolean solved;

    public static QuizResult from(Quiz quiz, boolean isSolved) {
        String outlet = "뉴스 원문";

        if (quiz.getNews() != null && quiz.getNews().getUrl() != null) {
            outlet = extractOutlet(quiz.getNews().getUrl());
        }

        return QuizResult.builder()
                .quizId(quiz.getId())
                .question(quiz.getQuestion())
                .options(List.of(quiz.getOption1(), quiz.getOption2(), quiz.getOption3(), quiz.getOption4()))
                .sourceOutlet(outlet)
                .sourceDate(quiz.getNews() != null && quiz.getNews().getDateTimePub() != null
                        ? quiz.getNews().getDateTimePub().toLocalDate() : null)
                .sourceUrl(quiz.getNews() != null ? quiz.getNews().getUrl() : null)
                .solved(isSolved)
                .build();
    }

    private static String extractOutlet(String url) {
        try {
            return url.split("/")[2].replace("www.", "");
        } catch (Exception e) {
            return "뉴스 원문";
        }
    }
}
