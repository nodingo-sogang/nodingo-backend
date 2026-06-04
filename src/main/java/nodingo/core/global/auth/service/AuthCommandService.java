package nodingo.core.global.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nodingo.core.global.auth.client.NaverAuthClient;
import nodingo.core.global.auth.dto.response.ReissueTokenResponse;
import nodingo.core.global.auth.jwt.JwtTokenProvider;
import nodingo.core.keyword.repository.RecommendKeywordRepository;
import nodingo.core.keyword.repository.UserKeywordExploreRepository;
import nodingo.core.news.repository.RecommendNewsRepository;
import nodingo.core.notification.repository.NotificationSettingRepository;
import nodingo.core.quiz.repository.UserQuizResultRepository;
import nodingo.core.user.domain.InterestLevel;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserBadgeRepository;
import nodingo.core.user.repository.UserInterestRepository;
import nodingo.core.user.repository.UserRepository;
import nodingo.core.user.repository.UserScrapRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthCommandService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NaverAuthClient naverAuthClient;
    private final UserQuizResultRepository userQuizResultRepository;
    private final UserKeywordExploreRepository userKeywordExploreRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserScrapRepository userScrapRepository;
    private final RecommendNewsRepository recommendNewsRepository;
    private final RecommendKeywordRepository recommendKeywordRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserInterestRepository userInterestRepository;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    /**
     * 토큰 재발급
     */
    public ReissueTokenResponse reissue(String accessToken, String refreshToken) {
        validateForReissue(accessToken, refreshToken);
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 Refresh Token입니다."));

        Authentication auth = jwtTokenProvider.getAuthenticationFromUser(user);
        String newAccessToken = jwtTokenProvider.createAccessToken(auth);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(auth);
        user.updateRefreshToken(newRefreshToken);

        return new ReissueTokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     */
    public void logout(User user, String accessToken) {
        user.updateRefreshToken(null);
        userRepository.save(user);
        registerBlacklist(accessToken);
    }

    /**
     * 회원 탈퇴
     */
    public void withdraw(User user, String accessToken) {
        revokeNaverToken(user);
        registerBlacklist(accessToken);
        deleteAllUserData(user);

        log.info(">>>> [Withdrawal Complete] Successfully deleted all data for userId: {}", user.getId());
    }

    /******************** Helper Methods ********************/

    private void validateForReissue(String accessToken, String refreshToken) {
        if (accessToken != null && isBlacklisted(accessToken)) {
            throw new RuntimeException("이미 로그아웃된 토큰입니다.");
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token이 만료되었거나 유효하지 않습니다.");
        }
    }

    private boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + accessToken));
    }

    private void registerBlacklist(String accessToken) {
        if (accessToken == null) return;

        long remainingMillis = jwtTokenProvider.getRemainingTime(accessToken);
        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken,
                    "logout",
                    remainingMillis,
                    TimeUnit.MILLISECONDS
            );
            log.info(">>>> [Blacklist] Access token registered (remaining: {}ms)", remainingMillis);
        }
    }

    private void revokeNaverToken(User user) {
        if (!"naver".equalsIgnoreCase(user.getProvider())) return;
        if (user.getNaverAccessToken() == null) return;

        try {
            naverAuthClient.revokeToken(
                    naverClientId,
                    naverClientSecret,
                    user.getNaverAccessToken(),
                    "delete",
                    "NAVER"
            );
            log.info(">>>> [Naver Revoke] Successfully unlinked naver account for userId: {}", user.getId());
        } catch (Exception e) {
            log.error(">>>> [Naver Revoke Failed] userId: {}, reason: {}", user.getId(), e.getMessage());
        }
    }

    private void deleteAllUserData(User user) {
        Long userId = user.getId();

        user.getPersonas().clear();
        userRepository.saveAndFlush(user);

        userQuizResultRepository.deleteByUserId(userId);
        userKeywordExploreRepository.deleteByUserId(userId);
        userBadgeRepository.deleteByUserId(userId);
        userScrapRepository.deleteByUserId(userId);
        recommendNewsRepository.deleteByUserId(userId);
        recommendKeywordRepository.deleteByUserId(userId);
        notificationSettingRepository.deleteByUserId(userId);

        userInterestRepository.deleteByUserIdAndLevel(userId, InterestLevel.SPECIFIC);
        userInterestRepository.deleteByUserIdAndLevel(userId, InterestLevel.MACRO);

        userRepository.delete(user);
    }
}