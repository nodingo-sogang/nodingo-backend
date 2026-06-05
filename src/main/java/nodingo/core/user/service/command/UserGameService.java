package nodingo.core.user.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserBadge;
import nodingo.core.user.repository.UserBadgeRepository;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.utils.GamePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserGameService {

    private final UserRepository userRepository;
    private final GamePolicy gamePolicy;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRankingService userRankingService;

    public void checkAndRewardAttendance(Long userId) {
        User user = getUserOrElseThrow(userId);
        LocalDate standardToday = getStandardToday();

        ifFirstVisit(user, standardToday);

        checkAndSaveExploreBadges(user);
    }

    private void ifFirstVisit(User user, LocalDate standardToday) {
        if (user.recordAttendance(standardToday)) {
            int firstVisitXp = gamePolicy.getFirstVisitXp();
            user.addXp(firstVisitXp);

            userRankingService.updateWeeklyXp(user.getId(), firstVisitXp);

            if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.FIRST_VISIT)) {
                userBadgeRepository.save(UserBadge.create(user, BadgeType.FIRST_VISIT));
            }

            int currentStreak = user.getConsecutiveAttendanceDays();
            if (currentStreak >= 7) {
                if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.ATTENDANCE_7)) {
                    userBadgeRepository.save(UserBadge.create(user, BadgeType.ATTENDANCE_7));
                    log.info(">>>> [Badge Achievement] User {} earned 7-day Streak Badge!", user.getId());
                }
            }
            if (currentStreak >= 30) {
                if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.ATTENDANCE_30)) {
                    userBadgeRepository.save(UserBadge.create(user, BadgeType.ATTENDANCE_30));
                    log.info(">>>> [Badge Achievement] User {} earned 30-day Streak Badge!", user.getId());
                }
            }

            log.info(">>>> [Attendance Reward] User {} checked in for date: {}. Earned {} XP. Current Streak: {}",
                    user.getId(), standardToday, firstVisitXp, currentStreak);
        }
    }

    private void checkAndSaveExploreBadges(User user) {
        int exploredCount = user.getTotalNodesExplored();

        if (exploredCount >= 1) {
            if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.FIRST_EXPLORE)) {
                userBadgeRepository.save(UserBadge.create(user, BadgeType.FIRST_EXPLORE));
                log.info(">>>> [Badge Achievement] User {} earned FIRST_EXPLORE Badge!", user.getId());
            }
        }

        if (exploredCount >= 10) {
            if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.EXPLORE_10)) {
                userBadgeRepository.save(UserBadge.create(user, BadgeType.EXPLORE_10));
                log.info(">>>> [Badge Achievement] User {} earned EXPLORE_10 Badge!", user.getId());
            }
        }

        if (exploredCount >= 50) {
            if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.EXPLORE_50)) {
                userBadgeRepository.save(UserBadge.create(user, BadgeType.EXPLORE_50));
                log.info(">>>> [Badge Achievement] User {} earned EXPLORE_50 Badge!", user.getId());
            }
        }
    }

    private static LocalDate getStandardToday() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private User getUserOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}