package org.example.seasontonebackend.member.controller;



import org.example.seasontonebackend.member.auth.JwtTokenProvider;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.Role;
import org.example.seasontonebackend.member.dto.MemberCreateDto;
import org.example.seasontonebackend.member.dto.MemberDongBuildingRequestDto;
import org.example.seasontonebackend.member.dto.MemberLoginDto;
import org.example.seasontonebackend.member.dto.MemberProfileDto;
import org.example.seasontonebackend.member.service.MemberService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;


    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/create")
    public ResponseEntity<?> memberCreate(@RequestBody MemberCreateDto memberCreateDto) {
        Member member = memberService.create(memberCreateDto);
        
        // 프론트엔드가 기대하는 형태로 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "회원가입이 완료되었습니다.");
        response.put("user", Map.of(
            "id", member.getId().toString(),
            "email", member.getEmail(),
            "nickname", member.getName(), // 프론트엔드는 nickname 필드를 기대함
            "role", member.getRole().toString().toLowerCase(), // 프론트엔드는 소문자 role을 기대함
            "profileCompleted", false,
            "diagnosisCompleted", false,
            "onboardingCompleted", false
        ));
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginDto memberLoginDto) {
        Member member = memberService.login(memberLoginDto);
        String jwtToken = jwtTokenProvider.createToken(member.getId(), member.getEmail(), member.getRole().toString());

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", member.getId());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }


    @GetMapping("/profile")
    public ResponseEntity<?> memberProfile(@AuthenticationPrincipal Member member) {
        MemberProfileDto memberProfileDto = memberService.getMemberProfile(member);
        return new ResponseEntity<>(memberProfileDto, HttpStatus.OK);
    }

    @PostMapping("/profile/setting")
    public ResponseEntity<?> addMemberDongBuilding(@RequestBody MemberDongBuildingRequestDto memberDongBuildingRequestDto, @AuthenticationPrincipal Member member) {
        System.out.println("=== Profile Setting Request ===");
        System.out.println("Member object: " + member);
        System.out.println("Request data: " + memberDongBuildingRequestDto);
        
        if (member == null) {
            System.out.println("ERROR: Member is null!");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "인증된 사용자 정보를 찾을 수 없습니다.");
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
        
        System.out.println("Member ID: " + member.getId());
        System.out.println("Member Email: " + member.getEmail());
        
        memberService.setMemberDongBuilding(memberDongBuildingRequestDto, member.getId());

        // 성공 응답에 success 필드 포함
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", "프로필이 성공적으로 업데이트되었습니다.");
        
        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }


}
