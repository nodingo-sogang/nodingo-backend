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

    private final ThreadPoolTaskExecutor onboardingExecutor;

    @Async("onboardingExecutor")
    public void initEmbeddingAndRecommend(Long userId, List<Long> keywordIds) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found. userId=" + userId));
            List<Keyword> keywords = keywordRepository.findAllById(keywordIds);

            // 1. 유저 임베딩 초기화
            userVectorService.initUserEmbedding(user, keywords);

            // 2. 추천 키워드 초기화
            recommendKeywordInitService.initForNewUser(user);

            // 3. 🌟 [대형 최적화] 기존 for 루프를 깨부수고 CompletableFuture를 통한 동시 병렬 처리 가동!
            List<CompletableFuture<Void>> futures = keywordIds.stream()
                    .map(keywordId -> CompletableFuture.runAsync(() -> {
                        // 각 스레드가 키워드를 하나씩 나눠 잡고 파이썬 서버를 동시에 찌릅니다.
                        quizGenerationService.generateForOnboarding(keywordId, userId);
                    }, onboardingExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 4. 최종 온보딩 완료 상태 세팅
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