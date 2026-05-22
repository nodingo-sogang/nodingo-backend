package nodingo.core.keyword.repository.custom;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.dto.query.KeywordCandidate;
import nodingo.core.user.domain.UserPersona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface KeywordRepositoryCustom {
    List<KeywordCandidate> findCandidateKeywords(LocalDateTime startTime, LocalDateTime endTime);
    List<Keyword> findMacrosByDate(Collection<String> words, LocalDate targetDate);
    List<Keyword> findSpecificsByDate(Collection<String> normalizedWords, LocalDate targetDate);
    List<Keyword> findMacrosForOnboarding(UserPersona persona, LocalDate targetDate, int limit);
    List<Keyword> findSpecificsForOnboarding(Long macroId, LocalDate targetDate, int limit);
}