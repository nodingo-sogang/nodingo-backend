package nodingo.core.quiz.utils;

public interface QuizPolicy {
    int getXpPerCorrectAnswer();
    int getDailyGoalCount();
    int getDailyGoalBonusXp();
}