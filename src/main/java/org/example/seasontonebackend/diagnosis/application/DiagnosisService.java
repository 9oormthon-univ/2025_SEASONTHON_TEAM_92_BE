package org.example.seasontonebackend.diagnosis.application;

import org.example.seasontonebackend.diagnosis.dto.*;

public interface DiagnosisService {
    DiagnosisResponseDTO createOrUpdateDiagnosis(Long memberId, DiagnosisRequestDTO request);
    DiagnosisResponseDTO getMyDiagnosis(Long memberId);
    Boolean hasInitialDiagnosis(Long memberId);
}