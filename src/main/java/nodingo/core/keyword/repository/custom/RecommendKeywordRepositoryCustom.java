package nodingo.core.keyword.repository.custom;

import nodingo.core.keyword.domain.RecommendKeyword;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecommendKeywordRepositoryCustom {
    List<RecommendKeyword> findAllWithKeyword(Long userId);
    Optional<RecommendKeyword> findRecommend(Long userId, Long keywordId);
    List<RecommendKeyword> findTabsByUserAndDate(Long userId, LocalDate targetDate);
}
