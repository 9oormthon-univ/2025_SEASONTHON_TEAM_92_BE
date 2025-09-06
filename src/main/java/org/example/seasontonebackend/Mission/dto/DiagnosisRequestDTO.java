package org.example.seasontonebackend.Mission.dto;

import lombok.*;

import java.util.List;

public class DiagnosisRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionParticipate {

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {
            private Long questionId;
            private String answer;
            private Integer score;
        }

        private List<Response> responses;
    }
}