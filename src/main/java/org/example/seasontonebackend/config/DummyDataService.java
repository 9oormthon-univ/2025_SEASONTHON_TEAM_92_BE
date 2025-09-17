package org.example.seasontonebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.Role;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.example.seasontonebackend.smartdiagnosis.domain.entity.SmartMeasurement;
import org.example.seasontonebackend.smartdiagnosis.repository.SmartMeasurementRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataService {

    private final MemberRepository memberRepository;
    private final SmartMeasurementRepository smartMeasurementRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Transactional
    public void createDummyUsers(int count) {
        log.info("{}명의 더미 사용자를 생성합니다...", count);
        
        List<Member> members = new ArrayList<>();
        
        String[] names = {"김철수", "이영희", "박민수", "최지영", "정현우", "강서연", "윤도현", "임수진", 
                         "한지훈", "오나영", "송재현", "조미래", "배성호", "신예린", "권태준", "홍지은",
                         "서동현", "문소영", "양준호", "노하늘", "백승우", "심지원", "유태민", "남소희",
                         "도현수", "라미영", "마준서", "바다현", "사랑희", "아름수", "자유영", "차민호",
                         "카이영", "타라수", "파란희", "하늘민", "거울영", "나비수", "다람희", "라면민",
                         "마법영", "바람수", "사과희", "아이민", "자전거영", "차량수", "카페희", "타이어민",
                         "파일영", "하드수", "거울희", "나무민", "다이아영", "라디오수", "마우스희", "바이올린민",
                         "사진영", "아이폰수", "자동차희", "차고민", "카드영", "타블렛수", "파워희", "하이민",
                         "거리영", "나침반수", "다이어리희", "라벨민", "마스크영", "바나나수", "사과희", "아이스민",
                         "자석영", "차이수", "카메라희", "타이머민", "파일영", "하모니수", "거울희", "나비민",
                         "다이아영", "라디오수", "마우스희", "바이올린민", "사진영", "아이폰수", "자동차희", "차고민",
                         "카드영", "타블렛수", "파워희", "하이민", "거리영", "나침반수", "다이어리희", "라벨민"};
        
        String[] buildings = {"아파트", "오피스텔", "빌라", "원룸", "투룸", "쓰리룸"};
        String[] buildingTypes = {"신축", "준신축", "중고", "리모델링"};
        String[] contractTypes = {"전세", "월세", "반전세"};
        String[] dongs = {"101동", "102동", "103동", "201동", "202동", "203동", "301동", "302동", "303동"};
        
        for (int i = 0; i < count; i++) {
            String name = names[i % names.length] + (i / names.length > 0 ? (i / names.length) : "");
            String email = "user" + (i + 1) + "@example.com";
            
            Member member = Member.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(Role.User)
                .building(buildings[random.nextInt(buildings.length)])
                .detailAddress("상세주소 " + (i + 1))
                .buildingType(buildingTypes[random.nextInt(buildingTypes.length)])
                .contractType(contractTypes[random.nextInt(contractTypes.length)])
                .security((long) (random.nextInt(10000) + 1000) * 10000) // 1000만원 ~ 1억원
                .rent(random.nextInt(50) + 20) * 10000) // 20만원 ~ 70만원
                .maintenanceFee(random.nextInt(20) + 5) * 10000) // 5만원 ~ 25만원
                .gpsVerified(random.nextBoolean())
                .contractVerified(random.nextBoolean())
                .dong(dongs[random.nextInt(dongs.length)])
                .onboardingCompleted(random.nextBoolean())
                .diagnosisCompleted(random.nextBoolean())
                .providerId("provider_" + (i + 1))
                .socialType(random.nextBoolean() ? SocialType.GOOGLE : SocialType.KAKAO)
                .build();
                
            members.add(member);
        }
        
        List<Member> savedMembers = memberRepository.saveAll(members);
        log.info("{}명의 더미 사용자가 생성되었습니다.", savedMembers.size());
        
        // 각 사용자에 대한 스마트 측정 데이터도 생성
        createSmartMeasurementsForUsers(savedMembers);
    }

    private void createSmartMeasurementsForUsers(List<Member> members) {
        List<SmartMeasurement> measurements = new ArrayList<>();
        
        String[] locations = {"거실", "침실", "화장실", "주방", "베란다"};
        String[] devices = {"스마트폰", "태블릿", "노트북", "데스크톱"};
        
        for (Member member : members) {
            // 각 사용자당 3-10개의 측정 데이터 생성
            int measurementCount = random.nextInt(8) + 3;
            
            for (int i = 0; i < measurementCount; i++) {
                SmartMeasurement.MeasurementType type = SmartMeasurement.MeasurementType.values()[
                    random.nextInt(SmartMeasurement.MeasurementType.values().length)
                ];
                
                BigDecimal value;
                String unit;
                
                switch (type) {
                    case NOISE:
                        value = BigDecimal.valueOf(random.nextDouble() * 50 + 30); // 30-80 dB
                        unit = "dB";
                        break;
                    case LEVEL:
                        value = BigDecimal.valueOf(random.nextDouble() * 100 + 1); // 1-101 Hz
                        unit = "Hz";
                        break;
                    case INTERNET:
                        value = BigDecimal.valueOf(random.nextDouble() * 90 + 10); // 10-100 Mbps
                        unit = "Mbps";
                        break;
                    default:
                        value = BigDecimal.valueOf(random.nextDouble() * 100);
                        unit = "unit";
                }
                
                SmartMeasurement measurement = SmartMeasurement.builder()
                    .member(member)
                    .measurementType(type)
                    .measuredValue(value)
                    .unit(unit)
                    .locationInfo(locations[random.nextInt(locations.length)])
                    .deviceInfo(devices[random.nextInt(devices.length)])
                    .measurementDuration(random.nextInt(300) + 60) // 1-5분
                    .createdAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                    .build();
                    
                measurements.add(measurement);
            }
        }
        
        smartMeasurementRepository.saveAll(measurements);
        log.info("{}개의 스마트 측정 데이터가 생성되었습니다.", measurements.size());
    }

    @Transactional
    public void clearAllDummyData() {
        log.info("모든 더미 데이터를 삭제합니다...");
        smartMeasurementRepository.deleteAll();
        memberRepository.deleteAll();
        log.info("더미 데이터 삭제가 완료되었습니다.");
    }
}