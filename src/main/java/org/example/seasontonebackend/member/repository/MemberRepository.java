package org.example.seasontonebackend.member.repository;


import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByProviderId(String providerId);

    Optional<Member> findByIdAndSocialType(Long id, SocialType socialType);
}