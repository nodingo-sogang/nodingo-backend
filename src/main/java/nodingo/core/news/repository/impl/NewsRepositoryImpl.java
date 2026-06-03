package nodingo.core.news.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.news.repository.custom.NewsRepositoryCustom;
import nodingo.core.user.domain.UserScrap;
import java.util.Optional;

import static nodingo.core.keyword.domain.QKeyword.keyword;
import static nodingo.core.keyword.domain.QNewsKeyword.newsKeyword;
import static nodingo.core.news.domain.QNews.news;
import static nodingo.core.user.domain.QUser.user;
import static nodingo.core.user.domain.QUserScrap.userScrap;

@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<UserScrap> findScrapDetail(Long userId, Long newsId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(userScrap)
                        .join(userScrap.user, user).fetchJoin()
                        .join(userScrap.news, news).fetchJoin()
                        .leftJoin(news.newsKeywords, newsKeyword).fetchJoin()
                        .leftJoin(newsKeyword.keyword, keyword).fetchJoin()
                        .where(
                                user.id.eq(userId),
                                news.id.eq(newsId)
                        )
                        .fetchOne()
        );
    }
}
