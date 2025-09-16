package org.example.seasontonebackend.smartdiagnosis.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.smartdiagnosis.domain.entity.*;
import org.example.seasontonebackend.smartdiagnosis.dto.SmartDiagnosisResponseDTO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmartDiagnosisConverter {

    public SmartDiagnosisResponseDTO.LevelHistory toLevelHistoryDto(SmartMeasurement measurement) {
        return SmartDiagnosisResponseDTO.LevelHistory.builder()
                .measurementId(measurement.getMeasurementId())
                .location(measurement.getLocationInfo())
                .totalTilt(measurement.getMeasuredValue().doubleValue())
                .levelStatus(getLevelStatusFromTilt(measurement.getMeasuredValue().doubleValue()))
                .createdAt(measurement.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    public List<SmartDiagnosisResponseDTO.LevelHistory> toLevelHistoryList(List<SmartMeasurement> measurements) {
        return measurements.stream()
                .map(this::toLevelHistoryDto)
                .collect(Collectors.toList());
    }

    public SmartDiagnosisResponseDTO.MeasurementDetail toMeasurementDetailDto(
            SmartMeasurement measurement,
            LevelMeasurementDetail levelDetail,
            NoiseMeasurementDetail noiseDetail,
            InternetSpeedDetail internetDetail) {

        SmartDiagnosisResponseDTO.MeasurementInfo measurementInfo =
                SmartDiagnosisResponseDTO.MeasurementInfo.builder()
                        .measurementId(measurement.getMeasurementId())
                        .measurementType(measurement.getMeasurementType().name())
                        .location(measurement.getLocationInfo())
                        .measuredValue(measurement.getMeasuredValue().doubleValue())
                        .unit(measurement.getUnit())
                        .createdAt(measurement.getCreatedAt())
                        .build();

        SmartDiagnosisResponseDTO.LevelDetail levelDetailDto = null;
        if (levelDetail != null) {
            levelDetailDto = SmartDiagnosisResponseDTO.LevelDetail.builder()
                    .xAxis(levelDetail.getXAxis().doubleValue())
                    .yAxis(levelDetail.getYAxis().doubleValue())
                    .zAxis(levelDetail.getZAxis().doubleValue())
                    .totalTilt(levelDetail.getTotalTilt().doubleValue())
                    .isLevel(levelDetail.getIsLevel())
                    .levelStatus(levelDetail.getLevelStatus())
                    .build();
        }

        SmartDiagnosisResponseDTO.NoiseDetail noiseDetailDto = null;
        if (noiseDetail != null) {
            noiseDetailDto = SmartDiagnosisResponseDTO.NoiseDetail.builder()
                    .avgDecibel(noiseDetail.getAvgDecibel().doubleValue())
                    .minDecibel(noiseDetail.getMinDecibel().doubleValue())
                    .maxDecibel(noiseDetail.getMaxDecibel().doubleValue())
                    .category(noiseDetail.getCategory())
                    .comparisonText(noiseDetail.getComparisonText())
                    .build();
        }

        SmartDiagnosisResponseDTO.InternetDetail internetDetailDto = null;
        if (internetDetail != null) {
            internetDetailDto = SmartDiagnosisResponseDTO.InternetDetail.builder()
                    .downloadSpeed(internetDetail.getDownloadSpeed().doubleValue())
                    .uploadSpeed(internetDetail.getUploadSpeed().doubleValue())
                    .ping(internetDetail.getPing().doubleValue())
                    .speedGrade(internetDetail.getSpeedGrade())
                    .comparison(internetDetail.getComparison())
                    .build();
        }

        return SmartDiagnosisResponseDTO.MeasurementDetail.builder()
                .measurement(measurementInfo)
                .levelDetails(levelDetailDto)
                .noiseDetails(noiseDetailDto)
                .internetDetails(internetDetailDto)
                .build();
    }

    private String getLevelStatusFromTilt(double tilt) {
        if (tilt < 0.5) return "완전 수평";
        if (tilt < 1.0) return "거의 수평";
        if (tilt < 2.0) return "약간 기울어짐";
        if (tilt < 5.0) return "기울어짐";
        if (tilt < 10.0) return "많이 기울어짐";
        return "매우 기울어짐";
    }
}