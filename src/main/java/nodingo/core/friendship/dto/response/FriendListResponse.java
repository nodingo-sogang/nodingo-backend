package nodingo.core.friendship.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.friendship.dto.result.FriendListResult;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendListResponse {
    private List<FriendProfileResponse> friends;

    public static FriendListResponse from(FriendListResult result) {
        List<FriendProfileResponse> responses = result.getFriendResults().stream()
                .map(FriendProfileResponse::from)
                .toList();

        return new FriendListResponse(responses);
    }
}