package nodingo.core.quiz.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.quiz.QuizNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.domain.UserQuizResult;
import nodingo.core.quiz.dto.command.QuizSubmitCommand;
import nodingo.core.quiz.dto.result.QuizRewardResult;
import nodingo.core.quiz.repository.QuizRepository;
import nodingo.core.quiz.repository.UserQuizResultRepository;
import nodingo.core.quiz.utils.QuizPolicy;
import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserBadge;
import nodingo.core.user.repository.UserBadgeRepository;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.service.command.UserRankingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final QuizPolicy quizPolicy;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRankingService userRankingService;

    public QuizRewardResult submitQuiz(QuizSubmitCommand command) {
        User user = getUserOrElseThrow(command);
        Quiz quiz = getQuizOrElseThrow(command);

        checkIfSubmitted(command);

        boolean isCorrect = quiz.getAnswerIndex().equals(command.getSelectedOptionIndex() - 1);
        userQuizResultRepository.save(UserQuizResult.create(user, quiz, isCorrect));

        List<String> newBadges = new ArrayList<>();
        int earnedXp = 0;

        if (isCorrect) {
            earnedXp = processCorrectAnswer(user, newBadges);
        }

        return QuizRewardResult.of(isCorrect, quiz.getAnswerIndex(), earnedXp, user, newBadges);
    }

    private int processCorrectAnswer(User user, List<String> newBadges) {
        int xp = calculateXp(user);

        if (user.getTotalQuizzesCompleted() == 1) {
            newBadges.add("FIRST_QUIZ");

            if (!userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), BadgeType.FIRST_QUIZ)) {
                userBadgeRepository.save(UserBadge.create(user, BadgeType.FIRST_QUIZ));
            }
        }

        return xp;
    }

    private int calculateXp(User user) {
        int xp = quizPolicy.getXpPerCorrectAnswer();

        LocalDate standardToday = getStandardToday();

        if (user.checkAndAddDailyQuiz(standardToday, quizPolicy.getDailyGoalCount())) {
            xp += quizPolicy.getDailyGoalBonusXp();
        }

        user.addXp(xp);

        userRankingService.updateWeeklyXp(user.getId(), xp);

        return xp;
    }

    private static LocalDate getStandardToday() {
        return java.time.LocalTime.now().isBefore(java.time.LocalTime.of(5, 0))
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();
    }

    private Quiz getQuizOrElseThrow(QuizSubmitCommand command) {
        return quizRepository.findById(command.getQuizId())
                .orElseThrow(() -> new QuizNotFoundException("퀴즈를 찾을 수 없습니다."));
    }

    private User getUserOrElseThrow(QuizSubmitCommand command) {
        return userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void checkIfSubmitted(QuizSubmitCommand command) {
        if (userQuizResultRepository.existsByUserIdAndQuizId(command.getUserId(), command.getQuizId())) {
            throw new IllegalArgumentException("이미 제출한 퀴즈입니다.");
        }
    }
}