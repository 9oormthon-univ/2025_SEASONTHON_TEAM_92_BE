package org.example.seasontonebackend.member.controller;



import org.example.seasontonebackend.member.auth.JwtTokenProvider;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.Role;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.dto.*;
import org.example.seasontonebackend.member.service.MemberService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
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
        return new ResponseEntity<>(member.getId(), HttpStatus.CREATED);
    }


    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginDto memberLoginDto) {
        String jwtToken = memberService.login(memberLoginDto);

        Map<String, Object> loginInfo = new HashMap<>();
//        loginInfo.put("id", member.getId());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }


    @GetMapping("/profile")
    public ResponseEntity<?> memberProfile(@AuthenticationPrincipal Member member) {
        MemberProfileDto memberProfileDto = memberService.getMemberProfile(member.getId());
        return new ResponseEntity<>(memberProfileDto, HttpStatus.OK);
    }

    @PostMapping("/profile/setting")
    public ResponseEntity<?> addMemberDongBuilding(@RequestBody MemberDongBuildingRequestDto memberDongBuildingRequestDto, @AuthenticationPrincipal Member member) {
        memberService.setMemberDongBuilding(memberDongBuildingRequestDto, member.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping("/google/create")
    public ResponseEntity<?> googleMemberCreate(@RequestParam Long googleUser, @RequestBody MemberDongBuildingRequestDto memberDongBuildingRequestDto) {
//        String token = memberService.googleMemberCreate(googleUser, memberDongBuildingRequestDto);
        memberService.setMemberDongBuilding(memberDongBuildingRequestDto, googleUser);

        MemberLoginDto memberLoginDto = new MemberLoginDto();


        String jwtToken = memberService.login(memberLoginDto);

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }



//    @GetMapping("/google/token")
//    public ResponseEntity<?> googleLoginGetToken(@RequestParam Long googleUser) {
//        String token = memberService.googleGetToken(googleUser);
//
//        Map<String, Object> loginInfo = new HashMap<>();
//        loginInfo.put("token", token);
//        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
//    }


    @PatchMapping("/profile/modify")
    public ResponseEntity<?> modifyUserProfile(@RequestBody ModifyMemberProfileDto modifyMemberProfileDto, @AuthenticationPrincipal Member member) {
        memberService.modifyMemberProfile(member, modifyMemberProfileDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping("/login/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleTokenDto googleTokenDto) {
        AccessTokenDto accessTokenDto = memberService.getAccessToken(googleTokenDto.getGoogleToken());
        GoogleProfileDto googleProfileDto = memberService.getGoogleProfile(accessTokenDto.getAccess_token());

        Member originalMember = memberService.getMemberBySocialId(googleProfileDto.getSub());
        if(originalMember == null){
            originalMember = memberService.createOauth(googleProfileDto.getSub(), googleProfileDto.getEmail(), SocialType.GOOGLE);
        }
//        회원가입돼 있는 회원이라면 토큰발급
        String jwtToken = jwtTokenProvider.createToken(originalMember.getId() ,originalMember.getEmail(), originalMember.getRole().toString());

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", originalMember.getId());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }


//



}


