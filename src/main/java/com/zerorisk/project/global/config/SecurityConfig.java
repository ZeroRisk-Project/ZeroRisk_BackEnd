package com.zerorisk.project.global.config;

import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.security.JwtAuthenticationFilter;
import com.zerorisk.project.global.security.JwtTokenProvider;
import com.zerorisk.project.global.security.oauth.CustomOAuth2UserService;
import com.zerorisk.project.global.security.oauth.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // withCredentials: true로 쿠키를 주고받으려면 allowedOrigins를 * 대신 정확한 주소로 지정하고
    // allowCredentials(true)를 켜야 함. 둘 중 하나라도 안 맞으면 브라우저가 요청 자체를 막음.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"errorCode\":\"AUTH_005\",\"message\":\"로그인이 필요합니다.\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/competitions/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/competitions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/competitions/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/competitions/*/rankings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/competitions/*/archive").permitAll()
                        // 커뮤니티 [공개] 조회 API - 로그인 없이도 접근 가능해야 함
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/**").permitAll()
                        // 프로필 공개 설정 - 로그인 필수 (와일드카드 permitAll보다 먼저 배치)
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me/settings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me/settings").authenticated()
                        // 공개 프로필 조회 - 로그인 여부와 무관하게 접근 가능해야 함
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/*").permitAll()
                        // 공지사항 / 긴급 알림 - 비로그인 사용자도 봐야 함
                        .requestMatchers(HttpMethod.GET, "/api/v1/announcements").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/system-notices/active").permitAll()
                        // 전체 랭킹 조회 - 로그인 없이도 접근 가능해야 함 (와일드카드 permitAll보다 먼저 배치)
                        .requestMatchers(HttpMethod.GET, "/api/v1/rankings/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rankings").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, userRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}