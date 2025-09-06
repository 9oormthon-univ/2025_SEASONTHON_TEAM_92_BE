package org.example.seasontonebackend.officetel.converter;

import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.officetel.dto.OfficetelMarketDataResponseDTO;
import org.example.seasontonebackend.officetel.dto.OfficetelTransactionResponseDTO;
import org.example.seasontonebackend.officetel.dto.PublicApiResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OfficetelConverter {

    private static final String DATE_FORMAT = "%d-%02d-%02d";

    public OfficetelTransactionResponseDTO convertToTransactionDTO(PublicApiResponseDTO.Item item) {
        String contractDate = String.format(DATE_FORMAT, item.getYear(), item.getMonth(), item.getDay());
        return OfficetelTransactionResponseDTO.builder()
                .buildingName(Optional.ofNullable(item.getBuildingName()).orElse("N/A").trim())
                .deposit(Optional.ofNullable(item.getDeposit()).orElse("0").trim())
                .monthlyRent(Optional.ofNullable(item.getMonthlyRent()).orElse("0").trim())
                .area(String.valueOf(item.getArea()))
                .contractDate(contractDate)
                .floor(Optional.ofNullable(item.getFloor()).orElse("N/A").trim())
                .buildYear(Optional.ofNullable(item.getBuildYear()).orElse("N/A").trim())
                .contractType(Optional.ofNullable(item.getContractType()).orElse("N/A").trim())
                .contractTerm(Optional.ofNullable(item.getContractTerm()).orElse("N/A").trim())
                .build();
    }

    public OfficetelMarketDataResponseDTO calculateJeonseMarketData(String neighborhood, List<PublicApiResponseDTO.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .filter(deposit -> deposit > 0)
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianDeposit = calculateMedian(deposits);
        String recentDate = findMostRecentDate(items);
        String district = extractDistrict(items);

        return OfficetelMarketDataResponseDTO.builder()
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

    public OfficetelMarketDataResponseDTO calculateMonthlyRentMarketData(String neighborhood, List<PublicApiResponseDTO.Item> items) {
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

        return OfficetelMarketDataResponseDTO.builder()
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
            return Double.parseDouble(amount.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("금액 파싱 실패: {}", amount);
            return 0.0;
        }
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String findMostRecentDate(List<PublicApiResponseDTO.Item> items) {
        return items.stream()
                .map(item -> String.format(DATE_FORMAT, item.getYear(), item.getMonth(), item.getDay()))
                .max(String::compareTo)
                .orElse("N/A");
    }

    private String extractDistrict(List<PublicApiResponseDTO.Item> items) {
        return items.isEmpty() ? "N/A" :
                Optional.ofNullable(items.get(0).getDistrict()).orElse("N/A");
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        Collections.sort(values);
        int size = values.size();

        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2);
        }
    }
}