package nodingo.core.user.service.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.service.command.RecommendKeywordInitService;
import nodingo.core.quiz.service.command.QuizGenerationService;
import nodingo.core.user.domain.OnboardingStatus;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.service.vector.UserVectorService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingAsyncService {

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final UserVectorService userVectorService;
    private final RecommendKeywordInitService recommendKeywordInitService;
    private final QuizGenerationService quizGenerationService;

    @Async
    public void initEmbeddingAndRecommend(Long userId, List<Long> keywordIds) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found. userId=" + userId));
            List<Keyword> keywords = keywordRepository.findAllById(keywordIds);

            userVectorService.initUserEmbedding(user, keywords);
            recommendKeywordInitService.initForNewUser(user);

            for (Long keywordId : keywordIds) {
                quizGenerationService.generateForOnboarding(keywordId, userId);
            }

            user.updateOnboardingStatus(OnboardingStatus.COMPLETED);
            userRepository.save(user);
            log.info(">>>> [OnboardingAsync] Onboarding initialization completed. userId={}", userId);

        } catch (Exception e) {
            log.error(">>>> [OnboardingAsync] Onboarding initialization failed. userId={}", userId, e);
            userRepository.findById(userId).ifPresent(u -> {
                u.updateOnboardingStatus(OnboardingStatus.FAILED);
                userRepository.save(u);
            });
        }
    }
}