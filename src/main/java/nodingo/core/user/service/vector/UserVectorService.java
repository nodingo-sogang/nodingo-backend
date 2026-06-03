package nodingo.core.user.service.vector;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import nodingo.core.ai.client.AiClient;
import nodingo.core.ai.dto.userEmbedding.UserEmbedding;
import nodingo.core.global.exception.ai.AiIntegrationException;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.global.exception.scrap.ScrapNotFoundException;
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

    @Transactional
    public void initUserEmbedding(User user, List<Keyword> selectedKeywords) {
        log.info(">>>> [UserVectorService] Starting initial embedding generation - userId: {}", user.getId());

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
            UserEmbedding.Response response = aiClient.initUserEmbedding(request);
            if (response == null || response.getEmbedding() == null) {
                throw new AiIntegrationException("Received empty embedding response from AI server.");
            }
            user.updateEmbedding(response.getEmbedding());
        } catch (Exception e) {
            log.error(">>>> [UserVectorService] AI server communication failed - userId: {}, error: {}", user.getId(), e.getMessage());
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

            sendUpdateToAi(user, activity);

        } catch (Exception e) {
            log.error(">>>> [AI Error] News Update Failed: {}", e.getMessage());
        }
    }

    @Async("embeddingTaskExecutor")
    @Transactional
    public void updateKeywordEmbeddingAsync(Long userId, Long keywordId) {
        try {
            User user = getUserElseThrow(userId);

            Keyword keyword = getKeywordElseThrow(keywordId);

            UserEmbedding.Activity activity = UserEmbedding.Activity.createKeywordActivity(keyword, 0.8);

            sendUpdateToAi(user, activity);

        } catch (Exception e) {
            log.error(">>>> [AI Error] Keyword Update Failed: {}", e.getMessage());
        }
    }

    private Keyword getKeywordElseThrow(Long keywordId) {
        return keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("Keyword not found"));
    }

    private User getUserElseThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user;
    }

    private void sendUpdateToAi(User user, UserEmbedding.Activity activity) {
        UserEmbedding.UpdateRequest req = UserEmbedding.UpdateRequest.builder()
                .userId(user.getId())
                .oldEmbedding(user.getEmbedding())
                .activities(List.of(activity))
                .decay(0.7)
                .build();

        UserEmbedding.Response res = aiClient.updateUserEmbedding(req);

        if (res != null && res.getEmbedding() != null) {
            user.updateEmbedding(res.getEmbedding());
            userRepository.saveAndFlush(user);
            log.info(">>>> [AI Success] User Embedding Updated - ID: {}, Activity: {}", user.getId(), activity.getType());
        }
    }
}