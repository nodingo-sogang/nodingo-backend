package nodingo.core.news.repository;

import nodingo.core.news.domain.News;
import nodingo.core.news.repository.custom.NewsRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Long>, NewsRepositoryCustom {

    boolean existsByUri(String uri);

    @Query("select n from News n left join fetch n.newsKeywords nk left join fetch nk.keyword where n.id = :id")
    Optional<News> findByIdWithKeywords(@Param("id") Long id);

    List<News> findAllByDateTimePubBetweenAndEmbeddingIsNotNull(LocalDateTime start, LocalDateTime end);
}
