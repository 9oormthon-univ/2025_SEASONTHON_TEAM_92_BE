#!/bin/bash

# =================================================================
# SeasonTone 로컬 테스트 자동화 스크립트 (GPS 인증 추가)
# =================================================================

echo "===== 최종 스크립트로 로컬 테스트를 시작합니다. ====="

# --- 1. 사용자 A 생성 ---
echo "\n>> [1/7] 사용자 A (userA@example.com) 생성..."
curl -s -X POST http://localhost:8080/member/create -H "Content-Type: application/json" -d '{"name": "사용자A", "email": "userA@example.com", "password": "password123", "dong": "망원동", "building": "A빌라", "buildingType": "빌라", "contractType": "월세", "security": 1000, "rent": 60, "maintenanceFee": 10, "isGpsVerified": false}' > /dev/null

# --- 2. 사용자 B 생성 ---
echo ">> [2/7] 사용자 B (userB@example.com) 생성..."
curl -s -X POST http://localhost:8080/member/create -H "Content-Type: application/json" -d '{"name": "사용자B", "email": "userB@example.com", "password": "password123", "dong": "망원동", "building": "A빌라", "buildingType": "빌라", "contractType": "월세", "security": 1200, "rent": 65, "maintenanceFee": 5, "isGpsVerified": false}' > /dev/null

# --- 3. 사용자 A 로그인 (JWT 토큰 획득) ---
echo ">> [3/7] 사용자 A로 로그인하여 인증 토큰(JWT)을 획득합니다..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/member/doLogin -H "Content-Type: application/json" -d '{"email": "userA@example.com", "password": "password123"}')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
    echo "!! 오류: 토큰 획득에 실패했습니다. 응답: $LOGIN_RESPONSE"
    exit 1
fi
echo ">> 토큰 획득 성공."

# --- 4. 사용자 A GPS 위치 인증 ---
echo ">> [4/7] 사용자 A GPS 위치 인증..."
curl -s -X POST http://localhost:8080/api/location/verify \
-H "Authorization: Bearer $ACCESS_TOKEN" \
-H "Content-Type: application/json" \
-d '{"latitude": 37.5665, "longitude": 126.9780, "buildingName": "서울시청"}' > /dev/null

# --- 5. 사용자 A 진단 데이터 제출 ---
echo ">> [5/7] 사용자 A의 진단 데이터를 제출합니다..."
curl -s -X POST http://localhost:8080/api/v1/diagnosis/responses -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -d '{"responses": [{"questionId": 1, "score": 2}, {"questionId": 2, "score": 3}, {"questionId": 3, "score": 5}, {"questionId": 4, "score": 4}, {"questionId": 5, "score": 1}, {"questionId": 6, "score": 2}, {"questionId": 7, "score": 4}, {"questionId": 8, "score": 5}, {"questionId": 9, "score": 3}, {"questionId": 10, "score": 2}]}' > /dev/null

# --- 6. 사용자 A 리포트 생성 ---
echo ">> [6/7] 사용자 A의 리포트를 생성합니다..."
REPORT_ID=$(curl -s -X POST http://localhost:8080/report/create -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -d '{"reportContent": "곰팡이가 심하고 방음이 잘 안돼요."}' )

if [ -z "$REPORT_ID" ]; then
    echo "!! 오류: 리포트 ID 획득에 실패했습니다."
    exit 1
fi
echo ">> 리포트 생성 성공. (Report ID: $REPORT_ID)"

# --- 7. 사용자 A 리포트 조회 (최종 테스트) ---
echo ">> [7/7] 최종 리포트를 조회합니다..."
echo "\n===== 최종 리포트 결과 (Report ID: $REPORT_ID) ====="
curl -s -X GET http://localhost:8080/report/$REPORT_ID -H "Authorization: Bearer $ACCESS_TOKEN" | jq .

echo "\n===== 테스트가 완료되었습니다. ====="
