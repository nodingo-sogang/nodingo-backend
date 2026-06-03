package nodingo.core.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.RankingScope;
import nodingo.core.user.domain.UserPersona;

@Getter
@Builder
@AllArgsConstructor
public class RankingQuery {
    private Long userId;
    private RankingScope scope;
    private UserPersona userOwnPersona;
    private int page;

    public static RankingQuery of(Long userId, UserPersona myPersona, RankingScope scope, int page) {
        return RankingQuery.builder()
                .userId(userId)
                .userOwnPersona(myPersona)
                .scope(scope)
                .page(page)
                .build();
    }
}
