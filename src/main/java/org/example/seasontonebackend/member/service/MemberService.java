package org.example.seasontonebackend.member.service;


import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.dto.MemberCreateDto;
import org.example.seasontonebackend.member.dto.MemberLoginDto;
import org.example.seasontonebackend.member.dto.MemberProfileDto;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Member create(MemberCreateDto memberCreateDto) {
        Optional<Member> member = memberRepository.findByEmail(memberCreateDto.getEmail());
        if (member.isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");

        }


        Member newMember = Member.builder()
                .email(memberCreateDto.getEmail())
                .name(memberCreateDto.getName())
                .password(passwordEncoder.encode(memberCreateDto.getPassword()))
                .build();
        memberRepository.save(newMember);
        return newMember;
    }

    public Member login(MemberLoginDto memberLoginDto) {
        Optional<Member> optMember = memberRepository.findByEmail(memberLoginDto.getEmail());
        if (!optMember.isPresent()) {
            throw new IllegalArgumentException("no email found");
        }

        Member member = optMember.get();

        if (!passwordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("wrong password");
        }

        return member;
    }





    public MemberProfileDto getMemberProfile(Member member) {
        MemberProfileDto memberProfileDto = MemberProfileDto.builder()
                .profileName(member.getName())
                .profileEmail(member.getEmail())
                .build();

        return memberProfileDto;
    }
}
