package nodingo.core.keyword.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.BadgeType;
import nodingo.core.user.domain.UserBadge;
import nodingo.core.user.repository.UserBadgeRepository;
import nodingo.core.global.exception.scrap.DuplicateScrapException;
import nodingo.core.global.exception.scrap.ScrapNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.domain.RecommendKeyword;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.domain.UserScrap;
import nodingo.core.user.event.UserXpChangedEvent; // 이벤트 객체 import
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.repository.UserScrapRepository;
import nodingo.core.user.service.vector.UserVectorService;
import nodingo.core.user.utils.GamePolicy;
import org.springframework.context.ApplicationEventPublisher; // 퍼블리셔 import
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
    private final UserBadgeRepository userBadgeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void addScrap(Long userId, Long keywordId) {
        log.info(">>>> [Scrap] addScrap. userId={}, keywordId={}", userId, keywordId);

        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("해당 키워드를 찾을 수 없습니다."));

        if (userScrapRepository.isKeywordScrapped(userId, keywordId)) {
            throw new DuplicateScrapException("이미 스크랩한 키워드입니다.");
        }

        User user = getUser(userId);

        Optional<RecommendKeyword> recommendKeywordOpt = recommendKeywordRepository.findRecommend(userId, keywordId);

        UserScrap userScrap = recommendKeywordOpt.map(recommendKeyword -> UserScrap.createRecommendKeywordScrap(user, recommendKeyword)).orElseGet(() -> UserScrap.createPureKeywordScrap(user, keyword));

        userScrapRepository.save(userScrap);

        if (!userBadgeRepository.existsByUserIdAndBadgeType(userId, BadgeType.FIRST_SCRAP)) {
            userBadgeRepository.save(UserBadge.create(user, BadgeType.FIRST_SCRAP));
        }

        user.addKeywordScrap();
        int scrapXp = gamePolicy.getScrapXp();

        user.addXp(scrapXp);

        eventPublisher.publishEvent(new UserXpChangedEvent(user));

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

        eventPublisher.publishEvent(new UserXpChangedEvent(user));

        log.info(">>>> [Scrap] Scrap removed. userId={}, keywordId={}, xp={}", userId, keywordId, scrapXp);

        userScrapRepository.delete(scrap);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}