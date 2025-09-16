package org.example.seasontonebackend.member.config;

import org.example.seasontonebackend.member.auth.JwtTokenFilter;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.example.seasontonebackend.member.service.GoogleService;
import org.example.seasontonebackend.member.service.KakaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final MemberRepository memberRepository;
    private final GoogleService googleService;
    private final KakaoService kakaoService;

    @Value("${jwt.secret}")
    private String secretKey;

    public SecurityConfig(MemberRepository memberRepository, GoogleService googleService, KakaoService kakaoService) {
        this.memberRepository = memberRepository;
        this.googleService = googleService;
        this.kakaoService = kakaoService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain myfilter(HttpSecurity httpSecurity) throws Exception {
        JwtTokenFilter jwtTokenFilter = new JwtTokenFilter(memberRepository, secretKey);

        return httpSecurity
                .cors(cors -> cors.configurationSource(configurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/", "/error", "/health", "/actuator/**", "/ping",
                        "/member/create", "/member/doLogin", "/oauth2/**", "/login/oauth2/**",
                        "/h2-console/**", "/api/location/preview", "/public/**",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                    .successHandler((request, response, authentication) -> {
                        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
                        
                        if ("google".equals(registrationId)) {
                            googleService.onAuthenticationSuccess(request, response, authentication);
                        } else if ("kakao".equals(registrationId)) {
                            kakaoService.onAuthenticationSuccess(request, response, authentication);
                        } else {
                            throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
                        }
                    })
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://rental-lovat-theta.vercel.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
