package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.UserBadge;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class BadgeInfoResult {
    private String id;
    private String name;
    private String category;
    private String description;
    private String condition;
    private boolean earned;
    private LocalDate earnedAt;

    public static BadgeInfoResult of(BadgeType type, UserBadge history) {
        boolean isEarned = history != null;

        return BadgeInfoResult.builder()
                .id(type.getId())
                .name(type.getName())
                .category(type.getCategory())
                .description(type.getDescription())
                .condition(type.getCondition())
                .earned(isEarned)
                .earnedAt(isEarned ? history.getCreatedAt().toLocalDate() : null)
                .build();
    }
}