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

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Assuming GenericFilter is a custom base class or interface
// import com.example.yourproject.filter.GenericFilter; 
// Assuming MemberRepository and Member are defined elsewhere
// import com.example.yourproject.repository.MemberRepository;
// import com.example.yourproject.domain.Member;

@Slf4j
public class JwtTokenFilter extends GenericFilter {
    private final String secretKey;
    private final MemberRepository memberRepository;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/public",
            "/member/create",
            "/member/doLogin",
            "/oauth2",
            "/login/oauth2",
            "/h2-console",
            "/swagger-ui",
            "/v3/api-docs",
            "/error",
            "/health",
            "/actuator",
            "/ping",
            "/favicon.ico"
    );

    // 생성자를 통해 secretKey와 memberRepository를 주입받음
    public JwtTokenFilter(MemberRepository memberRepository, String secretKey) {
        this.memberRepository = memberRepository;
        this.secretKey = secretKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String uri = httpServletRequest.getRequestURI();

        log.info(">>> JwtTokenFilter: Received request for URI: {}", uri);

        // 공개 경로는 필터를 통과
        if (PUBLIC_PATHS.stream().anyMatch(uri::startsWith)) {
            log.info("Public path detected, skipping JWT filter for URI: {}", uri);
            chain.doFilter(request, response);
            return;
        }

        String token = httpServletRequest.getHeader("Authorization");
        try {
            if (token != null ) {
                if (!token.startsWith("Bearer ")) {
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
                log.info("JWT Token parsed - Email: {}", email);
                
                Member member = memberRepository.findByEmail(email).orElse(null);
                if (member != null) {
                    log.info("Member found: {} (ID: {})", member.getEmail(), member.getId());
                    Authentication memberAuth = new UsernamePasswordAuthenticationToken(member, jwtToken, member.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(memberAuth);
                    log.info("Authentication set successfully for member: {}", member.getEmail());
                } else {
                    SecurityContextHolder.clearContext();
                    log.warn("Member not found for email: {}. This usually means the user was deleted from the database but the JWT token is still valid.", email);
                    httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
                    httpServletResponse.setContentType("application/json;charset=UTF-8");
                    httpServletResponse.getWriter().write("{\"success\":false,\"message\":\"사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.\"}");
                    return; // 필터 체인 중단
                }
            } else {
                 log.info("No JWT token found in Authorization header for URI: {}", uri);
            }
            chain.doFilter(request, response);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT Token for URI: {}. Error: {}", uri, e.getMessage());
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            httpServletResponse.getWriter().write("{\"success\":false,\"message\":\"유효하지 않은 토큰입니다.\"}");
            // 여기서 return을 추가하여 필터 체인 중단
            return; 
        }
    }
}