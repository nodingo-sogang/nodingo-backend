package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.dto.response.UserSearchResponse;
import nodingo.core.user.dto.result.UserSearchResult;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchService {

    private final UserRepository userRepository;

    public UserSearchResponse searchByNickname(String nickname, Long myUserId) {
        User user = getUserOrElseThrow(nickname, myUserId);
        return UserSearchResponse.from(UserSearchResult.from(user));
    }

    private User getUserOrElseThrow(String nickname, Long myUserId) {
        return userRepository.findByNicknameAndIdNot(nickname, myUserId)
                .orElseThrow(() -> new UserNotFoundException("해당 닉네임의 유저를 찾을 수 없습니다."));
    }
}
