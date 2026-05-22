package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.result.KeywordListResult;
import nodingo.core.user.dto.result.KeywordResult;
import nodingo.core.user.dto.result.PersonaListResult;
import nodingo.core.user.dto.result.PersonaResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingQueryService {

    private final KeywordRepository keywordRepository;
    private static final int KEYWORD_LIMIT = 6;

    public PersonaListResult getPersonas() {
        List<PersonaResult> results = Arrays.stream(UserPersona.values())
                .map(PersonaResult::from)
                .toList();
        return PersonaListResult.from(results);
    }

    public KeywordListResult getMacroKeywords(UserPersona persona) {
        LocalDate targetDate = getTargetDate();

        List<KeywordResult> results = keywordRepository
                .findMacrosForOnboarding(persona, targetDate, KEYWORD_LIMIT)
                .stream()
                .map(KeywordResult::from)
                .toList();
        return KeywordListResult.from(results);
    }

    public KeywordListResult getSpecificKeywords(Long macroId) {
        LocalDate targetDate = getTargetDate();

        List<KeywordResult> results = keywordRepository
                .findSpecificsForOnboarding(macroId, targetDate, KEYWORD_LIMIT)
                .stream()
                .map(KeywordResult::from)
                .toList();
        return KeywordListResult.from(results);
    }

    private static LocalDate getTargetDate() {
        return LocalTime.now().isBefore(LocalTime.of(5, 0))
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();
    }
}