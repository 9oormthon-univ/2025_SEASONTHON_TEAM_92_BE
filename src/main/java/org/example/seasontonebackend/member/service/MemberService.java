package org.example.seasontonebackend.member.service;


import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.dto.MemberCreateDto;
import org.example.seasontonebackend.member.dto.MemberDongBuildingRequestDto;
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
                .dong(memberCreateDto.getDong())
                .building(memberCreateDto.getBuilding())
                .buildingType(memberCreateDto.getBuildingType())
                .contractType(memberCreateDto.getContractType())
                .security(memberCreateDto.getSecurity())
                .rent(memberCreateDto.getRent())
                .maintenanceFee(memberCreateDto.getMaintenanceFee())
                .isGpsVerified(memberCreateDto.isGpsVerified())
                .isContractVerified(false) // 계약서 인증은 별도 로직이므로 false로 초기화
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

        return MemberProfileDto.builder()
                .profileName(member.getName())
                .profileEmail(member.getEmail())
                .profileBuilding(member.getBuilding())
                .profileDong(member.getDong())
                .build();
    }

    public void setMemberDongBuilding(MemberDongBuildingRequestDto memberDongBuildingRequestDto, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NullPointerException("존재하지 않는 유저입니다"));


        member.setBuilding(memberDongBuildingRequestDto.getBuilding());
        member.setDong(memberDongBuildingRequestDto.getDong());
        member.setDetailAddress(memberDongBuildingRequestDto.getDetailAddress());
        member.setBuildingType(memberDongBuildingRequestDto.getBuildingType());
        member.setContractType(memberDongBuildingRequestDto.getContractType());
        member.setSecurity(memberDongBuildingRequestDto.getSecurity());

        System.out.println(memberId);
        System.out.println(member.getDong());
        System.out.println(member.getDetailAddress());
        System.out.println(member.getBuilding());
        System.out.println(member.getContractType());
        System.out.println(member.getSecurity());

        memberRepository.save(member);

    }
}
