package nodingo.core.quiz.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {

    @NotNull(message = "선택한 보기 인덱스는 필수입니다.")
    @Min(value = 1, message = "보기 인덱스는 1 이상이어야 합니다.")
    @Max(value = 4, message = "보기 인덱스는 4 이하여야 합니다.")
    private Integer selectedOptionIndex;
}
