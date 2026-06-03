package nodingo.core.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nodingo.core.user.domain.RankingScope;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingRequest {

    @NotNull(message = "랭킹 조회 스코프(FRIENDS 또는 PERSONA)는 필수입니다.")
    private RankingScope scope;

    private int page = 0;
}