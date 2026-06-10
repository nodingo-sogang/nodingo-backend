package nodingo.core.user.repository.custom;

import nodingo.core.user.domain.UserScrap;

import java.util.Optional;
import java.util.List;

public interface UserScrapRepositoryCustom {
    boolean isKeywordScrapped(Long userId, Long recommendKeywordId);
    Optional<UserScrap> findKeywordScrap(Long userId, Long recommendKeywordId);
    List<UserScrap> findKeywordScrapsByUserId(Long userId, int page, int size);
    List<UserScrap> findAllByUserId(Long userId);
    Optional<UserScrap> findByUserIdAndKeywordId(Long userId, Long keywordId);
}
