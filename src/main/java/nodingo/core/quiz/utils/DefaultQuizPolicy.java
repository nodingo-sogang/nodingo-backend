package nodingo.core.quiz.utils;


import org.springframework.stereotype.Component;

@Component
public class DefaultQuizPolicy implements QuizPolicy {
    @Override
    public int getXpPerCorrectAnswer() { return 20; }

    @Override
    public int getDailyGoalCount() { return 2; }

    @Override
    public int getDailyGoalBonusXp() { return 50; }
}