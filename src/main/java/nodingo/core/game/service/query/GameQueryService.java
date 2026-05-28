package nodingo.core.game.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.game.dto.result.DailyGoalsResult;
import nodingo.core.game.dto.result.GameProfileResult;
import nodingo.core.game.dto.result.UserGameResult;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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