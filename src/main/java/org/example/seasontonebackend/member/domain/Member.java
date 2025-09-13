package org.example.seasontonebackend.member.domain;



import jakarta.persistence.*;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Member implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    private String name; // 이름

    @Column(nullable = false, unique = true)
    private String email; // 이메일 유일해야함

    private String password; // 비밀번호

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.User; // 권한

    private String building;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "building_type")
    private String buildingType;

    @Column(name = "contract_type")
    private String contractType;

    private Long security; // 보증금

    private Integer rent; // 월세

    private Integer maintenanceFee; // 관리비

    @Column(name = "is_gps_verified")
    @Builder.Default
    private Boolean gpsVerified = false; // GPS 인증 여부

    @Column(name = "is_contract_verified")
    @Builder.Default
    private Boolean contractVerified = false; // 계약서 인증 여부

    private String dong;

    @Column(name = "onboarding_completed")
    @Builder.Default
    private Boolean onboardingCompleted = false; // 온보딩 완료 여부

    // ===== UserDetails 구현 =====
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email; // 로그인 ID로 email 사용
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 만료 여부 체크 로직 없으면 true
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 잠김 여부 체크 로직 없으면 true
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 패스워드 만료 여부 체크 로직 없으면 true
    }

    @Override
    public boolean isEnabled() {
        return true; // 활성화 여부 체크 로직 없으면 true
    }
}