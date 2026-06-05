package nodingo.core.user.service.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.service.command.RecommendKeywordInitService;
import nodingo.core.quiz.service.command.QuizGenerationService;
import nodingo.core.user.domain.OnboardingStatus;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.service.vector.UserVectorService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingAsyncService {

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final UserVectorService userVectorService;
    private final RecommendKeywordInitService recommendKeywordInitService;
    private final QuizGenerationService quizGenerationService;
    private final MonitoringMetrics metrics;

    private final ThreadPoolTaskExecutor onboardingExecutor;

    @Async("onboardingExecutor")
    public void initEmbeddingAndRecommend(Long userId, List<Long> keywordIds) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found. userId=" + userId));
            List<Keyword> keywords = keywordRepository.findAllById(keywordIds);

            log.info(">>>> [OnboardingAsync] Starting user embedding init. userId={}", userId);
            metrics.recordAiCall("onboarding.initEmbedding");
            userVectorService.initUserEmbedding(user, keywords);

            log.info(">>>> [OnboardingAsync] Starting recommend keyword init. userId={}", userId);
            metrics.recordAiCall("onboarding.initRecommend");
            recommendKeywordInitService.initForNewUser(user);

            log.info(">>>> [OnboardingAsync] Starting quiz generation. userId={}, keywordCount={}", userId, keywordIds.size());
            List<CompletableFuture<Void>> futures = keywordIds.stream()
                    .map(keywordId -> CompletableFuture.runAsync(() -> {
                        quizGenerationService.generateForOnboarding(keywordId, userId);
                    }, onboardingExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            user.updateOnboardingStatus(OnboardingStatus.COMPLETED);
            userRepository.save(user);
            log.info(">>>> [OnboardingAsync] Onboarding initialization completed. userId={}", userId);

        } catch (AiRateLimitException e) {
            log.error(">>>> [OnboardingAsync] OpenAI rate limit exceeded (429). userId={}", userId, e);
            metrics.recordAiFailure("onboarding", "RateLimitError");
            updateOnboardingStatus(userId, OnboardingStatus.FAILED);
        } catch (Exception e) {
            log.error(">>>> [OnboardingAsync] Onboarding initialization failed. userId={}", userId, e);
            updateOnboardingStatus(userId, OnboardingStatus.FAILED);
        }
    }

    private void updateOnboardingStatus(Long userId, OnboardingStatus status) {
        userRepository.findById(userId).ifPresent(u -> {
            u.updateOnboardingStatus(status);
            userRepository.save(u);
        });
    }
}