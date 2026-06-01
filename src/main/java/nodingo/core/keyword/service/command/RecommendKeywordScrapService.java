package nodingo.core.keyword.service.command;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendKeywordScrapService {

    private final UserScrapRepository userScrapRepository;
    private final UserRepository userRepository;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final UserVectorService userVectorService;
    private final GamePolicy gamePolicy;

    public void addScrap(Long userId, Long keywordId) {
        RecommendKeyword rk = getRk(userId, keywordId, "해당 키워드 추천 정보를 찾을 수 없습니다.");

        extracted(userId, rk);

        User user = getUser(userId);

        userScrapRepository.save(UserScrap.createRecommendKeywordScrap(user, rk));

        user.addKeywordScrap();
        user.addXp(gamePolicy.getScrapXp());

        userVectorService.updateKeywordEmbeddingAsync(userId, keywordId);
    }

    public void removeScrap(Long userId, Long keywordId) {
        RecommendKeyword rk = getRk(userId, keywordId, "추천 정보를 찾을 수 없습니다.");

        UserScrap scrap = getScrap(userId, rk);

        User user = getUser(userId);

        user.removeKeywordScrap();
        user.removeXp(gamePolicy.getScrapXp());

        userScrapRepository.delete(scrap);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private void extracted(Long userId, RecommendKeyword rk) {
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