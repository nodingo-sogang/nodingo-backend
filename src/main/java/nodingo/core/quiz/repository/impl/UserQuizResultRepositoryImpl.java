package nodingo.core.quiz.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.quiz.repository.custom.UserQuizResultRepositoryCustom;

import static nodingo.core.quiz.domain.QUserQuizResult.userQuizResult;

import java.util.List;

@RequiredArgsConstructor
public class UserQuizResultRepositoryImpl implements UserQuizResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findSolvedQuizIds(Long userId, List<Long> quizIds) {
        if (quizIds == null || quizIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(userQuizResult.quiz.id)
                .from(userQuizResult)
                .where(
                        userQuizResult.user.id.eq(userId),
                        userQuizResult.quiz.id.in(quizIds)
                )
                .fetch();
    }
}