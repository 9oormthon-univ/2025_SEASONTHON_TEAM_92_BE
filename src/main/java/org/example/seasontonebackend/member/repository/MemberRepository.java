package org.example.seasontonebackend.member.repository;


import org.example.seasontonebackend.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List; // 추가된 import
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    List<Member> findByDong(String dong);
}