package nodingo.core.user.service.command;

import lombok.RequiredArgsConstructor;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.utils.GamePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserGameService {

    private final UserRepository userRepository;
    private final GamePolicy gamePolicy;

    public void checkAndRewardAttendance(Long userId) {
        User user = getUserOrElseThrow(userId);
        ifFirstVisit(user);
    }

    private User getUserOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }


    private void ifFirstVisit(User user) {
        if (user.recordAttendance()) {
            user.addXp(gamePolicy.getFirstVisitXp());
        }
    }
}
