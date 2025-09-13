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

import java.util.Map;
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
                // 나머지 필드들은 기본값으로 초기화 (나중에 프로필 설정에서 업데이트)
                .dong(null)
                .building(null)
                .buildingType(null)
                .contractType(null)
                .security(null)
                .rent(null)
                .maintenanceFee(null)
                .gpsVerified(false)
                .contractVerified(false)
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
                .name(member.getName())
                .email(member.getEmail())
                .dong(member.getDong())
                .building(member.getBuilding())
                .buildingType(member.getBuildingType())
                .contractType(member.getContractType())
                .security(member.getSecurity())
                .rent(member.getRent())
                .maintenanceFee(member.getMaintenanceFee())
                .gpsVerified(member.isGpsVerified())
                .contractVerified(member.isContractVerified())
                .onboardingCompleted(member.isOnboardingCompleted())
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
        member.setRent(memberDongBuildingRequestDto.getRent());
        member.setMaintenanceFee(memberDongBuildingRequestDto.getMaintenanceFee());

        System.out.println(memberId);
        System.out.println(member.getDong());
        System.out.println(member.getDetailAddress());
        System.out.println(member.getBuilding());
        System.out.println(member.getContractType());
        System.out.println(member.getSecurity());

        memberRepository.save(member);

    }

    public void updateUserInfo(Member member, Map<String, Object> updateData) {
        boolean updated = false;
        
        if (updateData.containsKey("onboardingCompleted")) {
            member.setOnboardingCompleted((Boolean) updateData.get("onboardingCompleted"));
            updated = true;
        }
        
        if (updateData.containsKey("rent")) {
            member.setRent(((Number) updateData.get("rent")).intValue());
            updated = true;
        }
        
        if (updateData.containsKey("maintenanceFee")) {
            member.setMaintenanceFee(((Number) updateData.get("maintenanceFee")).intValue());
            updated = true;
        }
        
        if (updated) {
            memberRepository.save(member);
        }
    }
}
