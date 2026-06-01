package nodingo.core.keyword.repository;

import nodingo.core.keyword.domain.NewsKeyword;
import nodingo.core.keyword.repository.custom.NewsKeywordRepositoryCustom;
import nodingo.core.news.domain.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface NewsKeywordRepository extends JpaRepository<NewsKeyword, Long>, NewsKeywordRepositoryCustom {
    @Query("SELECT nk.keyword.id, COUNT(nk) FROM NewsKeyword nk WHERE nk.keyword.id IN :keywordIds GROUP BY nk.keyword.id")
    List<Object[]> countNewsRawByKeywordIds(@Param("keywordIds") List<Long> keywordIds);

    default Map<Long, Integer> countNewsByKeywordIds(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> results = countNewsRawByKeywordIds(keywordIds);
        return results.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Number) row[1]).intValue()
        ));
    }

    @Query("SELECT nk.news FROM NewsKeyword nk WHERE nk.keyword.id = :keywordId")
    List<News> findNewsEntitiesByKeywordId(@Param("keywordId") Long keywordId);
}