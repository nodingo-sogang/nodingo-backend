package nodingo.core.user.service.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.service.command.RecommendKeywordInitService;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.service.vector.UserVectorService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingAsyncService {

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final UserVectorService userVectorService;
    private final RecommendKeywordInitService recommendKeywordInitService;

    @Async
    @Transactional
    public void initEmbeddingAndRecommend(Long userId, List<Long> keywordIds) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
            List<Keyword> keywords = keywordRepository.findAllById(keywordIds);
            userVectorService.initUserEmbedding(user, keywords);
            recommendKeywordInitService.initForNewUser(user);
        } catch (Exception e) {
            log.error(">>>> [OnboardingAsync] User Embedding Initialization failed. userId={}", userId, e);
        }
    }
}