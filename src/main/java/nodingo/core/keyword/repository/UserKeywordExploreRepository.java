package nodingo.core.keyword.repository;

import nodingo.core.keyword.domain.UserKeywordExplore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface UserKeywordExploreRepository extends JpaRepository<UserKeywordExplore, Long> {
    @Query("SELECT e.keyword.id FROM UserKeywordExplore e WHERE e.user.id = :userId")
    Set<Long> findExploredKeywordIdsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndKeywordId(Long userId, Long keywordId);
}
