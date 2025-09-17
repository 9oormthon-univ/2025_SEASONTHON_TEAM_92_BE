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
public class GoogleService extends SimpleUrlAuthenticationSuccessHandler {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${oauth2.redirect.url}")
    private String frontendRedirectUrl;

    public GoogleService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();

            String providerId = oAuth2User.getAttribute("sub");
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            SocialType socialType = SocialType.GOOGLE;

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
                
                if (member == null) {
                    isNewUser = true;
                    
                    // 이메일이 null이면 가짜 이메일 생성
                    String finalEmail = email;
                    if (finalEmail == null || finalEmail.isEmpty()) {
                        finalEmail = "google_" + providerId + "@google.local";
                    }
                    
                    member = Member.builder()
                            .email(finalEmail) // null이 아닌 값 보장
                            .name(name != null ? name : "구글사용자")
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

            // 이메일이 null일 경우 providerId를 사용 (안전장치)
            String memberEmail = member.getEmail() != null ? member.getEmail() : member.getProviderId() + "@google.local";
            String jwtToken = jwtTokenProvider.createToken(member.getId(), memberEmail, member.getRole().toString());

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .queryParam("token", jwtToken)
                    .queryParam("isNewUser", isNewUser)
                    .build().toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            
        } catch (Exception e) {
            System.err.println("=== Google OAuth Error ===");
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