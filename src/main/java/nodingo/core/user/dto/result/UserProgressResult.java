package nodingo.core.user.dto.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProgressResult {
    private final Integer exploredCount;
    private final Integer totalCount;
    private final Double progressRate;

    public static UserProgressResult from(int exploredCount, long totalNodes) {
        double rate = (totalNodes == 0) ? 0.0 : ((double) exploredCount / totalNodes) * 100;
        return UserProgressResult.builder()
                .exploredCount(exploredCount)
                .totalCount((int) totalNodes)
                .progressRate(rate)
                .build();
    }
}