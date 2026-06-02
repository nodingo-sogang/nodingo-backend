package nodingo.core.keyword.repository.custom;

import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.news.domain.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface NewsKeywordRepositoryCustom {
    List<NewsKeyword> findTopByKeywordId(Long keywordId, int limit);
    Slice<News> findNewsSliceByKeywordId(Long keywordId, Pageable pageable);
}
