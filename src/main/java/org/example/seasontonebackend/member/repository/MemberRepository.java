package org.example.seasontonebackend.member.repository;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    List<Member> findByDong(String dong);
    Optional<Member> findByProviderId(String providerId);
    Optional<Member> findByIdAndSocialType(Long id, SocialType socialType);
}
