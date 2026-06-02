package nodingo.core.keyword.repository;

import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.custom.RecommendKeywordRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RecommendKeywordRepository extends JpaRepository<RecommendKeyword, Long>, RecommendKeywordRepositoryCustom {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RecommendKeyword r WHERE r.user.id = :userId AND r.targetDate = :targetDate")
    void deleteByUserIdAndTargetDate(@Param("userId") Long userId, @Param("targetDate") LocalDate targetDate);

    boolean existsByUserIdAndTargetDate(Long userId, LocalDate targetDate);

    Optional<RecommendKeyword> findByKeywordIdAndTargetDate(Long keywordId, LocalDate targetDate);

    Optional<RecommendKeyword> findByUserIdAndKeywordIdAndTargetDate(Long userId, Long keywordId, LocalDate targetDate);
}