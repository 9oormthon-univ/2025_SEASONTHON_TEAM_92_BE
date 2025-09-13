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

        Member member = memberRepository.findByProviderId(providerId).orElse(null);
        boolean isNewUser = false;

        if (member == null) {
            isNewUser = true;
            member = Member.builder()
                    .email(email)
                    .name(name)
                    .socialType(SocialType.GOOGLE)
                    .providerId(providerId)
                    .role(Role.User)
                    .onboardingCompleted(false)
                    .gpsVerified(false)
                    .contractVerified(false)
                    .build();
            memberRepository.save(member);
        }

        String jwtToken = jwtTokenProvider.createToken(member.getId(), member.getEmail(), member.getRole().toString());

        // TODO: 프론트엔드 리다이렉션 URL을 환경변수로 관리하는 것이 좋습니다.
        String redirectUrl = "https://rental-lovat-theta.vercel.app/auth/social-login?token=" + jwtToken + "&isNewUser=" + isNewUser;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}