package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.dto.result.BadgeListResult;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BadgeListResponse {
    private List<BadgeResponse> badges;

    public static BadgeListResponse from(BadgeListResult result) {
        return BadgeListResponse.builder()
                .badges(result.getBadges().stream()
                        .map(b -> BadgeResponse.builder()
                                .id(b.getId())
                                .name(b.getName())
                                .category(b.getCategory())
                                .description(b.getDescription())
                                .condition(b.getCondition())
                                .earned(b.isEarned())
                                .earnedAt(b.getEarnedAt())
                                .build())
                        .toList())
                .build();
    }
}