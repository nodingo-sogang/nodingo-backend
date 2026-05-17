package nodingo.core.keyword.repository;

import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.custom.KeywordRepositoryCustom;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long>, KeywordRepositoryCustom {

    Optional<Keyword> findByWordAndLevel(String word, InterestLevel level);

    // 중분류 조회
    List<Keyword> findAllByPersonaAndLevel(UserPersona persona, InterestLevel level);

    // 특정 중분류 하위의 소분류 조회
    List<Keyword> findAllByParentIdAndLevel(Long parentId, InterestLevel level);

    List<Keyword> findByNormalizedWordIn(Collection<String> normalizedWords);

    @Query("SELECT ka.keyword FROM KeywordAlias ka WHERE ka.alias = :alias")
    Optional<Keyword> findByAlias(@Param("alias") String alias);
}
