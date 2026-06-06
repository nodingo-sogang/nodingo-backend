package nodingo.core.user.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserInterest;
import nodingo.core.user.dto.command.InterestCommand;
import nodingo.core.user.dto.command.SaveOnboardingCommand;
import nodingo.core.user.repository.UserInterestRepository;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingService {

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final UserInterestRepository userInterestRepository;

    public List<Long> saveOnboardingInfo(SaveOnboardingCommand command) {
        log.info(">>>> [Onboarding] saveOnboardingInfo. userId={}", command.getUserId());
        User user = getUser(command.getUserId());
        userInterestRepository.deleteTodayInterests(user.getId(), LocalDate.now());
        user.completeOnboarding(command.getPersonas());
        userRepository.save(user);

        List<Long> allKeywordIds = extractAllKeywordIds(command);
        Map<Long, Keyword> keywordMap = getKeywordMap(allKeywordIds);

        InterestCommand interestCmd = command.getInterest();
        Keyword macroKeyword = getKeywordFromMap(keywordMap, interestCmd.getMacroId());
        UserInterest macroInterest = user.addInterest(macroKeyword, InterestLevel.MACRO, null, LocalDate.now());

        for (Long specificId : interestCmd.getSpecificIds()) {
            Keyword specificKeyword = getKeywordFromMap(keywordMap, specificId);
            user.addInterest(specificKeyword, InterestLevel.SPECIFIC, macroInterest, LocalDate.now());
        }

        log.info(">>>> [Onboarding] Onboarding info saved. userId={}, keywordCount={}",
                command.getUserId(), allKeywordIds.size());
        return allKeywordIds;
    }

    private User getUser(Long userId) {
        return userRepository.findByIdWithInterests(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private List<Long> extractAllKeywordIds(SaveOnboardingCommand command) {
        InterestCommand interest = command.getInterest();
        return Stream.concat(
                Stream.of(interest.getMacroId()),
                interest.getSpecificIds().stream()
        ).toList();
    }

    private Map<Long, Keyword> getKeywordMap(List<Long> ids) {
        return keywordRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Keyword::getId, k -> k));
    }

    private Keyword getKeywordFromMap(Map<Long, Keyword> map, Long id) {
        Keyword keyword = map.get(id);
        if (keyword == null) throw new KeywordNotFoundException("존재하지 않는 키워드입니다. ID: " + id);
        return keyword;
    }
}