package nodingo.core.friendship.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class FriendListResult {
    private List<FriendProfileResult> friendResults;
}