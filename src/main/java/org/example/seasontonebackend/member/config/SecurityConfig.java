package org.example.seasontonebackend.member.config;


import org.example.seasontonebackend.member.auth.JwtTokenFilter;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final MemberRepository memberRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    // JwtTokenFilter 대신, 필터가 필요로 하는 의존성을 직접 주입받음
    public SecurityConfig(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain myfilter (HttpSecurity httpSecurity) throws Exception {
        // 필터를 직접 생성하여 사용
        JwtTokenFilter jwtTokenFilter = new JwtTokenFilter(memberRepository, secretKey);

        return httpSecurity
                .cors(cors -> cors.configurationSource(configurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic((AbstractHttpConfigurer::disable))
                .sessionManagement(s->s.sessionCreationPolicy((SessionCreationPolicy.STATELESS)))
                .authorizeHttpRequests(a->a.requestMatchers("/ping", "/member/create", "/member/doLogin", "/member/google/doLogin", "/member/kakao/doLogin", "/h2-console/**", "/api/location/preview", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public CorsConfigurationSource configurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 도메인들 명시적 추가
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001", 
            "https://rental-lovat-theta.vercel.app",
            "https://*.vercel.app"
        ));
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}