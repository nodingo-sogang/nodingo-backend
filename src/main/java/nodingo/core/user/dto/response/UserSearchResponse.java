package nodingo.core.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nodingo.core.user.dto.result.UserSearchResult;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private UserSearchResult user;

    public static UserSearchResponse from(UserSearchResult result) {
        return new UserSearchResponse(result);
    }
}