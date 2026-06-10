package nodingo.core.keyword.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.service.command.UserRankingService;
import nodingo.core.user.utils.GamePolicy;
import nodingo.core.global.exception.recommendKeyword.RecommendKeywordNotFoundException;
import nodingo.core.global.exception.scrap.DuplicateScrapException;
import nodingo.core.global.exception.scrap.ScrapNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.repository.UserScrapRepository;
import nodingo.core.user.service.vector.UserVectorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendKeywordScrapService {

    private final UserScrapRepository userScrapRepository;
    private final UserRepository userRepository;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final KeywordRepository keywordRepository;
    private final UserVectorService userVectorService;
    private final GamePolicy gamePolicy;
    private final UserRankingService userRankingService;

    public void addScrap(Long userId, Long keywordId) {
        log.info(">>>> [Scrap] addScrap. userId={}, keywordId={}", userId, keywordId);

        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("해당 키워드를 찾을 수 없습니다."));

        if (userScrapRepository.isKeywordScrapped(userId, keywordId)) {
            throw new DuplicateScrapException("이미 스크랩한 키워드입니다.");
        }

        User user = getUser(userId);

        Optional<RecommendKeyword> recommendKeywordOpt = recommendKeywordRepository.findRecommend(userId, keywordId);

        UserScrap userScrap;
        if (recommendKeywordOpt.isPresent()) {
            userScrap = UserScrap.createRecommendKeywordScrap(user, recommendKeywordOpt.get());
        } else {
            userScrap = UserScrap.createPureKeywordScrap(user, keyword);
        }

        userScrapRepository.save(userScrap);

        user.addKeywordScrap();
        int scrapXp = gamePolicy.getScrapXp();
        user.addXp(scrapXp);
        userRankingService.updateWeeklyXp(user.getId(), scrapXp);

        log.info(">>>> [Scrap] Scrap added. userId={}, keywordId={}, xp={}", userId, keywordId, scrapXp);
        userVectorService.updateKeywordEmbeddingAsync(userId, keywordId);
    }

    public void removeScrap(Long userId, Long keywordId) {
        log.info(">>>> [Scrap] removeScrap. userId={}, keywordId={}", userId, keywordId);

        UserScrap scrap = userScrapRepository.findByUserIdAndKeywordId(userId, keywordId)
                .orElseThrow(() -> new ScrapNotFoundException("스크랩 기록을 찾을 수 없습니다."));

        User user = getUser(userId);
        user.removeKeywordScrap();

        int scrapXp = gamePolicy.getScrapXp();
        user.removeXp(scrapXp);

        userRankingService.updateWeeklyXp(user.getId(), -scrapXp);
        log.info(">>>> [Scrap] Scrap removed. userId={}, keywordId={}, xp={}", userId, keywordId, scrapXp);

        userScrapRepository.delete(scrap);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void isAlreadyScrapped(Long userId, RecommendKeyword rk) {
        if (userScrapRepository.isKeywordScrapped(userId, rk.getId())) {
            throw new DuplicateScrapException("이미 스크랩한 키워드입니다.");
        }
    }

    private RecommendKeyword getRk(Long userId, Long keywordId, String message) {
        return recommendKeywordRepository.findRecommend(userId, keywordId)
                .orElseThrow(() -> new RecommendKeywordNotFoundException(message));
    }

    private UserScrap getScrap(Long userId, RecommendKeyword rk) {
        return userScrapRepository.findKeywordScrap(userId, rk.getId())
                .orElseThrow(() -> new ScrapNotFoundException("스크랩 기록을 찾을 수 없습니다."));
    }
}