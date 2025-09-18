package org.example.seasontonebackend.smartdiagnosis.application;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.smartdiagnosis.converter.SmartDiagnosisConverter;
import org.example.seasontonebackend.smartdiagnosis.domain.entity.*;
import org.example.seasontonebackend.smartdiagnosis.domain.repository.*;
import org.example.seasontonebackend.smartdiagnosis.dto.SmartDiagnosisRequestDTO;
import org.example.seasontonebackend.smartdiagnosis.dto.SmartDiagnosisResponseDTO;
import org.example.seasontonebackend.smartdiagnosis.exception.SmartDiagnosisException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartDiagnosisService {

    private final SmartMeasurementRepository measurementRepository;
    private final LevelMeasurementDetailRepository levelDetailRepository;
    private final NoiseMeasurementDetailRepository noiseDetailRepository;
    private final NoiseDataPointRepository noiseDataPointRepository;
    private final InternetSpeedDetailRepository internetDetailRepository;
    private final SmartDiagnosisConverter converter;

    // ========== 소음 측정 기능 ==========

    @Transactional
    public SmartDiagnosisResponseDTO.NoiseStartResponse startNoiseMeasurement(
            Member member, SmartDiagnosisRequestDTO.NoiseStart request) {

        log.info("소음 측정 시작 - 사용자: {}, 위치: {}, 지속시간: {}초",
                member.getEmail(), request.getLocation(), request.getDuration());

        try {
            SmartMeasurement measurement = SmartMeasurement.builder()
                    .member(member)
                    .measurementType(SmartMeasurement.MeasurementType.NOISE)
                    .measuredValue(BigDecimal.ZERO)
                    .unit("dB")
                    .locationInfo(request.getLocation())
                    .deviceInfo("웹브라우저 마이크")
                    .measurementDuration(request.getDuration())
                    .build();

            SmartMeasurement savedMeasurement = measurementRepository.save(measurement);

            String sessionId = "noise_" + UUID.randomUUID().toString().substring(0, 8);
            String startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            return SmartDiagnosisResponseDTO.NoiseStartResponse.builder()
                    .sessionId(sessionId)
                    .measurementId(savedMeasurement.getMeasurementId())
                    .duration(request.getDuration())
                    .startTime(startTime)
                    .build();

        } catch (Exception e) {
            log.error("소음 측정 시작 실패", e);
            throw new SmartDiagnosisException("소음 측정을 시작할 수 없습니다.");
        }
    }

    @Transactional
    public SmartDiagnosisResponseDTO.NoiseRealtimeResponse processRealtimeNoise(
            Member member, SmartDiagnosisRequestDTO.NoiseRealtime request) {

        log.debug("실시간 소음 데이터 처리 - 세션: {}, 데시벨: {}",
                request.getSessionId(), request.getDecibel());

        try {
            String category = categorizeNoise(request.getDecibel());
            String recommendation = generateNoiseRecommendation(request.getDecibel(), category);

            return SmartDiagnosisResponseDTO.NoiseRealtimeResponse.builder()
                    .currentDecibel(request.getDecibel())
                    .category(category)
                    .recommendation(recommendation)
                    .build();

        } catch (Exception e) {
            log.error("실시간 소음 데이터 처리 실패", e);
            throw new SmartDiagnosisException("소음 데이터 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public SmartDiagnosisResponseDTO.NoiseCompleteResponse completeNoiseMeasurement(
            Member member, SmartDiagnosisRequestDTO.NoiseComplete request) {

        log.info("소음 측정 완료 - 사용자: {}, 세션: {}, 샘플 수: {}",
                member.getEmail(), request.getSessionId(), request.getSamples().size());

        try {
            // 최근 소음 측정 찾기
            List<SmartMeasurement> recentMeasurements = measurementRepository
                    .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                            member.getId(), SmartMeasurement.MeasurementType.NOISE);

            if (recentMeasurements.isEmpty()) {
                throw new SmartDiagnosisException("측정 세션을 찾을 수 없습니다.");
            }

            SmartMeasurement measurement = recentMeasurements.get(0);

            // 통계 계산
            NoiseStatistics stats = calculateNoiseStatistics(request.getSamples());

            // 측정값 업데이트
            measurement.setMeasuredValue(BigDecimal.valueOf(stats.getAvgDecibel()));
            measurementRepository.save(measurement);

            // 소음 세부 정보 저장
            NoiseMeasurementDetail detail = NoiseMeasurementDetail.builder()
                    .measurement(measurement)
                    .avgDecibel(BigDecimal.valueOf(stats.getAvgDecibel()))
                    .minDecibel(BigDecimal.valueOf(stats.getMinDecibel()))
                    .maxDecibel(BigDecimal.valueOf(stats.getMaxDecibel()))
                    .category(categorizeNoise(stats.getAvgDecibel()))
                    .comparisonText(generateNoiseComparison(stats.getAvgDecibel()))
                    .build();

            noiseDetailRepository.save(detail);

            // 개별 데이터 포인트 저장 (선택적)
            for (SmartDiagnosisRequestDTO.NoiseSample sample : request.getSamples()) {
                NoiseDataPoint dataPoint = NoiseDataPoint.builder()
                        .measurement(measurement)
                        .decibel(BigDecimal.valueOf(sample.getDecibel()))
                        .build();
                noiseDataPointRepository.save(dataPoint);
            }

            return SmartDiagnosisResponseDTO.NoiseCompleteResponse.builder()
                    .measurementId(measurement.getMeasurementId())
                    .avgDecibel(stats.getAvgDecibel())
                    .minDecibel(stats.getMinDecibel())
                    .maxDecibel(stats.getMaxDecibel())
                    .category(detail.getCategory())
                    .comparisonText(detail.getComparisonText())
                    .build();

        } catch (Exception e) {
            log.error("소음 측정 완료 처리 실패", e);
            throw new SmartDiagnosisException("소음 측정 완료 처리 중 오류가 발생했습니다.");
        }
    }

    // ========== 인터넷 속도 측정 기능 ==========

    @Transactional
    public SmartDiagnosisResponseDTO.InternetStartResponse startInternetSpeedTest(
            Member member, SmartDiagnosisRequestDTO.InternetStart request) {

        log.info("인터넷 속도 측정 시작 - 사용자: {}, 연결타입: {}, 위치: {}",
                member.getEmail(), request.getConnectionType(), request.getLocation());

        try {
            SmartMeasurement measurement = SmartMeasurement.builder()
                    .member(member)
                    .measurementType(SmartMeasurement.MeasurementType.INTERNET)
                    .measuredValue(BigDecimal.ZERO)
                    .unit("Mbps")
                    .locationInfo(request.getLocation())
                    .deviceInfo(request.getConnectionType())
                    .build();

            SmartMeasurement savedMeasurement = measurementRepository.save(measurement);

            String sessionId = "internet_" + UUID.randomUUID().toString().substring(0, 8);

            List<SmartDiagnosisResponseDTO.TestServer> testServers = Arrays.asList(
                    SmartDiagnosisResponseDTO.TestServer.builder()
                            .name("서울")
                            .location("Seoul")
                            .build()
            );

            return SmartDiagnosisResponseDTO.InternetStartResponse.builder()
                    .sessionId(sessionId)
                    .measurementId(savedMeasurement.getMeasurementId())
                    .testServers(testServers)
                    .build();

        } catch (Exception e) {
            log.error("인터넷 속도 측정 시작 실패", e);
            throw new SmartDiagnosisException("인터넷 속도 측정을 시작할 수 없습니다.");
        }
    }

    @Transactional
    public SmartDiagnosisResponseDTO.InternetCompleteResponse completeInternetSpeedTest(
            Member member, SmartDiagnosisRequestDTO.InternetComplete request) {

        log.info("인터넷 속도 측정 완료 - 사용자: {}, 다운로드: {}Mbps, 업로드: {}Mbps, 핑: {}ms",
                member.getEmail(), request.getDownloadSpeed(), request.getUploadSpeed(), request.getPing());

        try {
            // 최근 인터넷 측정 찾기
            List<SmartMeasurement> recentMeasurements = measurementRepository
                    .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                            member.getId(), SmartMeasurement.MeasurementType.INTERNET);

            if (recentMeasurements.isEmpty()) {
                throw new SmartDiagnosisException("측정 세션을 찾을 수 없습니다.");
            }

            SmartMeasurement measurement = recentMeasurements.get(0);

            // 다운로드 속도를 대표값으로 저장
            measurement.setMeasuredValue(BigDecimal.valueOf(request.getDownloadSpeed()));
            measurementRepository.save(measurement);

            // 인터넷 속도 등급 및 비교 계산
            String speedGrade = calculateSpeedGrade(request.getDownloadSpeed());
            String comparison = generateSpeedComparison(request.getDownloadSpeed());

            // 인터넷 속도 세부 정보 저장
            InternetSpeedDetail detail = InternetSpeedDetail.builder()
                    .measurement(measurement)
                    .downloadSpeed(BigDecimal.valueOf(request.getDownloadSpeed()))
                    .uploadSpeed(BigDecimal.valueOf(request.getUploadSpeed()))
                    .ping(BigDecimal.valueOf(request.getPing()))
                    .connectionType(measurement.getDeviceInfo())
                    .serverLocation(request.getServerLocation())
                    .speedGrade(speedGrade)
                    .comparison(comparison)
                    .build();

            internetDetailRepository.save(detail);

            return SmartDiagnosisResponseDTO.InternetCompleteResponse.builder()
                    .measurementId(measurement.getMeasurementId())
                    .downloadSpeed(request.getDownloadSpeed())
                    .uploadSpeed(request.getUploadSpeed())
                    .ping(request.getPing())
                    .speedGrade(speedGrade)
                    .comparison(comparison)
                    .build();

        } catch (Exception e) {
            log.error("인터넷 속도 측정 완료 처리 실패", e);
            throw new SmartDiagnosisException("인터넷 속도 측정 완료 처리 중 오류가 발생했습니다.");
        }
    }

    // ========== 통합 요약 기능 ==========

    public SmartDiagnosisResponseDTO.SmartDiagnosisSummary getSmartDiagnosisSummary(Member member) {
        log.info("스마트 진단 종합 결과 조회 - 사용자: {}", member.getEmail());

        try {
            // 각 측정 타입별 최근 결과 조회
            SmartDiagnosisResponseDTO.LevelSummary levelSummary = getLatestLevelSummary(member);
            SmartDiagnosisResponseDTO.NoiseSummary noiseSummary = getLatestNoiseSummary(member);
            SmartDiagnosisResponseDTO.InternetSummary internetSummary = getLatestInternetSummary(member);

            SmartDiagnosisResponseDTO.MeasurementSummary measurements =
                    SmartDiagnosisResponseDTO.MeasurementSummary.builder()
                            .level(levelSummary)
                            .noise(noiseSummary)
                            .internet(internetSummary)
                            .build();

            // 종합 점수 계산
            Integer overallScore = calculateOverallScore(levelSummary, noiseSummary, internetSummary);

            // 인사이트 생성
            List<String> insights = generateInsights(levelSummary, noiseSummary, internetSummary);

            return SmartDiagnosisResponseDTO.SmartDiagnosisSummary.builder()
                    .overallScore(overallScore)
                    .measurements(measurements)
                    .insights(insights)
                    .build();

        } catch (Exception e) {
            log.error("스마트 진단 종합 결과 조회 실패", e);
            throw new SmartDiagnosisException("종합 결과 조회 중 오류가 발생했습니다.");
        }
    }

    // ========== 기존 수평계 기능들 (그대로 유지) ==========

    @Transactional
    public SmartDiagnosisResponseDTO.LevelStartResponse startLevelMeasurement(
            Member member, SmartDiagnosisRequestDTO.LevelStart request) {

        log.info("수평 측정 시작 - 사용자: {}, 위치: {}", member.getEmail(), request.getLocation());

        try {
            SmartMeasurement measurement = SmartMeasurement.builder()
                    .member(member)
                    .measurementType(SmartMeasurement.MeasurementType.LEVEL)
                    .measuredValue(BigDecimal.ZERO)
                    .unit("degree")
                    .locationInfo(request.getLocation())
                    .deviceInfo("웹브라우저")
                    .build();

            SmartMeasurement savedMeasurement = measurementRepository.save(measurement);

            String sessionId = "level_" + UUID.randomUUID().toString().substring(0, 8);

            List<String> instructions = Arrays.asList(
                    "기기를 평평한 곳에 놓아주세요",
                    "3초간 움직이지 마세요",
                    "측정이 자동으로 완료됩니다"
            );

            return SmartDiagnosisResponseDTO.LevelStartResponse.builder()
                    .sessionId(sessionId)
                    .measurementId(savedMeasurement.getMeasurementId())
                    .instructions(instructions)
                    .build();

        } catch (Exception e) {
            log.error("수평 측정 시작 실패", e);
            throw new SmartDiagnosisException("수평 측정을 시작할 수 없습니다.");
        }
    }

    @Transactional
    public SmartDiagnosisResponseDTO.LevelMeasureResponse processLevelMeasurement(
            Member member, SmartDiagnosisRequestDTO.LevelMeasure request) {

        log.info("수평 데이터 처리 - 사용자: {}, 세션: {}", member.getEmail(), request.getSessionId());

        try {
            List<SmartMeasurement> recentMeasurements = measurementRepository
                    .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                            member.getId(), SmartMeasurement.MeasurementType.LEVEL);

            if (recentMeasurements.isEmpty()) {
                throw new SmartDiagnosisException("측정 세션을 찾을 수 없습니다.");
            }

            SmartMeasurement measurement = recentMeasurements.get(0);
            LevelCalculationResult result = calculateLevel(request.getGyroscope());

            measurement.setMeasuredValue(BigDecimal.valueOf(result.getTotalTilt()));
            measurementRepository.save(measurement);

            LevelMeasurementDetail detail = LevelMeasurementDetail.builder()
                    .measurement(measurement)
                    .xAxis(BigDecimal.valueOf(result.getXAxis()))
                    .yAxis(BigDecimal.valueOf(result.getYAxis()))
                    .zAxis(BigDecimal.valueOf(result.getZAxis()))
                    .totalTilt(BigDecimal.valueOf(result.getTotalTilt()))
                    .isLevel(result.getIsLevel())
                    .levelStatus(result.getStatus())
                    .build();

            levelDetailRepository.save(detail);

            return SmartDiagnosisResponseDTO.LevelMeasureResponse.builder()
                    .measurementId(measurement.getMeasurementId())
                    .xAxisTilt(result.getXAxis())
                    .yAxisTilt(result.getYAxis())
                    .totalTilt(result.getTotalTilt())
                    .isLevel(result.getIsLevel())
                    .levelStatus(result.getStatus())
                    .recommendation(generateLevelRecommendation(result))
                    .build();

        } catch (Exception e) {
            log.error("수평 데이터 처리 실패", e);
            throw new SmartDiagnosisException("수평 데이터 처리 중 오류가 발생했습니다.");
        }
    }

    public List<SmartDiagnosisResponseDTO.LevelHistory> getLevelHistory(Member member, int limit) {
        List<SmartMeasurement> measurements = measurementRepository
                .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                        member.getId(), SmartMeasurement.MeasurementType.LEVEL)
                .stream()
                .limit(limit)
                .toList();

        return converter.toLevelHistoryList(measurements);
    }

    public SmartDiagnosisResponseDTO.MeasurementDetail getMeasurementDetail(Member member, Long measurementId) {
        SmartMeasurement measurement = measurementRepository.findById(measurementId)
                .orElseThrow(() -> new SmartDiagnosisException("측정 결과를 찾을 수 없습니다."));

        if (!measurement.getMember().getId().equals(member.getId())) {
            throw new SmartDiagnosisException("접근 권한이 없습니다.");
        }

        LevelMeasurementDetail levelDetail = levelDetailRepository
                .findByMeasurementMeasurementId(measurementId).orElse(null);

        NoiseMeasurementDetail noiseDetail = noiseDetailRepository
                .findByMeasurementMeasurementId(measurementId).orElse(null);

        InternetSpeedDetail internetDetail = internetDetailRepository
                .findByMeasurementMeasurementId(measurementId).orElse(null);

        return converter.toMeasurementDetailDto(measurement, levelDetail, noiseDetail, internetDetail);
    }

    // ========== 유틸리티 메서드들 ==========

    private NoiseStatistics calculateNoiseStatistics(List<SmartDiagnosisRequestDTO.NoiseSample> samples) {
        if (samples.isEmpty()) {
            return NoiseStatistics.builder()
                    .avgDecibel(35.0)
                    .minDecibel(35.0)
                    .maxDecibel(35.0)
                    .build();
        }

        // 유효한 데이터만 필터링 (NaN, Infinity, 음수 제거)
        List<Double> validDecibels = samples.stream()
                .map(SmartDiagnosisRequestDTO.NoiseSample::getDecibel)
                .filter(decibel -> decibel != null && !Double.isNaN(decibel) && Double.isFinite(decibel) && decibel > 0)
                .collect(java.util.stream.Collectors.toList());

        if (validDecibels.isEmpty()) {
            return NoiseStatistics.builder()
                    .avgDecibel(35.0)
                    .minDecibel(35.0)
                    .maxDecibel(35.0)
                    .build();
        }

        double sum = validDecibels.stream().mapToDouble(Double::doubleValue).sum();
        double min = validDecibels.stream().mapToDouble(Double::doubleValue).min().orElse(35.0);
        double max = validDecibels.stream().mapToDouble(Double::doubleValue).max().orElse(35.0);

        return NoiseStatistics.builder()
                .avgDecibel(roundToTwo(sum / validDecibels.size()))
                .minDecibel(roundToTwo(min))
                .maxDecibel(roundToTwo(max))
                .build();
    }

    private String categorizeNoise(Double decibel) {
        if (decibel == null) return "알 수 없음";
        if (decibel < 30) return "매우 조용함";
        if (decibel < 40) return "조용함";
        if (decibel < 50) return "보통";
        if (decibel < 60) return "약간 시끄러움";
        if (decibel < 70) return "시끄러움";
        if (decibel < 80) return "매우 시끄러움";
        return "위험 수준";
    }

    private String generateNoiseRecommendation(Double decibel, String category) {
        if (decibel == null) return "측정값을 확인할 수 없습니다.";

        if (decibel < 40) {
            return "매우 조용한 환경입니다. 독서나 휴식에 적합합니다.";
        } else if (decibel < 50) {
            return "일반적인 생활소음 수준입니다.";
        } else if (decibel < 60) {
            return "대화에 약간 방해가 될 수 있는 수준입니다.";
        } else if (decibel < 70) {
            return "시끄러운 환경입니다. 장시간 노출 시 주의가 필요합니다.";
        } else {
            return "매우 시끄러운 환경입니다. 청력 보호를 위해 주의하세요.";
        }
    }

    private String generateNoiseComparison(Double avgDecibel) {
        if (avgDecibel < 35) {
            return "도서관보다 조용한 수준입니다.";
        } else if (avgDecibel < 45) {
            return "일반 주거지역 기준보다 5dB 낮습니다.";
        } else if (avgDecibel < 55) {
            return "일반 주거지역 기준과 비슷한 수준입니다.";
        } else if (avgDecibel < 65) {
            return "일반 주거지역 기준보다 10dB 높습니다.";
        } else {
            return "주거지역 기준을 초과하는 수준입니다.";
        }
    }

    private String calculateSpeedGrade(Double downloadSpeed) {
        if (downloadSpeed == null) return "측정 불가";
        if (downloadSpeed < 25) return "느림";
        if (downloadSpeed < 50) return "보통";
        if (downloadSpeed < 100) return "빠름";
        if (downloadSpeed < 200) return "매우 빠름";
        return "초고속";
    }

    private String generateSpeedComparison(Double downloadSpeed) {
        if (downloadSpeed == null) return "측정값을 확인할 수 없습니다.";

        // 한국 평균 인터넷 속도를 기준으로 비교 (예: 95Mbps)
        double koreanAverage = 95.0;
        double percentage = (downloadSpeed / koreanAverage) * 100;

        if (percentage > 120) {
            return String.format("전국 평균보다 %.0f%% 빠름", percentage - 100);
        } else if (percentage > 80) {
            return "전국 평균과 비슷한 수준";
        } else {
            return String.format("전국 평균보다 %.0f%% 느림", 100 - percentage);
        }
    }

    private SmartDiagnosisResponseDTO.LevelSummary getLatestLevelSummary(Member member) {
        List<SmartMeasurement> measurements = measurementRepository
                .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                        member.getId(), SmartMeasurement.MeasurementType.LEVEL)
                .stream()
                .limit(1)
                .toList();

        if (measurements.isEmpty()) {
            return SmartDiagnosisResponseDTO.LevelSummary.builder()
                    .latestValue(0.0)
                    .grade("측정 없음")
                    .trend("측정 필요")
                    .build();
        }

        SmartMeasurement latest = measurements.get(0);
        double tilt = latest.getMeasuredValue().doubleValue();

        return SmartDiagnosisResponseDTO.LevelSummary.builder()
                .latestValue(tilt)
                .grade(getLevelGrade(tilt))
                .trend("안정적")
                .build();
    }

    private SmartDiagnosisResponseDTO.NoiseSummary getLatestNoiseSummary(Member member) {
        List<SmartMeasurement> measurements = measurementRepository
                .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                        member.getId(), SmartMeasurement.MeasurementType.NOISE)
                .stream()
                .limit(1)
                .toList();

        if (measurements.isEmpty()) {
            return SmartDiagnosisResponseDTO.NoiseSummary.builder()
                    .latestValue(0.0)
                    .grade("측정 없음")
                    .trend("측정 필요")
                    .build();
        }

        SmartMeasurement latest = measurements.get(0);
        double noise = latest.getMeasuredValue().doubleValue();

        return SmartDiagnosisResponseDTO.NoiseSummary.builder()
                .latestValue(noise)
                .grade(getNoiseGrade(noise))
                .trend("개선됨")
                .build();
    }

    private SmartDiagnosisResponseDTO.InternetSummary getLatestInternetSummary(Member member) {
        List<SmartMeasurement> measurements = measurementRepository
                .findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
                        member.getId(), SmartMeasurement.MeasurementType.INTERNET)
                .stream()
                .limit(1)
                .toList();

        if (measurements.isEmpty()) {
            return SmartDiagnosisResponseDTO.InternetSummary.builder()
                    .latestValue(0.0)
                    .grade("측정 없음")
                    .comparison("측정 필요")
                    .build();
        }

        SmartMeasurement latest = measurements.get(0);
        double speed = latest.getMeasuredValue().doubleValue();

        return SmartDiagnosisResponseDTO.InternetSummary.builder()
                .latestValue(speed)
                .grade(calculateSpeedGrade(speed))
                .comparison("평균 대비 108%")
                .build();
    }

    private Integer calculateOverallScore(
            SmartDiagnosisResponseDTO.LevelSummary level,
            SmartDiagnosisResponseDTO.NoiseSummary noise,
            SmartDiagnosisResponseDTO.InternetSummary internet) {

        int levelScore = getGradeScore(level.getGrade());
        int noiseScore = getGradeScore(noise.getGrade());
        int internetScore = getGradeScore(internet.getGrade());

        return (levelScore + noiseScore + internetScore) / 3;
    }

    private List<String> generateInsights(
            SmartDiagnosisResponseDTO.LevelSummary level,
            SmartDiagnosisResponseDTO.NoiseSummary noise,
            SmartDiagnosisResponseDTO.InternetSummary internet) {

        List<String> insights = new ArrayList<>();

        if ("우수".equals(level.getGrade())) {
            insights.add("수평 상태가 양호합니다.");
        }

        if ("우수".equals(noise.getGrade()) || "매우 조용함".equals(noise.getGrade())) {
            insights.add("소음 환경이 매우 좋습니다.");
        }

        if ("빠름".equals(internet.getGrade()) || "매우 빠름".equals(internet.getGrade()) || "초고속".equals(internet.getGrade())) {
            insights.add("인터넷 속도가 우수합니다.");
        }

        if (insights.isEmpty()) {
            insights.add("추가 측정을 통해 더 정확한 진단을 받아보세요.");
        }

        return insights;
    }

    private String getLevelGrade(double tilt) {
        if (tilt < 1.0) return "우수";
        if (tilt < 2.0) return "양호";
        if (tilt < 5.0) return "보통";
        return "개선 필요";
    }

    private String getNoiseGrade(double decibel) {
        if (decibel < 40) return "우수";
        if (decibel < 50) return "양호";
        if (decibel < 60) return "보통";
        return "개선 필요";
    }

    private int getGradeScore(String grade) {
        switch (grade) {
            case "우수": case "매우 빠름": case "초고속": return 90;
            case "양호": case "빠름": return 75;
            case "보통": return 60;
            case "개선 필요": case "느림": return 40;
            default: return 0;
        }
    }

    // 기존 수평계 관련 메서드들
    private LevelCalculationResult calculateLevel(SmartDiagnosisRequestDTO.GyroscopeData gyroscope) {
        double beta = gyroscope.getBeta() != null ? gyroscope.getBeta() : 0.0;
        double gamma = gyroscope.getGamma() != null ? gyroscope.getGamma() : 0.0;
        double alpha = gyroscope.getAlpha() != null ? gyroscope.getAlpha() : 0.0;

        double totalTilt = Math.sqrt(beta * beta + gamma * gamma);
        boolean isLevel = totalTilt < 2.0;

        return LevelCalculationResult.builder()
                .xAxis(roundToTwo(gamma))
                .yAxis(roundToTwo(beta))
                .zAxis(roundToTwo(alpha))
                .totalTilt(roundToTwo(totalTilt))
                .isLevel(isLevel)
                .status(getLevelStatusFromTilt(totalTilt))
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

    private String generateLevelRecommendation(LevelCalculationResult result) {
        if (result.getIsLevel()) {
            return "바닥이 평평합니다. 가구 배치에 적합한 상태입니다.";
        } else if (result.getTotalTilt() < 5.0) {
            return "약간의 기울기가 있습니다. 가구 다리 조절을 고려해보세요.";
        } else {
            return "상당한 기울기가 감지되었습니다. 수평 조절이 필요할 수 있습니다.";
        }
    }

    private double roundToTwo(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // 내부 클래스들
    @Getter
    @Builder
    private static class NoiseStatistics {
        private Double avgDecibel;
        private Double minDecibel;
        private Double maxDecibel;
    }

    @Getter
    @Builder
    private static class LevelCalculationResult {
        private Double xAxis;
        private Double yAxis;
        private Double zAxis;
        private Double totalTilt;
        private Boolean isLevel;
        private String status;
    }
}