package nodingo.core.user.service.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.userEmbedding.UserEmbedding;
import nodingo.core.global.exception.ai.AiIntegrationException;
import nodingo.core.global.exception.ai.AiRateLimitException;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.global.exception.scrap.ScrapNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.global.metrics.MonitoringMetrics;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVectorService {

    private final AiClient aiClient;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final KeywordRepository keywordRepository;
    private final MonitoringMetrics metrics;

    @Transactional
    public void initUserEmbedding(User user, List<Keyword> selectedKeywords) {
        log.info(">>>> [UserVector] Starting initial embedding. userId={}", user.getId());

        List<UserEmbedding.InterestKeyword> keywordItems = selectedKeywords.stream()
                .map(keyword -> UserEmbedding.InterestKeyword.builder()
                        .keywordId(keyword.getId())
                        .word(keyword.getWord())
                        .embedding(keyword.getEmbedding())
                        .build())
                .toList();

        UserEmbedding.InitRequest request = UserEmbedding.InitRequest.builder()
                .userId(user.getId())
                .interestKeywords(keywordItems)
                .build();

        try {
            metrics.recordAiCall("userVector.initEmbedding");
            UserEmbedding.Response response = aiClient.initUserEmbedding(request);
            if (response == null || response.getEmbedding() == null) {
                throw new AiIntegrationException("Received empty embedding response from AI server.");
            }
            user.updateEmbedding(response.getEmbedding());
            log.info(">>>> [UserVector] Embedding initialized. userId={}", user.getId());
        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("userVector.initEmbedding", "RateLimitError");
            log.error(">>>> [UserVector] OpenAI rate limit exceeded (429). userId={}", user.getId(), e);
            throw new AiIntegrationException("Failed to initialize user embedding: rate limit exceeded.");
        } catch (Exception e) {
            metrics.recordAiFailure("userVector.initEmbedding", e.getClass().getSimpleName());
            log.error(">>>> [UserVector] AI call failed. userId={}, error: {}", user.getId(), e.getMessage(), e);
            throw new AiIntegrationException("Failed to initialize user embedding via AI server.");
        }
    }

    @Async("embeddingTaskExecutor")
    @Transactional
    public void updateUserEmbeddingAsync(Long userId, Long newsId) {
        try {
            UserScrap scrap = newsRepository.findScrapDetail(userId, newsId)
                    .orElseThrow(() -> new ScrapNotFoundException("Scrap not found"));

            User user = scrap.getUser();
            News news = scrap.getNews();
            UserEmbedding.Activity activity = UserEmbedding.Activity.createNewsScrap(news, 0.5);

            sendUpdateToAi(user, activity, "userVector.updateEmbedding.news");

        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("userVector.updateEmbedding.news", "RateLimitError");
            log.error(">>>> [UserVector] OpenAI rate limit exceeded (429). userId={}", userId, e);
        } catch (Exception e) {
            log.error(">>>> [UserVector] News embedding update failed. userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    @Async("userScrapEmbeddingExecutor")
    @Transactional
    public void updateKeywordEmbeddingAsync(Long userId, Long keywordId) {
        try {
            User user = getUserElseThrow(userId);
            Keyword keyword = getKeywordElseThrow(keywordId);
            UserEmbedding.Activity activity = UserEmbedding.Activity.createKeywordActivity(keyword, 0.8);

            sendUpdateToAi(user, activity, "userVector.updateEmbedding.keyword");

        } catch (AiRateLimitException e) {
            metrics.recordAiFailure("userVector.updateEmbedding.keyword", "RateLimitError");
            log.error(">>>> [UserVector] OpenAI rate limit exceeded (429). userId={}", userId, e);
        } catch (Exception e) {
            log.error(">>>> [UserVector] Keyword embedding update failed. userId={}, error: {}", userId, e.getMessage(), e);
        }
    }

    private void sendUpdateToAi(User user, UserEmbedding.Activity activity, String feature) {
        UserEmbedding.UpdateRequest req = UserEmbedding.UpdateRequest.builder()
                .userId(user.getId())
                .oldEmbedding(user.getEmbedding())
                .activities(List.of(activity))
                .decay(0.7)
                .build();

        try {
            metrics.recordAiCall(feature);
            UserEmbedding.Response res = aiClient.updateUserEmbedding(req);
            if (res != null && res.getEmbedding() != null) {
                user.updateEmbedding(res.getEmbedding());
                userRepository.saveAndFlush(user);
                log.info(">>>> [UserVector] Embedding updated. userId={}, activity={}", user.getId(), activity.getType());
            }
        } catch (AiRateLimitException e) {
            metrics.recordAiFailure(feature, "RateLimitError");
            log.error(">>>> [UserVector] OpenAI rate limit exceeded (429). userId={}", user.getId(), e);
        } catch (Exception e) {
            metrics.recordAiFailure(feature, e.getClass().getSimpleName());
            log.error(">>>> [UserVector] Embedding update failed. userId={}, error: {}", user.getId(), e.getMessage(), e);
        }
    }

    private Keyword getKeywordElseThrow(Long keywordId) {
        return keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("Keyword not found"));
    }

    private User getUserElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}