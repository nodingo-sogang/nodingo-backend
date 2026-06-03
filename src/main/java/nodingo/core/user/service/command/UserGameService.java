package nodingo.core.user.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.utils.GamePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserGameService {

    private final UserRepository userRepository;
    private final GamePolicy gamePolicy;

    public void checkAndRewardAttendance(Long userId) {
        User user = getUserOrElseThrow(userId);

        LocalDate standardToday = getStandardToday();

        ifFirstVisit(user, standardToday);
    }

    private static LocalDate getStandardToday() {
        return LocalTime.now().isBefore(LocalTime.of(5, 0))
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();
    }

    private User getUserOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void ifFirstVisit(User user, LocalDate standardToday) {
        if (user.recordAttendance(standardToday)) {
            user.addXp(gamePolicy.getFirstVisitXp());
            log.info(">>>> [Attendance Reward] User {} checked in for date: {}. Earned {} XP",
                    user.getId(), standardToday, gamePolicy.getFirstVisitXp());
        }
    }
}