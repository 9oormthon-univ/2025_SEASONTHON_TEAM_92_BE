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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@Transactional
public class GoogleService extends SimpleUrlAuthenticationSuccessHandler {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public GoogleService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        SocialType socialType = SocialType.GOOGLE;

        // 1. 소셜 ID로 사용자 조회
        Member member = memberRepository.findByProviderId(providerId).orElse(null);
        boolean isNewUser = false;

        if (member == null) {
            // 2. 소셜 ID로 가입한 사용자가 없으면, 이메일로 조회
            Optional<Member> existingMemberOpt = memberRepository.findByEmail(email);

            if (existingMemberOpt.isPresent()) {
                // 2a. 이메일이 존재하면, 기존 계정에 소셜 정보 연동
                member = existingMemberOpt.get();
                member.setProviderId(providerId);
                member.setSocialType(socialType);
                memberRepository.save(member);
            } else {
                // 2b. 이메일도 존재하지 않으면, 신규 소셜 회원으로 가입
                isNewUser = true;
                member = Member.builder()
                        .email(email)
                        .name(name)
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

        // 3. JWT 토큰 생성 및 프론트엔드로 리다이렉트
        String jwtToken = jwtTokenProvider.createToken(member.getId(), member.getEmail(), member.getRole().toString());
        String redirectUrl = "https://rental-lovat-theta.vercel.app/auth/social-login?token=" + jwtToken + "&isNewUser=" + isNewUser;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
