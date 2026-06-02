package nodingo.core.keyword.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.util.SliceUtil;
import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.repository.custom.NewsKeywordRepositoryCustom;
import nodingo.core.news.domain.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;

import static nodingo.core.keyword.domain.QNewsKeyword.newsKeyword;
import static nodingo.core.news.domain.QNews.news;

@Repository
@RequiredArgsConstructor
public class NewsKeywordRepositoryImpl implements NewsKeywordRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NewsKeyword> findTopByKeywordId(Long keywordId, int limit) {
        return queryFactory
                .selectFrom(newsKeyword)
                .join(newsKeyword.news, news).fetchJoin()
                .where(newsKeyword.keyword.id.eq(keywordId))
                .orderBy(newsKeyword.weight.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Slice<News> findNewsSliceByKeywordId(Long keywordId, Pageable pageable) {

        List<News> content = queryFactory
                .select(newsKeyword.news)
                .from(newsKeyword)
                .where(newsKeyword.keyword.id.eq(keywordId))
                .orderBy(newsKeyword.weight.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return SliceUtil.checkLastPage(pageable, content);
    }
}