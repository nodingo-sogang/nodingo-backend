package nodingo.core.news.service.command;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import nodingo.core.global.exception.news.NewsNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.global.exception.scrap.DuplicateScrapException;
import nodingo.core.news.domain.News;
import nodingo.core.news.repository.NewsRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.repository.UserScrapRepository;
import nodingo.core.user.service.vector.UserVectorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NewsScrapService {

    private final UserScrapRepository userScrapRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final UserVectorService userVectorService;

    public void addScrap(Long userId, Long newsId) {
        log.info(">>>> [News Scrap] addScrap. userId={}, newsId={}", userId, newsId);
        ifScrapped(userId, newsId);
        createUserScrap(userId, newsId);
        userVectorService.updateUserEmbeddingAsync(userId, newsId);
        log.info(">>>> [News Scrap] Scrap added. userId={}, newsId={}", userId, newsId);
    }

    public void removeScrap(Long userId, Long newsId) {
        log.info(">>>> [News Scrap] removeScrap. userId={}, newsId={}", userId, newsId);
        UserScrap scrap = getOrElseThrow(userId, newsId);
        userScrapRepository.delete(scrap);
        log.info(">>>> [News Scrap] Scrap removed. userId={}, newsId={}", userId, newsId);
    }

    private void createUserScrap(Long userId, Long newsId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다."));

        userScrapRepository.save(UserScrap.createNewsScrap(user, news));
    }

    private void ifScrapped(Long userId, Long newsId) {
        if (userScrapRepository.existsByUserIdAndNewsId(userId, newsId)) {
            throw new DuplicateScrapException("이미 스크랩한 뉴스입니다.");
        }
    }

    private UserScrap getOrElseThrow(Long userId, Long newsId) {
        return userScrapRepository.findByUserIdAndNewsId(userId, newsId)
                .orElseThrow(() -> new DuplicateScrapException("스크랩하지 않은 뉴스입니다."));
    }
}