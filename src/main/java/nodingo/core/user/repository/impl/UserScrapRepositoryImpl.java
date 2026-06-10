package nodingo.core.user.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.custom.UserScrapRepositoryCustom;

import java.util.Optional;
import java.util.List;

import static nodingo.core.keyword.domain.QKeyword.keyword;
import static nodingo.core.keyword.domain.QRecommendKeyword.recommendKeyword;
import static nodingo.core.user.domain.QUserScrap.userScrap;

@RequiredArgsConstructor
public class UserScrapRepositoryImpl implements UserScrapRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean isKeywordScrapped(Long userId, Long recommendKeywordId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(userScrap)
                .where(
                        userScrap.user.id.eq(userId),
                        userScrap.recommendKeyword.id.eq(recommendKeywordId)
                )
                .fetchFirst();

        return fetchOne != null;
    }

    @Override
    public Optional<UserScrap> findKeywordScrap(Long userId, Long recommendKeywordId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(userScrap)
                        .where(
                                userScrap.user.id.eq(userId),
                                userScrap.recommendKeyword.id.eq(recommendKeywordId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<UserScrap> findKeywordScrapsByUserId(Long userId, int page, int size) {
        return queryFactory
                .selectFrom(userScrap)
                .join(userScrap.recommendKeyword, recommendKeyword).fetchJoin()
                .join(recommendKeyword.keyword, keyword).fetchJoin()
                .where(
                        userScrap.user.id.eq(userId),
                        userScrap.recommendKeyword.isNotNull()
                )
                .orderBy(userScrap.id.desc())
                .offset((long) page * size)
                .limit(size + 1L)
                .fetch();
    }

    @Override
    public List<UserScrap> findAllByUserId(Long userId) {
        return queryFactory
                .selectFrom(userScrap)
                .join(userScrap.recommendKeyword, recommendKeyword).fetchJoin()
                .join(recommendKeyword.keyword, keyword).fetchJoin()
                .where(
                        userScrap.user.id.eq(userId),
                        userScrap.recommendKeyword.isNotNull()
                )
                .orderBy(userScrap.id.desc())
                .fetch();
    }
}
