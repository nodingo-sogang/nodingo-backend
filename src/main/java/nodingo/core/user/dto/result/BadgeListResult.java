package nodingo.core.user.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BadgeListResult {
    private List<BadgeInfoResult> badges;
}