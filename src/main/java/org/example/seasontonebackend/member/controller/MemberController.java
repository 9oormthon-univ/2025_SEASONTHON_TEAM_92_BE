package org.example.seasontonebackend.member.controller;



import org.example.seasontonebackend.member.auth.JwtTokenProvider;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.dto.MemberCreateDto;
import org.example.seasontonebackend.member.dto.MemberLoginDto;
import org.example.seasontonebackend.member.dto.MemberProfileDto;
import org.example.seasontonebackend.member.service.MemberService;
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
        return new ResponseEntity<>(member.getId(), HttpStatus.CREATED);
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



}
