package org.example.seasontonebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.Role;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.example.seasontonebackend.smartdiagnosis.domain.entity.SmartMeasurement;
import org.example.seasontonebackend.smartdiagnosis.domain.repository.SmartMeasurementRepository;
import org.example.seasontonebackend.diagnosis.domain.entity.DiagnosisResponse;
import org.example.seasontonebackend.diagnosis.domain.DiagnosisScore;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisResponseRepository;
import org.example.seasontonebackend.report.domain.Report;
import org.example.seasontonebackend.report.repository.ReportRepository;
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
    private final DiagnosisResponseRepository diagnosisResponseRepository;
    private final ReportRepository reportRepository;
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
        
        // 빌라 전용 더미 데이터
        String[] villaNames = {"청담빌라", "강남빌라", "역삼빌라", "선릉빌라", "삼성빌라", "논현빌라", "신사빌라", "압구정빌라", 
                              "망원빌라", "홍대빌라", "상수빌라", "합정빌라", "성수빌라", "건대빌라", "왕십리빌라", "청량리빌라",
                              "이태원빌라", "한남빌라", "용산빌라", "서초빌라", "방배빌라", "사당빌라", "낙성대빌라", "서울대빌라"};
        String[] villaAddresses = {"서울시 강남구 청담동", "서울시 강남구 역삼동", "서울시 강남구 선릉동", "서울시 강남구 삼성동", 
                                  "서울시 강남구 논현동", "서울시 강남구 신사동", "서울시 강남구 압구정동", "서울시 마포구 망원동",
                                  "서울시 마포구 홍대입구", "서울시 마포구 상수동", "서울시 마포구 합정동", "서울시 성동구 성수동",
                                  "서울시 광진구 건대입구", "서울시 성동구 왕십리동", "서울시 동대문구 청량리동", "서울시 용산구 이태원동",
                                  "서울시 용산구 한남동", "서울시 용산구 용산동", "서울시 서초구 서초동", "서울시 서초구 방배동",
                                  "서울시 동작구 사당동", "서울시 동작구 낙성대동", "서울시 관악구 서울대입구"};
        
        for (int i = 0; i < count; i++) {
            String name = names[i % names.length] + (i / names.length > 0 ? (i / names.length) : "");
            String email = "user" + (i + 1) + "@example.com";
            
            // 빌라인 경우 특별한 데이터 생성
            String building = buildings[random.nextInt(buildings.length)];
            String buildingName = building.equals("빌라") ? villaNames[random.nextInt(villaNames.length)] : "건물" + (i + 1);
            String detailAddress = building.equals("빌라") ? villaAddresses[random.nextInt(villaAddresses.length)] : "상세주소 " + (i + 1);
            
            Member member = Member.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(Role.User)
                .building(building)
                .detailAddress(detailAddress)
                .buildingType(buildingTypes[random.nextInt(buildingTypes.length)])
                .contractType(contractTypes[random.nextInt(contractTypes.length)])
                .security((long) (random.nextInt(10000) + 1000) * 10000) // 1000만원 ~ 1억원
                .rent((random.nextInt(50) + 20) * 10000) // 20만원 ~ 70만원
                .maintenanceFee((random.nextInt(20) + 5) * 10000) // 5만원 ~ 25만원
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
        
        // 각 사용자에 대한 관련 데이터도 생성
        createSmartMeasurementsForUsers(savedMembers);
        createDiagnosisResponsesForUsers(savedMembers);
        createReportsForUsers(savedMembers);
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

    private void createDiagnosisResponsesForUsers(List<Member> members) {
        List<DiagnosisResponse> responses = new ArrayList<>();
        
        // 각 사용자당 진단 응답 생성 (10개 카테고리 × 2개 질문 = 20개 응답)
        for (Member member : members) {
            for (int category = 1; category <= 10; category++) {
                for (int question = 1; question <= 2; question++) {
                    long questionId = (category - 1) * 2 + question;
                    
                    // 다양한 점수 분포 생성 (1-5점)
                    DiagnosisScore score;
                    int scoreValue = random.nextInt(5) + 1;
                    
                    // 카테고리별로 점수 분포 조정 (현실적인 패턴)
                    switch (category) {
                        case 1: // 소음 - 보통 낮은 점수
                            scoreValue = random.nextInt(3) + 1; // 1-3점
                            break;
                        case 2: // 수압/온수 - 보통 점수
                            scoreValue = random.nextInt(3) + 2; // 2-4점
                            break;
                        case 3: // 채광 - 건물에 따라 다름
                            scoreValue = random.nextInt(4) + 1; // 1-4점
                            break;
                        case 7: // 보안/안전 - 보통 높은 점수
                            scoreValue = random.nextInt(2) + 3; // 3-4점
                            break;
                        case 9: // 편의시설 - 보통 점수
                            scoreValue = random.nextInt(3) + 2; // 2-4점
                            break;
                        default: // 나머지는 일반적인 분포
                            scoreValue = random.nextInt(5) + 1; // 1-5점
                    }
                    
                    score = DiagnosisScore.values()[scoreValue - 1];
                    
                    DiagnosisResponse response = DiagnosisResponse.builder()
                        .userId(member.getId())
                        .questionId(questionId)
                        .score(score)
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .build();
                        
                    responses.add(response);
                }
            }
        }
        
        diagnosisResponseRepository.saveAll(responses);
        log.info("{}개의 진단 응답 데이터가 생성되었습니다.", responses.size());
    }

    private void createReportsForUsers(List<Member> members) {
        List<Report> reports = new ArrayList<>();
        
        String[] reportTypes = {"free", "premium"};
        String[] userInputs = {
            "소음 문제가 심각합니다. 위층에서 계속 발소리가 들려요.",
            "수압이 너무 약해서 샤워할 때 불편합니다.",
            "채광이 부족해서 낮에도 불을 켜야 합니다.",
            "주차 공간이 부족해서 매번 고민입니다.",
            "난방비가 너무 많이 나와요.",
            "환기가 잘 안되어 습도가 높습니다.",
            "보안이 걱정됩니다. 출입문이 자주 열려있어요.",
            "관리사무소가 제대로 관리하지 않습니다.",
            "편의시설이 부족합니다.",
            "인터넷 속도가 너무 느려요."
        };
        
        // 각 사용자당 1-3개의 리포트 생성
        for (Member member : members) {
            int reportCount = random.nextInt(3) + 1; // 1-3개
            
            for (int i = 0; i < reportCount; i++) {
                String reportType = reportTypes[random.nextInt(reportTypes.length)];
                String userInput = userInputs[random.nextInt(userInputs.length)];
                
                Report report = Report.builder()
                    .member(member)
                    .userInput(userInput)
                    .reportType(reportType)
                    .isShareable(true)
                    .build();
                    
                reports.add(report);
            }
        }
        
        reportRepository.saveAll(reports);
        log.info("{}개의 리포트가 생성되었습니다.", reports.size());
    }

    @Transactional
    public void clearAllDummyData() {
        log.info("모든 더미 데이터를 삭제합니다...");
        reportRepository.deleteAll();
        diagnosisResponseRepository.deleteAll();
        smartMeasurementRepository.deleteAll();
        memberRepository.deleteAll();
        log.info("더미 데이터 삭제가 완료되었습니다.");
    }
}