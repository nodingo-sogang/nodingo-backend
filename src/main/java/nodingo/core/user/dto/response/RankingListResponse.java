package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.dto.result.RankingListResult;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RankingListResponse {
    private String scope;
    private String period;
    private List<RankingEntryResponse> entries;
    private RankingEntryResponse myEntry;

    public static RankingListResponse from(RankingListResult result) {
        List<RankingEntryResponse> responseEntries = result.getEntries().stream()
                .map(RankingEntryResponse::from)
                .toList();

        RankingEntryResponse myResponseEntry = result.getMyEntry() != null
                ? RankingEntryResponse.from(result.getMyEntry())
                : null;

        return new RankingListResponse(result.getScope(), result.getPeriod(), responseEntries, myResponseEntry);
    }
}