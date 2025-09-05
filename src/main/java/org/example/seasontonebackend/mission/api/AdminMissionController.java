package org.example.seasontonebackend.mission.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.mission.application.MissionService;
import org.example.seasontonebackend.mission.dto.MissionRequestDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
public class AdminMissionController {

    private final MissionService missionService;

    @PostMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> createMission(@RequestBody MissionRequestDTO request,
                                           @AuthenticationPrincipal Member admin) {
        try {
            var mission = missionService.createMission(request, admin.getId());
            return ResponseEntity.ok(Map.of("success", true, "data", mission));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> getAllMissions() {
        try {
            var missions = missionService.getAllMissions();
            return ResponseEntity.ok(Map.of("success", true, "data", missions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/{missionId}/status")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> updateMissionStatus(@PathVariable Long missionId,
                                                 @RequestParam Boolean isActive) {
        try {
            missionService.updateMissionStatus(missionId, isActive);
            return ResponseEntity.ok(Map.of("success", true, "message", "미션 상태가 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}