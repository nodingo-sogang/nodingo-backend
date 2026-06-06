package nodingo.core.user.service.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.exception.user.UserNotFoundException;
import nodingo.core.keyword.repository.KeywordRepository;
import nodingo.core.user.domain.User;
import nodingo.core.user.dto.result.UserProgressResult;
import nodingo.core.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProgressQueryService {
    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;

    public UserProgressResult getMyProgress(Long userId) {
        log.info(">>>> [Progress Query] getMyProgress. userId={}", userId);
        User user = getUserOrElseThrow(userId);

        long totalNodes = keywordRepository.count();
        int exploredCount = user.getTotalNodesExplored();

        log.info(">>>> [Progress Query] getMyProgress result. userId={}, explored={}, total={}",
                userId, exploredCount, totalNodes);
        return UserProgressResult.from(exploredCount, totalNodes);
    }

    private User getUserOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}