package org.example.seasontonebackend.diagnosis.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.example.seasontonebackend.diagnosis.domain.entity.Diagnosis;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisRepository;
import org.example.seasontonebackend.diagnosis.dto.DiagnosisResponseDTO;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.member.repository.MemberRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DiagnosisConverter {

    private final DiagnosisRepository diagnosisRepository;

    public DiagnosisResponseDTO toResponseDTO(Diagnosis diagnosis, MemberRepository memberRepository) {
        Member member = memberRepository.findById(diagnosis.getMemberId()).orElse(null);

        DiagnosisResponseDTO.DiagnosisResponseDTOBuilder builder = DiagnosisResponseDTO.builder()
                .diagnosisId(diagnosis.getId())
                .totalScore(diagnosis.getTotalScore())
                .scores(diagnosis.getScores())
                .diagnosedAt(diagnosis.getDiagnosedAt());

        // 비교 데이터는 Member에 building, dong 필드가 있을 때만 계산
        // 후에 Member 엔티티에 빌딩과 동 받은거까지 구현해야 될 것 같습니다!
        if (member != null && hasLocationInfo(member)) {
            String building = member.getBuilding(); // Member 엔티티에 building 필드 필요
            String dong = member.getDong();         // Member 엔티티에 dong 필드 필요

            Double buildingAverage = diagnosisRepository.getAverageScoreByBuilding(building);
            Double dongAverage = diagnosisRepository.getAverageScoreByDong(dong);

            Integer buildingRank = diagnosisRepository.getBuildingRank(building, diagnosis.getTotalScore());
            Integer dongRank = diagnosisRepository.getDongRank(dong, diagnosis.getTotalScore());

            List<Diagnosis> buildingDiagnoses = diagnosisRepository.findByBuilding(building);
            List<Diagnosis> dongDiagnoses = diagnosisRepository.findByDong(dong);

            builder.buildingRank(buildingRank)
                    .dongRank(dongRank)
                    .buildingTotal(buildingDiagnoses.size())
                    .dongTotal(dongDiagnoses.size())
                    .buildingAverage(buildingAverage != null ? buildingAverage : 0.0)
                    .dongAverage(dongAverage != null ? dongAverage : 0.0);
        } else {
            // Member에 위치 정보가 없으면 기본값 설정
            builder.buildingRank(0)
                    .dongRank(0)
                    .buildingTotal(0)
                    .dongTotal(0)
                    .buildingAverage(0.0)
                    .dongAverage(0.0);
        }

        return builder.build();
    }

    // Member에 위치 정보가 있는지 확인
    private boolean hasLocationInfo(Member member) {
        try {
            // Member 엔티티에 building, dong 필드가 있고 null이 아닌지 확인
            return member.getBuilding() != null && member.getDong() != null;
        } catch (Exception e) {
            // getBuilding(), getDong() 메서드가 없으면 false 반환
            return false;
        }
    }
}