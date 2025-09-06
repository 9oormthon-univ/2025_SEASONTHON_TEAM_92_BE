package org.example.seasontonebackend.mission.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.mission.application.MissionService;
import org.example.seasontonebackend.mission.dto.MissionRequestDTO;
import org.example.seasontonebackend.mission.dto.MissionResponseDTO;
import org.example.seasontonebackend.member.domain.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mission/weekly")
@RequiredArgsConstructor
@Slf4j
public class WeeklyMissionController {

    private final MissionService missionService;

    // 현재 활성 미션 조회
    @GetMapping("/current")
    public ResponseEntity<MissionResponseDTO.ApiResponse<MissionResponseDTO.CurrentMission>> getCurrentMission(
            @AuthenticationPrincipal Member member) {

        try {
            MissionResponseDTO.CurrentMission mission = missionService.getCurrentMission(member.getId());
            return ResponseEntity.ok(MissionResponseDTO.ApiResponse.success(mission));

        } catch (Exception e) {
            log.error("현재 미션 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(MissionResponseDTO.ApiResponse.error(e.getMessage()));
        }
    }

    // 미션 참여하기
    @PostMapping("/{missionId}/participate")
    public ResponseEntity<MissionResponseDTO.ApiResponse<Map<String, Object>>> participateInMission(
            @PathVariable Long missionId,
            @RequestBody MissionRequestDTO.MissionParticipate request,
            @AuthenticationPrincipal Member member) {

        try {
            Long responseId = missionService.participateInMission(member.getId(), missionId, request);

            // 총점 계산
            Integer totalScore = request.getResponses().stream()
                    .mapToInt(MissionRequestDTO.MissionParticipate.Response::getScore)
                    .sum();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("response_id", responseId);
            responseData.put("total_score", totalScore);
            responseData.put("message", "미션 참여가 완료되었습니다!");
            responseData.put("next_step", "결과 확인하기");

            return ResponseEntity.ok(MissionResponseDTO.ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("미션 참여 실패", e);
            return ResponseEntity.badRequest()
                    .body(MissionResponseDTO.ApiResponse.error(e.getMessage()));
        }
    }

    // 미션 결과 조회
    @GetMapping("/{missionId}/result")
    public ResponseEntity<MissionResponseDTO.ApiResponse<MissionResponseDTO.MissionResult>> getMissionResult(
            @PathVariable Long missionId,
            @AuthenticationPrincipal Member member) {

        try {
            MissionResponseDTO.MissionResult result = missionService.getMissionResult(member.getId(), missionId);
            return ResponseEntity.ok(MissionResponseDTO.ApiResponse.success(result));

        } catch (Exception e) {
            log.error("미션 결과 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(MissionResponseDTO.ApiResponse.error(e.getMessage()));
        }
    }
}