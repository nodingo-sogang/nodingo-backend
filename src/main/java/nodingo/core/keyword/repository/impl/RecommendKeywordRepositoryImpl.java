package nodingo.core.keyword.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.keyword.domain.QKeyword;
import nodingo.core.keyword.domain.QRecommendKeyword;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.custom.RecommendKeywordRepositoryCustom;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static nodingo.core.keyword.domain.QKeyword.keyword;
import static nodingo.core.keyword.domain.QRecommendKeyword.recommendKeyword;

@RequiredArgsConstructor
public class RecommendKeywordRepositoryImpl implements RecommendKeywordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RecommendKeyword> findAllWithKeyword(Long userId) {
        QRecommendKeyword rk = recommendKeyword;
        QKeyword k = keyword;

        return queryFactory
                .selectFrom(rk)
                .join(rk.keyword, k).fetchJoin()
                .where(rk.user.id.eq(userId))
                .fetch();
    }

    @Override
    public Optional<RecommendKeyword> findRecommend(Long userId, Long keywordId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(recommendKeyword)
                        .join(recommendKeyword.keyword, keyword).fetchJoin()
                        .where(
                                recommendKeyword.user.id.eq(userId),
                                recommendKeyword.keyword.id.eq(keywordId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<RecommendKeyword> findTabsByUserAndDate(Long userId, LocalDate targetDate) {
        return queryFactory
                .selectFrom(recommendKeyword)
                .join(recommendKeyword.keyword, keyword).fetchJoin()
                .where(
                        recommendKeyword.user.id.eq(userId),
                        recommendKeyword.targetDate.eq(targetDate)
                )
                .fetch();
    }
}
