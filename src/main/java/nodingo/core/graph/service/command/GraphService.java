package nodingo.core.graph.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.user.utils.GamePolicy;
import nodingo.core.global.exception.keyword.KeywordNotFoundException;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.domain.Keyword;
import nodingo.core.keyword.domain.UserKeywordExplore;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.keyword.repository.UserKeywordExploreRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;
    private final UserKeywordExploreRepository exploreRepository;
    private final GamePolicy gamePolicy; // 🔥 정책 주입

    @Transactional
    public void exploreNode(Long userId, Long keywordId) {
        if (isAlreadyExplored(userId, keywordId)) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new KeywordNotFoundException("해당 키워드를 찾을 수 없습니다."));

        exploreRepository.save(UserKeywordExplore.create(user, keyword));

        user.addNodeExplore();
        boolean isLevelUp = user.addXp(gamePolicy.getExploreXp());

        if (isLevelUp) {
            log.info(">>>> [Level Up] User {} reached level {}", user.getId(), user.getLevel());
        }
    }

    private boolean isAlreadyExplored(Long userId, Long keywordId) {
        if (exploreRepository.existsByUserIdAndKeywordId(userId, keywordId)) {
            log.info(">>>> [Explore] User {} already explored Keyword {}", userId, keywordId);
            return true;
        }
        return false;
    }
}