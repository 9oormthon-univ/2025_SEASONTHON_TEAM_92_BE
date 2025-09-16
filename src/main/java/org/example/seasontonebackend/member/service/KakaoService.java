package org.example.seasontonebackend.member.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.example.seasontonebackend.member.auth.JwtTokenProvider;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.Role;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Service
@Transactional
public class KakaoService extends SimpleUrlAuthenticationSuccessHandler {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${oauth2.redirect.url}")
    private String frontendRedirectUrl;

    public KakaoService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();

        String providerId = oAuth2User.getAttribute("id").toString();
        String email = null;
        String name = null;

        // 카카오 사용자 정보 가져오기
        Object kakaoAccount = oAuth2User.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            email = (String) ((java.util.Map) kakaoAccount).get("email");
            Object profile = ((java.util.Map) kakaoAccount).get("profile");
            if (profile != null) {
                name = (String) ((java.util.Map) profile).get("nickname");
            }
        }

        SocialType socialType = SocialType.KAKAO;

        Member member = memberRepository.findByProviderId(providerId).orElse(null);
        boolean isNewUser = false;

        if (member == null) {
            // 이메일이 있는 경우에만 중복 계정 확인
            if (email != null && !email.isEmpty()) {
                Optional<Member> existingMemberOpt = memberRepository.findByEmail(email);
                if (existingMemberOpt.isPresent()) {
                    member = existingMemberOpt.get();
                    member.setProviderId(providerId);
                    member.setSocialType(socialType);
                    memberRepository.save(member);
                }
            }
            
            // 기존 계정이 없으면 새로 생성
            if (member == null) {
                isNewUser = true;
                member = Member.builder()
                        .email(email) // null일 수 있음
                        .name(name != null ? name : "카카오사용자") // 닉네임이 없으면 기본값
                        .socialType(socialType)
                        .providerId(providerId)
                        .role(Role.User)
                        .onboardingCompleted(false)
                        .gpsVerified(false)
                        .contractVerified(false)
                        .build();
                memberRepository.save(member);
            }
        }

        // 이메일이 null일 경우 providerId를 사용
        String memberEmail = member.getEmail() != null ? member.getEmail() : member.getProviderId() + "@kakao.local";
        String jwtToken = jwtTokenProvider.createToken(member.getId(), memberEmail, member.getRole().toString());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", jwtToken)
                .queryParam("isNewUser", isNewUser)
                .build().toUriString();
        
        System.out.println("=== Kakao OAuth Redirect Debug ===");
        System.out.println("Frontend Redirect URL: " + frontendRedirectUrl);
        System.out.println("Final Redirect URL: " + redirectUrl);
        System.out.println("JWT Token: " + jwtToken);
        System.out.println("Is New User: " + isNewUser);
        System.out.println("================================");
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        
        } catch (Exception e) {
            System.err.println("=== Kakao OAuth Error ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================");
            
            // 에러 발생 시 프론트엔드로 에러 파라미터와 함께 리다이렉트
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .queryParam("error", "oauth_failed")
                    .queryParam("message", e.getMessage())
                    .build().toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, errorRedirectUrl);
        }
    }
}