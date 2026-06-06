package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.dto.result.UserProfileResult;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService {

    private final UserRepository userRepository;

    public UserProfileResult getProfile(Long userId) {
        log.info(">>>> [User Profile Query] getProfile. userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return UserProfileResult.from(user);
    }
}