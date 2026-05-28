package nodingo.core.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.dto.result.UserProgressResult;

@Getter
@Builder
public class UserProgressResponse {
    private final Integer exploredCount;
    private final Integer totalCount;
    private final Double progressRate;

    public static UserProgressResponse from(UserProgressResult result) {
        return UserProgressResponse.builder()
                .exploredCount(result.getExploredCount())
                .totalCount(result.getTotalCount())
                .progressRate(result.getProgressRate())
                .build();
    }
}