# 주간 미션 시스템

## 담당 기능
월세 공동협약 네트워크 Phase 2 - 주간 이슈 체크 시스템

## 구현 내용

### 핵심 기능
- 관리자가 매주 1-3개 질문으로 미션 생성 (방음, 수압, 주차 등)
- 사용자 미션 참여 및 중복 방지
- 참여 즉시 집계 결과 확인
- 수집 데이터를 재계약 협상 자료로 활용

### 기술 스택
- Spring Boot + JPA + MySQL
- 기존 Member/JWT 인증 시스템 연동
- JSON 타입 활용한 유연한 질문/답변 구조

### 데이터베이스
```sql
-- 미션 정보 (질문은 JSON으로 저장)
CREATE TABLE missions (
    mission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    category ENUM('NOISE', 'WATER_PRESSURE', 'PARKING', 'HEATING', 'OTHER'),
    questions JSON NOT NULL,
    expires_at DATETIME NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT NOW()
);

-- 참여 기록 (답변도 JSON으로 저장)
CREATE TABLE mission_participations (
    participation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mission_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    answers JSON NOT NULL,
    participated_at DATETIME DEFAULT NOW(),
    UNIQUE KEY uk_member_mission (member_id, mission_id)  -- 중복 참여 방지
);
```

### API 엔드포인트
```
관리자:
POST   /api/admin/missions           미션 생성
GET    /api/admin/missions           미션 목록
PATCH  /api/admin/missions/{id}/status  상태 변경

사용자:
GET    /api/missions/active          활성 미션 조회
POST   /api/missions/{id}/participate   미션 참여
GET    /api/missions/{id}/results    결과 조회
GET    /api/missions/my-participations  내 참여 내역
```

### 파일 구조
```
mission/
├── api/           AdminMissionController, MissionController
├── application/   MissionService, MissionServiceImpl
├── converter/     MissionConverter
├── domain/
│   ├── entity/    Mission, MissionParticipation, MissionCategory
│   └── repository/ MissionRepository, MissionParticipationRepository
├── dto/           4개 DTO (Request/Response)
└── exception/     MissionException
```

### JSON 데이터 형식

#### Mission.questions 예시
```json
{
  "questions": [
    {
      "id": 1,
      "text": "옆집 생활 소음이 들리는 편인가요?",
      "type": "MULTIPLE_CHOICE",
      "options": [
        {"id": 1, "text": "전혀 안 들림", "score": 1},
        {"id": 2, "text": "가끔 들림", "score": 2},
        {"id": 3, "text": "자주 들림", "score": 3}
      ]
    }
  ]
}
```

#### MissionParticipation.answers 예시
```json
{
  "answers": [
    {"questionId": 1, "optionId": 2, "score": 2}
  ]
}
```

## 데모 예시

**미션**: "이번 주 방음 상태 점검"
- Q1: 옆집 소음이 들리나요? (전혀/가끔/자주)
- Q2: 최근 1달 층간소음 경험? (없음/1-2번/3번 이상)

**결과**: "참여자 25명 중 80%가 소음 문제 경험"