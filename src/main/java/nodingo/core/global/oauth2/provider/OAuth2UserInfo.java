package nodingo.core.global.oauth2.provider;

import java.util.Map;

public interface OAuth2UserInfo {
    String getProvider();
    String getProviderId();
    String getName();
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
    Map<String, Object> getAttributes();
}
