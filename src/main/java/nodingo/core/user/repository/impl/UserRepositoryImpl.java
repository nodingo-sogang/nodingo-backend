package nodingo.core.user.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.user.domain.QUser;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.repository.custom.UserRepositoryCustom;

import java.util.List;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<User> fetchLeaderboardByPersona(UserPersona persona, int limit, int offset) {
        QUser user = QUser.user;
        return queryFactory
                .selectFrom(user)
                .join(user.personas).fetchJoin()
                .where(user.personas.contains(persona))
                .orderBy(user.weeklyXp.desc(), user.id.asc())
                .limit(limit)
                .offset(offset)
                .fetch();
    }

    @Override
    public long countHigherRankedByPersona(UserPersona persona, int weeklyXp, Long userId) {
        QUser user = QUser.user;
        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        user.personas.contains(persona),
                        user.weeklyXp.gt(weeklyXp)
                                .or(user.weeklyXp.eq(weeklyXp).and(user.id.lt(userId)))
                )
                .fetchOne();
        return count != null ? count : 0L;
    }
}
