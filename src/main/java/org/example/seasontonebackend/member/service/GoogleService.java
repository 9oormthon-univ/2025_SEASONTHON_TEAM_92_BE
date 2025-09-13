package org.example.seasontonebackend.member.service;

import jakarta.transaction.Transactional;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.seasontonebackend.member.auth.JwtTokenProvider;
import org.example.seasontonebackend.member.domain.Member;
import java.io.IOException;



@Service
@Transactional
public class GoogleService extends SimpleUrlAuthenticationSuccessHandler {
    private final MemberRepository memberRepository;

    public GoogleService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        System.out.println("sub = " + oAuth2User.getAttribute("sub"));
        System.out.println("email = " + oAuth2User.getAttribute("email"));


        Member member = memberRepository.findByProviderId(providerId).orElse(null);

        if (member == null) {
            member = Member.builder()
                    .email(email)
                    .socialType(SocialType.GOOGLE)
                    .providerId(providerId)
                    .build();

            memberRepository.save(member);
            Long memberId = member.getId();
//            response.sendRedirect("http://localhost:3000/member/login/google/create?memberId="+memberId);

        } else {
            Long memberId = member.getId();

//            response.sendRedirect("http://localhost:3000/member/login/google/present?memberId="+memberId);
        }





    }
}
