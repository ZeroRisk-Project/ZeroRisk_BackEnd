package com.zerorisk.project.global.security.oauth;

import com.zerorisk.project.domain.user.entity.OAuthProvider;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuthUserInfo userInfo = extractUserInfo(registrationId, oidcUser.getClaims());
        User user = findOrCreateUser(userInfo);

        return new DefaultOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "sub");
    }

    private OAuthUserInfo extractUserInfo(String registrationId, Map<String, Object> claims) {
        if ("google".equals(registrationId)) {
            return new OAuthUserInfo(
                    (String) claims.get("email"),
                    (String) claims.get("name"),
                    OAuthProvider.GOOGLE,
                    (String) claims.get("sub"));
        }
        if ("kakao".equals(registrationId)) {
            return new OAuthUserInfo(
                    (String) claims.get("email"),
                    (String) claims.get("nickname"),
                    OAuthProvider.KAKAO,
                    (String) claims.get("sub"));
        }
        throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + registrationId);
    }

    private User findOrCreateUser(OAuthUserInfo userInfo) {
        return userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> userRepository.save(
                        User.createOAuthUser(userInfo.email(), resolveNickname(userInfo), userInfo.provider(),
                                userInfo.providerId())));
    }

    private record OAuthUserInfo(String email, String nickname, OAuthProvider provider, String providerId) {
    }

    private String resolveNickname(OAuthUserInfo userInfo) {
        String base = userInfo.nickname() != null ? userInfo.nickname() : "user";
        String candidate = truncate(base, 12);

        if (!userRepository.existsByNickname(candidate)) {
            return candidate;
        }

        String suffix = String.valueOf(System.currentTimeMillis() % 10000);
        String truncatedBase = truncate(base, 12 - suffix.length());
        return truncatedBase + suffix;
    }

    private String truncate(String value, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}