package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.global.util.DateUtil;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.OnboardingStatus;
import nodingo.core.user.domain.UserPersona;
import nodingo.core.user.dto.result.*;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingQueryService {

    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private static final int KEYWORD_LIMIT = 6;

    public PersonaListResult getPersonas() {
        log.info(">>>> [Onboarding Query] getPersonas.");
        List<PersonaResult> results = Arrays.stream(UserPersona.values())
                .map(PersonaResult::from)
                .toList();
        return PersonaListResult.from(results);
    }

    public KeywordListResult getMacroKeywords(UserPersona persona) {
        LocalDate targetDate = DateUtil.getTargetDate();
        log.info(">>>> [Onboarding Query] getMacroKeywords. persona={}, targetDate={}", persona, targetDate);
        List<KeywordResult> results = keywordRepository
                .findMacrosForOnboarding(persona, targetDate, KEYWORD_LIMIT)
                .stream()
                .map(KeywordResult::from)
                .toList();
        log.info(">>>> [Onboarding Query] getMacroKeywords result. persona={}, count={}", persona, results.size());
        return KeywordListResult.from(results);
    }

    public OnboardingStatusResult getOnboardingStatus(Long userId) {
        log.info(">>>> [Onboarding Query] getOnboardingStatus. userId={}", userId);
        OnboardingStatus status = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."))
                .getOnboardingStatus();
        log.info(">>>> [Onboarding Query] getOnboardingStatus result. userId={}, status={}", userId, status);
        return OnboardingStatusResult.of(status);
    }

    public KeywordListResult getSpecificKeywords(Long macroId) {
        LocalDate targetDate = DateUtil.getTargetDate();
        log.info(">>>> [Onboarding Query] getSpecificKeywords. macroId={}, targetDate={}", macroId, targetDate);
        List<KeywordResult> results = keywordRepository
                .findSpecificsForOnboarding(macroId, targetDate, KEYWORD_LIMIT)
                .stream()
                .map(KeywordResult::from)
                .toList();
        log.info(">>>> [Onboarding Query] getSpecificKeywords result. macroId={}, count={}", macroId, results.size());
        return KeywordListResult.from(results);
    }
}