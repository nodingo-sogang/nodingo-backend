package nodingo.core.global.auth.service;

import lombok.RequiredArgsConstructor;
import nodingo.core.global.auth.CustomOAuth2User;
import nodingo.core.global.oauth2.provider.NaverUserInfo;
import nodingo.core.global.oauth2.provider.OAuth2UserInfo;
import nodingo.core.user.domain.User;
import nodingo.core.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo;
        if ("naver".equals(registrationId)) {
            oAuth2UserInfo = new NaverUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String naverAccessToken = userRequest.getAccessToken().getTokenValue();

        User user = processOAuth2User(oAuth2UserInfo, naverAccessToken);
        return new CustomOAuth2User(user, oAuth2UserInfo.getAttributes());
    }

    private User processOAuth2User(OAuth2UserInfo userInfo, String naverAccessToken) {
        return userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .map(existingUser -> {
                    existingUser.updateInfo(userInfo.getName(), userInfo.getEmail());
                    existingUser.updateNickname(userInfo.getNickname());
                    existingUser.updateProfileImageUrl(userInfo.getProfileImageUrl());
                    existingUser.updateNaverAccessToken(naverAccessToken);
                    return existingUser;
                })
                .orElseGet(() -> {
                    User newUser = User.create(
                            userInfo.getProvider(),
                            userInfo.getProviderId(),
                            userInfo.getProvider() + "_" + userInfo.getProviderId(),
                            userInfo.getName(),
                            userInfo.getEmail(),
                            userInfo.getNickname(),
                            userInfo.getProfileImageUrl()
                    );
                    newUser.updateNaverAccessToken(naverAccessToken);
                    return userRepository.save(newUser);
                });
    }
}