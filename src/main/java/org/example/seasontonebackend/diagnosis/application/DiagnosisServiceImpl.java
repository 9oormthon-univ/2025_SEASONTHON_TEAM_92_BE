package org.example.seasontonebackend.diagnosis.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.seasontonebackend.diagnosis.converter.DiagnosisConverter;
import org.example.seasontonebackend.diagnosis.domain.entity.Diagnosis;
import org.example.seasontonebackend.diagnosis.domain.repository.DiagnosisRepository;
import org.example.seasontonebackend.diagnosis.dto.*;
import org.example.seasontonebackend.diagnosis.exception.DiagnosisException;
import org.example.seasontonebackend.member.repository.MemberRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final MemberRepository memberRepository;
    private final DiagnosisConverter diagnosisConverter;

    @Override
    public DiagnosisResponseDTO createOrUpdateDiagnosis(Long memberId, DiagnosisRequestDTO request) {
        var existingDiagnosis = diagnosisRepository.findByMemberId(memberId);

        Diagnosis diagnosis;
        if (existingDiagnosis.isPresent()) {
            diagnosis = existingDiagnosis.get();
            diagnosis.updateScores(request.getScores());
        } else {
            diagnosis = Diagnosis.builder()
                    .memberId(memberId)
                    .totalScore(0)
                    .build();
            diagnosis.updateScores(request.getScores());
        }

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        return diagnosisConverter.toResponseDTO(savedDiagnosis, memberRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosisResponseDTO getMyDiagnosis(Long memberId) {
        Diagnosis diagnosis = diagnosisRepository.findByMemberId(memberId)
                .orElseThrow(() -> new DiagnosisException("진단 결과가 없습니다."));

        return diagnosisConverter.toResponseDTO(diagnosis, memberRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean hasInitialDiagnosis(Long memberId) {
        return diagnosisRepository.findByMemberId(memberId).isPresent();
    }
}