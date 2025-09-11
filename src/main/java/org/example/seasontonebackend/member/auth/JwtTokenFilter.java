package org.example.seasontonebackend.member.auth;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JwtTokenFilter extends GenericFilter {
    private final String secretKey;
    private final MemberRepository memberRepository;

    // 생성자를 통해 secretKey와 memberRepository를 주입받음
    public JwtTokenFilter(MemberRepository memberRepository, String secretKey) {
        this.memberRepository = memberRepository;
        this.secretKey = secretKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        log.info(">>> JwtTokenFilter: Received request for URI: {}", httpServletRequest.getRequestURI());

        String token = httpServletRequest.getHeader("Authorization");
        try {
            if (token != null ) {
                if (!token.substring(0, 7).equals("Bearer ")) {
                    throw new AuthenticationServiceException("not bearer type");
                }
                String jwtToken = token.substring(7);
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();

                List<GrantedAuthority> authoruties = new ArrayList<>();
                authoruties.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));
                UserDetails userDetails = new User(claims.getSubject(), "", authoruties);
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, jwtToken, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                String email = claims.getSubject();
                Member member = memberRepository.findByEmail(email).orElse(null);
                if (member != null) {
                    Authentication memberAuth = new UsernamePasswordAuthenticationToken(member, jwtToken, member.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(memberAuth);
                }
            }
            chain.doFilter(request, response);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            e.printStackTrace();
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType("application/json;char");
            httpServletResponse.getWriter().write("Invalid token");
        }
    }
}