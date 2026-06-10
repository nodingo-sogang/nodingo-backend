package nodingo.core.user.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.custom.UserScrapRepositoryCustom;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

import static nodingo.core.keyword.domain.QKeyword.keyword;
import static nodingo.core.keyword.domain.QRecommendKeyword.recommendKeyword;
import static nodingo.core.user.domain.QUserScrap.userScrap;

@RequiredArgsConstructor
public class UserScrapRepositoryImpl implements UserScrapRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean isKeywordScrapped(Long userId, Long keywordId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(userScrap)
                .where(
                        userScrap.user.id.eq(userId),
                        userScrap.keyword.id.eq(keywordId)
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
    public List<UserScrap> findKeywordScrapsByUserId(Long userId, Pageable pageable) {
        return queryFactory
                .selectFrom(userScrap)
                .join(userScrap.keyword, keyword).fetchJoin()
                .leftJoin(userScrap.recommendKeyword, recommendKeyword).fetchJoin()
                .where(
                        userScrap.user.id.eq(userId),
                        userScrap.keyword.isNotNull()
                )
                .orderBy(userScrap.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
    }

    @Override
    public List<UserScrap> findAllByUserId(Long userId) {
        return queryFactory
                .selectFrom(userScrap)
                .join(userScrap.keyword, keyword).fetchJoin()
                .where(userScrap.user.id.eq(userId))
                .orderBy(userScrap.id.desc())
                .fetch();
    }

    @Override
    public Optional<UserScrap> findByUserIdAndKeywordId(Long userId, Long keywordId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(userScrap)
                        .where(
                                userScrap.user.id.eq(userId),
                                userScrap.keyword.id.eq(keywordId)
                        )
                        .fetchOne()
        );
    }
}
