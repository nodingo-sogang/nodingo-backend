package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RankingListResult {
    private String scope;
    private String period;
    private List<RankingEntryResult> entries;
    private RankingEntryResult myEntry;
}