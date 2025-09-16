package org.example.seasontonebackend.member.service;



import org.example.seasontonebackend.member.auth.JwtTokenProvider;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.dto.*;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
@Transactional
public class MemberService {
    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;


    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

        return memberRepository.save(newMember);
    }

    public String login(MemberLoginDto memberLoginDto) {
        Optional<Member> optMember = memberRepository.findByEmail(memberLoginDto.getEmail());
        if (!optMember.isPresent()) {
            throw new IllegalArgumentException("no email found");
        }

        Member member = optMember.get();

        if (!passwordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("wrong password");
        }

        String token = jwtTokenProvider.createToken(member.getId(), member.getEmail(), String.valueOf(member.getRole()));

        return token;
    }





    public MemberProfileDto getMemberProfile(Long memberId) {

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NullPointerException("Member not found"));

            return MemberProfileDto.builder()
                    .profileName(member.getName())
                    .profileEmail(member.getEmail())
                    .profileBuilding(member.getBuilding())
                    .profileDong(member .getDong())
                    .build();

    }

    public void setMemberDongBuilding(MemberDongBuildingRequestDto memberDongBuildingRequestDto, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NullPointerException("존재하지 않는 유저입니다"));


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


    public String googleGetToken(Long googleUser) {
        Member member = memberRepository.findByIdAndSocialType(googleUser, SocialType.GOOGLE).orElseThrow(() -> new NullPointerException("존재하지 않는 유저입니다"));
        String token = jwtTokenProvider.createToken(member.getId(), member.getEmail(), String.valueOf(member.getRole()));

        return token;
        }

    public void modifyMemberProfile(Member member, ModifyMemberProfileDto modifyMemberProfileDto) {
        member.setName(modifyMemberProfileDto.getProfileName());
//        member.setDong(modifyMemberProfileDto.getDong());
        member.setBuilding(modifyMemberProfileDto.getProfileBuilding());
        member.setEmail(modifyMemberProfileDto.getProfileEmail());

        member.setDong(modifyMemberProfileDto.getDong());
        member.setDetailAddress(modifyMemberProfileDto.getDetailAddress());
        member.setBuilding(modifyMemberProfileDto.getProfileBuilding());
//        member.setBuildingType(modifyMemberProfileDto.getBuildingType());
//        member.setContractType(modifyMemberProfileDto.getContractType());
//        member.setSecurity(modifyMemberProfileDto.getSecurity());


        memberRepository.save(member);


    }



    public AccessTokenDto getAccessToken(String code){
//        인가코드, clientId, client_secret, redirect_uri, grant_type

//        Spring6부터 RestTemplate 비추천상태이기에, 대신 RestClient 사용
        RestClient restClient = RestClient.create();

//        MultiValueMap을 통해 자동으로 form-data형식으로 body 조립 가능
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<AccessTokenDto> response =  restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
//                ?code=xxxx&client_id=yyyy&
                .body(params)
//                retrieve:응답 body값만을 추출
                .retrieve()
                .toEntity(AccessTokenDto.class);

        System.out.println("응답 accesstoken JSON " + response.getBody());
        return response.getBody();
    }

    public GoogleProfileDto getGoogleProfile(String token){
        RestClient restClient = RestClient.create();
        ResponseEntity<GoogleProfileDto> response =  restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer "+token)
                .retrieve()
                .toEntity(GoogleProfileDto.class);
        System.out.println("profile JSON" + response.getBody());
        return response.getBody();
    }

    public Member getMemberBySocialId(String sub) {
        Member member = memberRepository.findByProviderId(sub).orElse(null);
        return member;
    }

    public Member createOauth(String sub, String email, SocialType socialType) {
        Member member = Member.builder()
                .providerId(sub)
                .email(email)
                .socialType(socialType)
                .build();

        memberRepository.save(member);
        return member;
    }
}

