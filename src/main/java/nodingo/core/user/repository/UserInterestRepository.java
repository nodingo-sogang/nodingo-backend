package nodingo.core.user.repository;

import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
    @Modifying(clearAutomatically = true)
    @Query("delete from UserInterest ui where ui.user.id = :userId and ui.targetDate = :today")
    void deleteTodayInterests(@Param("userId") Long userId, @Param("today") LocalDate today);

    List<UserInterest> findByUserId(Long userId);

    @Query("SELECT i.keyword.id FROM UserInterest i WHERE i.user.id = :userId")
    Set<Long> findScrappedKeywordIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserInterest u where u.user.id = :userId and u.level = :level")
    void deleteByUserIdAndLevel(@Param("userId") Long userId, @Param("level") InterestLevel level);
}
