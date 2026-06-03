package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.user.dto.result.GameProfileResult;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameQueryService {
    private final UserRepository userRepository;

    public GameProfileResult getMyProfile(Long userId) {
        User user = getUserOrElseThrow(userId);
        return GameProfileResult.from(user);
    }

    private User getUserOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}