package nodingo.core.quiz.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.quiz.domain.Quiz;
import nodingo.core.quiz.repository.custom.QuizRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

import static nodingo.core.quiz.domain.QQuiz.quiz;

@Repository
@RequiredArgsConstructor
public class QuizRepositoryImpl implements QuizRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Quiz> findRecentQuizzes(Long keywordId) {
        return queryFactory.selectFrom(quiz)
                .leftJoin(quiz.news).fetchJoin()
                .where(quiz.keyword.id.eq(keywordId))
                .orderBy(quiz.id.desc())
                .limit(3)
                .fetch()
                .stream()
                .sorted(Comparator.comparingLong(Quiz::getId))
                .toList();
    }
}
