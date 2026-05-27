package nodingo.core.keyword.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.keyword.dto.result.ScrapKeywordNodeResult;
import nodingo.core.user.domain.UserPersona;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapKeywordNodeResponse {
    private Long id;
    private String word;
    private UserPersona persona;

    public static ScrapKeywordNodeResponse from(ScrapKeywordNodeResult result) {
        return ScrapKeywordNodeResponse.builder()
                .id(result.getId())
                .word(result.getWord())
                .persona(result.getPersona())
                .build();
    }
}