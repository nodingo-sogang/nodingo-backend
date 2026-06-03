package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.dto.result.RankingEntryResult;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntryResponse {
    private int rank;
    private String nickname;
    private int level;
    private int weekXp;
    private String persona;
    private boolean isMe;

    public static RankingEntryResponse from(RankingEntryResult result) {
        return RankingEntryResponse.builder()
                .rank(result.getRank())
                .nickname(result.getNickname())
                .level(result.getLevel())
                .weekXp(result.getWeekXp())
                .persona(result.getPersona())
                .isMe(result.isMe())
                .build();
    }
}
