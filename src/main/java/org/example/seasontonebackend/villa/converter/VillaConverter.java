package org.example.seasontonebackend.villa.converter;

import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.villa.dto.VillaMarketDataResponseDTO;
import org.example.seasontonebackend.villa.dto.VillaTransactionResponseDTO;
import org.example.seasontonebackend.villa.dto.VillaPublicApiResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VillaConverter {

    private static final String DATE_FORMAT = "%d-%02d-%02d";

    public VillaTransactionResponseDTO convertToTransactionDTO(VillaPublicApiResponseDTO.Item item) {
        String contractDate = String.format(DATE_FORMAT, item.getYear(), item.getMonth(), item.getDay());
        return VillaTransactionResponseDTO.builder()
                .buildingName(Optional.ofNullable(item.getBuildingName()).orElse("빌라").trim())
                .deposit(Optional.ofNullable(item.getDeposit()).orElse("0").trim())
                .monthlyRent(Optional.ofNullable(item.getMonthlyRent()).orElse("0").trim())
                .area(String.valueOf(item.getArea()))
                .contractDate(contractDate)
                .floor("N/A") // 빌라 API에는 층 정보가 없음
                .buildYear(Optional.ofNullable(item.getBuildYear()).orElse("N/A").trim())
                .contractType(Optional.ofNullable(item.getContractType()).orElse("N/A").trim())
                .contractTerm(Optional.ofNullable(item.getContractTerm()).orElse("N/A").trim())
                .build();
    }

    public VillaMarketDataResponseDTO calculateJeonseMarketData(String neighborhood, List<VillaPublicApiResponseDTO.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .filter(deposit -> deposit > 0)
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianDeposit = calculateMedian(deposits);
        String recentDate = findMostRecentDate(items);
        String district = extractDistrict(items);

        return VillaMarketDataResponseDTO.builder()
                .neighborhood(neighborhood)
                .district(district)
                .avgDeposit(roundToTwoDecimals(avgDeposit))
                .avgMonthlyRent(0.0)
                .medianDeposit(roundToTwoDecimals(medianDeposit))
                .medianMonthlyRent(0.0)
                .transactionCount(items.size())
                .recentTransactionDate(recentDate)
                .build();
    }

    public VillaMarketDataResponseDTO calculateMonthlyRentMarketData(String neighborhood, List<VillaPublicApiResponseDTO.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .collect(Collectors.toList());

        List<Double> monthlyRents = items.stream()
                .map(item -> parseAmount(item.getMonthlyRent()))
                .filter(rent -> rent > 0)
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgMonthlyRent = monthlyRents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianDeposit = calculateMedian(deposits);
        double medianMonthlyRent = calculateMedian(monthlyRents);
        String recentDate = findMostRecentDate(items);
        String district = extractDistrict(items);

        return VillaMarketDataResponseDTO.builder()
                .neighborhood(neighborhood)
                .district(district)
                .avgDeposit(roundToTwoDecimals(avgDeposit))
                .avgMonthlyRent(roundToTwoDecimals(avgMonthlyRent))
                .medianDeposit(roundToTwoDecimals(medianDeposit))
                .medianMonthlyRent(roundToTwoDecimals(medianMonthlyRent))
                .transactionCount(items.size())
                .recentTransactionDate(recentDate)
                .build();
    }

    public double parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0.0;
        }
        try {
            // 금액을 만원 단위로 변환 (원 단위를 만원으로 나누기)
            double amountInWon = Double.parseDouble(amount.replace(",", "").trim());
            return amountInWon / 10000.0; // 만원 단위로 변환
        } catch (NumberFormatException e) {
            log.warn("빌라 금액 파싱 실패: {}", amount);
            return 0.0;
        }
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();

        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private String findMostRecentDate(List<VillaPublicApiResponseDTO.Item> items) {
        if (items.isEmpty()) return "";

        return items.stream()
                .map(item -> String.format(DATE_FORMAT, item.getYear(), item.getMonth(), item.getDay()))
                .max(String::compareTo)
                .orElse("");
    }

    private String extractDistrict(List<VillaPublicApiResponseDTO.Item> items) {
        return items.stream()
                .map(VillaPublicApiResponseDTO.Item::getDistrict)
                .filter(district -> district != null && !district.trim().isEmpty())
                .findFirst()
                .orElse("");
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}