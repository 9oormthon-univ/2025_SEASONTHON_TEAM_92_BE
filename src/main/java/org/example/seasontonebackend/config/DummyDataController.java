package org.example.seasontonebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DummyDataController {

    private final DummyDataService dummyDataService;
    private final Environment environment;

    @PostMapping("/dummy-data")
    public ResponseEntity<Map<String, Object>> createDummyData(@RequestParam(defaultValue = "100") int count) {
        // Railway 환경에서만 허용
        String activeProfile = environment.getActiveProfiles().length > 0 
            ? environment.getActiveProfiles()[0] 
            : "local";
            
        if (!"railway".equals(activeProfile)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Railway 환경에서만 더미 데이터 생성이 가능합니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            dummyDataService.createDummyUsers(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", count + "명의 더미 사용자가 생성되었습니다.");
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("더미 데이터 생성 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "더미 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/dummy-data")
    public ResponseEntity<Map<String, Object>> clearDummyData() {
        // Railway 환경에서만 허용
        String activeProfile = environment.getActiveProfiles().length > 0 
            ? environment.getActiveProfiles()[0] 
            : "local";
            
        if (!"railway".equals(activeProfile)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Railway 환경에서만 더미 데이터 삭제가 가능합니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            dummyDataService.clearAllDummyData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 더미 데이터가 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("더미 데이터 삭제 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "더미 데이터 삭제 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}