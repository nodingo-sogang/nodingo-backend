package nodingo.core.keyword.repository;

import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.custom.KeywordRepositoryCustom;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;



public interface KeywordRepository extends JpaRepository<Keyword, Long>, KeywordRepositoryCustom {
    List<Keyword> findAllByPersonaAndLevel(UserPersona persona, InterestLevel level, Pageable pageable);

    List<Keyword> findAllByParentIdAndLevel(Long parentId, InterestLevel level, Pageable pageable);

    List<Keyword> findByNormalizedWordIn(Collection<String> normalizedWords);

    Optional<Keyword> findByNormalizedWordAndLevelAndTargetDate(String normalizedWord, InterestLevel level, LocalDate targetDate);

    Optional<Keyword> findByWordAndLevelAndTargetDate(String word, InterestLevel level, LocalDate targetDate);

    List<Keyword> findAllByTargetDate(LocalDate targetDate);
}
