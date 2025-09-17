package org.example.seasontonebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.domain.Role;
import org.example.seasontonebackend.member.domain.SocialType;
import org.example.seasontonebackend.member.repository.MemberRepository;
import org.example.seasontonebackend.smartdiagnosis.domain.entity.SmartMeasurement;
import org.example.seasontonebackend.smartdiagnosis.repository.SmartMeasurementRepository;
import org.example.seasontonebackend.mission.domain.entity.WeeklyMission;
import org.example.seasontonebackend.mission.domain.entity.MissionQuestion;
import org.example.seasontonebackend.mission.repository.WeeklyMissionRepository;
import org.example.seasontonebackend.diagnosis.domain.entity.DiagnosisResponse;
import org.example.seasontonebackend.diagnosis.domain.DiagnosisScore;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisResponseRepository;
import org.example.seasontonebackend.report.domain.Report;
import org.example.seasontonebackend.report.repository.ReportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final SmartMeasurementRepository smartMeasurementRepository;
    private final WeeklyMissionRepository weeklyMissionRepository;
    private final DiagnosisResponseRepository diagnosisResponseRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            // Railway 환경에서만 더미 데이터 생성
            String activeProfile = environment.getActiveProfiles().length > 0 
                ? environment.getActiveProfiles()[0] 
                : "local";
                
            if (!"railway".equals(activeProfile)) {
                log.info("더미 데이터 생성을 건너뜁니다. 활성 프로필: {}", activeProfile);
                return;
            }

            // 이미 데이터가 있는지 확인
            long existingCount = memberRepository.count();
            if (existingCount > 0) {
                log.info("이미 {}명의 사용자가 존재합니다. 더미 데이터 생성을 건너뜁니다.", existingCount);
                return;
            }

            log.info("더미 데이터 생성을 시작합니다...");
            
            // 더미 사용자 생성
            List<Member> members = createDummyMembers(100);
            
            // 더미 스마트 측정 데이터 생성
            createDummySmartMeasurements(members);
            
            // 더미 진단 응답 데이터 생성 (리포트 신뢰성 향상)
            createDummyDiagnosisResponses(members);
            
            // 더미 리포트 데이터 생성
            createDummyReports(members);
            
            // 더미 미션 데이터 생성
            createDummyMissions();
            
            log.info("더미 데이터 생성이 완료되었습니다. 총 {}명의 사용자가 생성되었습니다.", members.size());
            
        } catch (Exception e) {
            log.error("더미 데이터 생성 중 오류가 발생했습니다.", e);
            // 오류가 발생해도 애플리케이션 시작을 중단하지 않음
        }
    }

    private List<Member> createDummyMembers(int count) {
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
        
        return memberRepository.saveAll(members);
    }

    private void createDummySmartMeasurements(List<Member> members) {
        List<SmartMeasurement> measurements = new ArrayList<>();
        
        String[] units = {"dB", "Hz", "Mbps", "ms"};
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

    private void createDummyMissions() {
        List<WeeklyMission> missions = new ArrayList<>();
        
        // 주간 미션 데이터
        String[] categories = {"소음 측정", "환경 체크", "안전 점검", "편의 시설"};
        String[] titles = {
            "주간 소음 측정하기", "방음 효과 확인하기", "이웃과의 소통하기", 
            "환경 친화적 생활하기", "안전 시설 점검하기", "편의 시설 이용하기"
        };
        String[] descriptions = {
            "이번 주에는 주변 소음 수준을 측정해보세요.",
            "방음 효과가 잘 작동하는지 확인해보세요.",
            "이웃과의 소통을 통해 좋은 관계를 만들어보세요.",
            "환경을 생각하는 생활 습관을 실천해보세요.",
            "안전 시설들이 제대로 작동하는지 점검해보세요.",
            "편의 시설들을 적극적으로 활용해보세요."
        };
        
        LocalDate startDate = LocalDate.now().minusWeeks(4);
        
        for (int i = 0; i < 4; i++) {
            WeeklyMission mission = WeeklyMission.builder()
                .category(categories[i % categories.length])
                .title(titles[i % titles.length])
                .description(descriptions[i % descriptions.length])
                .startDate(startDate.plusWeeks(i))
                .endDate(startDate.plusWeeks(i).plusDays(6))
                .isActive(i >= 2) // 최근 2주만 활성화
                .build();
                
            missions.add(mission);
        }
        
        weeklyMissionRepository.saveAll(missions);
        log.info("{}개의 주간 미션이 생성되었습니다.", missions.size());
    }

    private void createDummyDiagnosisResponses(List<Member> members) {
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

    private void createDummyReports(List<Member> members) {
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
}