package org.example.seasontonebackend.mission.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.mission.application.MissionService;
import org.example.seasontonebackend.mission.dto.ParticipateRequestDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @GetMapping("/active")
    public ResponseEntity<?> getActiveMissions(@AuthenticationPrincipal Member member) {
        try {
            var missions = missionService.getActiveMissions(member.getId());
            return ResponseEntity.ok(Map.of("success", true, "data", missions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{missionId}/participate")
    public ResponseEntity<?> participate(@PathVariable Long missionId,
                                         @RequestBody ParticipateRequestDTO request,
                                         @AuthenticationPrincipal Member member) {
        try {
            missionService.participate(missionId, member.getId(), request);
            return ResponseEntity.ok(Map.of("success", true, "message", "미션 참여가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{missionId}/results")
    public ResponseEntity<?> getResults(@PathVariable Long missionId) {
        try {
            var results = missionService.getMissionResults(missionId);
            return ResponseEntity.ok(Map.of("success", true, "data", results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/my-participations")
    public ResponseEntity<?> getMyParticipations(@AuthenticationPrincipal Member member) {
        try {
            var participations = missionService.getMyParticipations(member.getId());
            return ResponseEntity.ok(Map.of("success", true, "data", participations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}