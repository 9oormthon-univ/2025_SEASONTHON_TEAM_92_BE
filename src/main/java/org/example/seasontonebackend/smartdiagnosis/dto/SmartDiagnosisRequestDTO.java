package org.example.seasontonebackend.smartdiagnosis.dto;

import lombok.*;

import java.util.List;

public class SmartDiagnosisRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelStart {
        private String location;
        private String measurementType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelMeasure {
        private String sessionId;
        private AccelerometerData accelerometer;
        private GyroscopeData gyroscope;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccelerometerData {
        private Double x;
        private Double y;
        private Double z;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GyroscopeData {
        private Double alpha;
        private Double beta;
        private Double gamma;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseStart {
        private String location;
        private Integer duration; // 측정 시간 (초)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseRealtime {
        private String sessionId;
        private Double decibel;
        private String timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseComplete {
        private String sessionId;
        private List<NoiseSample> samples;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoiseSample {
        private String timestamp;
        private Double decibel;
    }

    // 인터넷 속도 측정 관련 추가
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternetStart {
        private String connectionType; // WiFi, Cellular
        private String location;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternetComplete {
        private String sessionId;
        private Double downloadSpeed; // Mbps
        private Double uploadSpeed;   // Mbps
        private Double ping;          // ms
        private String serverLocation;
    }


}