package nodingo.core.keyword.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.domain.UserScrap;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapKeywordNodeResult {
    private Long id;
    private String word;
    private UserPersona persona;

    public static ScrapKeywordNodeResult from(UserScrap scrap) {
        Keyword keyword = scrap.getRecommendKeyword().getKeyword();
        return ScrapKeywordNodeResult.builder()
                .id(keyword.getId())
                .word(keyword.getWord())
                .persona(keyword.getPersona())
                .build();
    }
}