package org.example.seasontonebackend.policy.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.policy.dto.DisputeAgencyResponseDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeAgencyService {

    public DisputeAgencyResponseDTO.AgencyList getAgenciesByRegion(String region, String agencyType) {
        List<DisputeAgencyResponseDTO.AgencyDetail> allAgencies = getAllAgencies();

        List<DisputeAgencyResponseDTO.AgencyDetail> filteredAgencies = allAgencies.stream()
                .filter(agency -> isAgencyInRegion(agency, region))
                .filter(agency -> agencyType == null || agency.getAgencyType().equals(agencyType))
                .toList();

        return DisputeAgencyResponseDTO.AgencyList.builder()
                .agencies(filteredAgencies)
                .userRegion(region)
                .totalCount(filteredAgencies.size())
                .build();
    }

    public DisputeAgencyResponseDTO.AgencyList getRecommendedAgencies(String disputeType, Member member) {
        String userRegion = member.getDong();
        List<DisputeAgencyResponseDTO.AgencyDetail> allAgencies = getAllAgencies();

        List<DisputeAgencyResponseDTO.AgencyDetail> recommendedAgencies = allAgencies.stream()
                .filter(agency -> isAgencyInRegion(agency, userRegion))
                .filter(agency -> isAgencyRelevantForDispute(agency, disputeType))
                .limit(3)
                .toList();

        return DisputeAgencyResponseDTO.AgencyList.builder()
                .agencies(recommendedAgencies)
                .userRegion(userRegion)
                .totalCount(recommendedAgencies.size())
                .build();
    }

    private List<DisputeAgencyResponseDTO.AgencyDetail> getAllAgencies() {
        return Arrays.asList(
                DisputeAgencyResponseDTO.AgencyDetail.builder()
                        .agencyId(1L)
                        .agencyName("서울특별시 임대차분쟁조정위원회")
                        .agencyType("조정위원회")
                        .description("임대차 관련 분쟁의 조정을 담당하는 기관")
                        .jurisdiction("서울특별시 전역")
                        .contactInfo(DisputeAgencyResponseDTO.ContactInfo.builder()
                                .phone("02-000-0000")
                                .address("서울시 중구 세종대로 110")
                                .website("https://seoul.go.kr")
                                .build())
                        .operatingHours("평일 09:00-18:00")
                        .serviceTypes(Arrays.asList("조정신청", "상담", "법률자문"))
                        .processingTime("접수 후 30일 이내")
                        .costInfo("무료")
                        .build(),

                DisputeAgencyResponseDTO.AgencyDetail.builder()
                        .agencyId(2L)
                        .agencyName("청년법률센터")
                        .agencyType("법률지원기관")
                        .description("청년 대상 무료 법률 상담 및 지원")
                        .jurisdiction("전국")
                        .contactInfo(DisputeAgencyResponseDTO.ContactInfo.builder()
                                .phone("02-1234-5678")
                                .address("서울시 서대문구 충정로 15")
                                .website("https://youthlegal.or.kr")
                                .build())
                        .operatingHours("평일 09:00-18:00")
                        .serviceTypes(Arrays.asList("무료상담", "법률지원", "권익보호"))
                        .processingTime("즉시 상담 가능")
                        .costInfo("무료")
                        .build(),

                DisputeAgencyResponseDTO.AgencyDetail.builder()
                        .agencyId(3L)
                        .agencyName("마포구 청년지원센터")
                        .agencyType("청년지원기관")
                        .description("마포구 청년 대상 종합 지원 서비스")
                        .jurisdiction("마포구")
                        .contactInfo(DisputeAgencyResponseDTO.ContactInfo.builder()
                                .phone("02-3153-8100")
                                .address("서울시 마포구 월드컵로 212 마포구청")
                                .website("https://mapo.go.kr/youth")
                                .build())
                        .operatingHours("평일 09:00-18:00")
                        .serviceTypes(Arrays.asList("정책안내", "주거상담", "분쟁1차상담"))
                        .processingTime("당일 상담 가능")
                        .costInfo("무료")
                        .build()
        );
    }

    private boolean isAgencyInRegion(DisputeAgencyResponseDTO.AgencyDetail agency, String region) {
        if (region == null) return true;

        String jurisdiction = agency.getJurisdiction();

        if (region.contains("마포")) {
            return jurisdiction.contains("마포구") ||
                    jurisdiction.contains("서울") ||
                    jurisdiction.contains("전국");
        } else if (region.contains("강남")) {
            return jurisdiction.contains("강남구") ||
                    jurisdiction.contains("서울") ||
                    jurisdiction.contains("전국");
        } else if (region.contains("서울")) {
            return jurisdiction.contains("서울") ||
                    jurisdiction.contains("전국");
        }

        return jurisdiction.contains("전국");
    }

    private boolean isAgencyRelevantForDispute(DisputeAgencyResponseDTO.AgencyDetail agency, String disputeType) {
        String agencyName = agency.getAgencyName().toLowerCase();
        String description = agency.getDescription().toLowerCase();

        switch (disputeType) {
            case "보증금반환":
                return agencyName.contains("조정") || agencyName.contains("법률") || description.contains("분쟁");
            case "임대료인상":
                return agencyName.contains("조정") || description.contains("임대차");
            case "수선의무":
                return agencyName.contains("조정") || description.contains("상담");
            default:
                return true;
        }
    }
}
