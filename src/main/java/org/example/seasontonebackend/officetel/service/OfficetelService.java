package org.example.seasontonebackend.officetel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.example.seasontonebackend.officetel.dto.OfficetelTransactionDto;
import org.example.seasontonebackend.officetel.dto.OfficetelMarketDataDto;
import org.example.seasontonebackend.officetel.dto.PublicApiResponseDto;

@Slf4j
@Service
public class OfficetelService {

    // 상수 정의
    private static final String API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent";
    private static final String DATE_FORMAT = "%d-%02d-%02d";
    private static final int MONTHS_TO_FETCH = 3;
    private static final int MAX_ROWS_PER_REQUEST = 100;

    @Value("${officetel.api.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();

    // 공통 데이터 수집 메서드
    private List<PublicApiResponseDto.Item> fetchAllItemsForPeriod(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < MONTHS_TO_FETCH; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<PublicApiResponseDto.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null && !monthlyItems.isEmpty()) {
                allItems.addAll(monthlyItems);
            }
        }
        return allItems;
    }

    public Map<String, List<OfficetelTransactionDto>> getOfficetelRentData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        return allItems.stream()
                .map(this::convertToDto)
                .collect(Collectors.groupingBy(OfficetelTransactionDto::getBuildingName));
    }

    public List<OfficetelMarketDataDto> getJeonseMarketData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        // 전세만 필터링 (월세가 0인 경우)
        List<PublicApiResponseDto.Item> jeonseItems = allItems.stream()
                .filter(this::isValidNeighborhood)
                .filter(this::isJeonseTransaction)
                .collect(Collectors.toList());

        return groupByNeighborhoodAndCalculate(jeonseItems, this::calculateJeonseMarketData);
    }

    public List<OfficetelMarketDataDto> getMonthlyRentMarketData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        // 월세만 필터링 (월세가 0보다 큰 경우)
        List<PublicApiResponseDto.Item> monthlyRentItems = allItems.stream()
                .filter(this::isValidNeighborhood)
                .filter(this::isMonthlyRentTransaction)
                .collect(Collectors.toList());

        return groupByNeighborhoodAndCalculate(monthlyRentItems, this::calculateMonthlyRentMarketData);
    }

    // 필터링 메서드들
    private boolean isValidNeighborhood(PublicApiResponseDto.Item item) {
        return item.getNeighborhood() != null && !item.getNeighborhood().trim().isEmpty();
    }

    private boolean isJeonseTransaction(PublicApiResponseDto.Item item) {
        return parseAmount(item.getMonthlyRent()) == 0.0 && parseAmount(item.getDeposit()) > 0.0;
    }

    private boolean isMonthlyRentTransaction(PublicApiResponseDto.Item item) {
        return parseAmount(item.getMonthlyRent()) > 0.0;
    }

    // 공통 그룹핑 및 계산 메서드
    private List<OfficetelMarketDataDto> groupByNeighborhoodAndCalculate(
            List<PublicApiResponseDto.Item> items,
            java.util.function.BiFunction<String, List<PublicApiResponseDto.Item>, OfficetelMarketDataDto> calculator) {

        Map<String, List<PublicApiResponseDto.Item>> groupedByNeighborhood =
                items.stream().collect(Collectors.groupingBy(PublicApiResponseDto.Item::getNeighborhood));

        return groupedByNeighborhood.entrySet().stream()
                .map(entry -> calculator.apply(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private OfficetelMarketDataDto calculateJeonseMarketData(String neighborhood, List<PublicApiResponseDto.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .filter(deposit -> deposit > 0)
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianDeposit = calculateMedian(deposits);
        String recentDate = findMostRecentDate(items);
        String district = extractDistrict(items);

        return new OfficetelMarketDataDto(
                neighborhood,
                district,
                roundToTwoDecimals(avgDeposit),
                0.0,  // 월세는 0
                roundToTwoDecimals(medianDeposit),
                0.0,  // 월세는 0
                items.size(),
                recentDate
        );
    }

    private OfficetelMarketDataDto calculateMonthlyRentMarketData(String neighborhood, List<PublicApiResponseDto.Item> items) {
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

        return new OfficetelMarketDataDto(
                neighborhood,
                district,
                roundToTwoDecimals(avgDeposit),
                roundToTwoDecimals(avgMonthlyRent),
                roundToTwoDecimals(medianDeposit),
                roundToTwoDecimals(medianMonthlyRent),
                items.size(),
                recentDate
        );
    }

    // 유틸리티 메서드들
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String findMostRecentDate(List<PublicApiResponseDto.Item> items) {
        return items.stream()
                .map(item -> String.format(DATE_FORMAT, item.getYear(), item.getMonth(), item.getDay()))
                .max(String::compareTo)
                .orElse("N/A");
    }

    private String extractDistrict(List<PublicApiResponseDto.Item> items) {
        return items.isEmpty() ? "N/A" :
                Optional.ofNullable(items.get(0).getDistrict()).orElse("N/A");
    }

    private double parseAmount(String amount) {
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

    private List<PublicApiResponseDto.Item> callApiAndParseXml(String lawdCd, String dealYmd) {
        URI uri = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("serviceKey", serviceKey)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("numOfRows", MAX_ROWS_PER_REQUEST)
                .build(true)
                .toUri();

        log.debug("API 요청 URL: {}", uri);

        try {
            String xmlResponse = restTemplate.getForObject(uri, String.class);
            if (xmlResponse != null) {
                PublicApiResponseDto responseDto = xmlMapper.readValue(xmlResponse, PublicApiResponseDto.class);
                if (responseDto != null && responseDto.getBody() != null && responseDto.getBody().getItems() != null) {
                    log.info("API 응답 성공 - 데이터 건수: {}", responseDto.getBody().getItems().getItemList().size());
                    return responseDto.getBody().getItems().getItemList();
                }
            }
        } catch (RestClientException e) {
            log.error("API 호출 실패 - URL: {}, 오류: {}", uri, e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("XML 파싱 실패 - URL: {}, 오류: {}", uri, e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생 - URL: {}, 오류: {}", uri, e.getMessage());
        }
        return Collections.emptyList();
    }

    private OfficetelTransactionDto convertToDto(PublicApiResponseDto.Item item) {
        String contractDate = String.format(DATE_FORMAT, item.getYear(), item.getMonth(), item.getDay());
        return new OfficetelTransactionDto(
                Optional.ofNullable(item.getBuildingName()).orElse("N/A").trim(),
                Optional.ofNullable(item.getDeposit()).orElse("0").trim(),
                Optional.ofNullable(item.getMonthlyRent()).orElse("0").trim(),
                String.valueOf(item.getArea()),
                contractDate,
                Optional.ofNullable(item.getFloor()).orElse("N/A").trim(),
                Optional.ofNullable(item.getBuildYear()).orElse("N/A").trim(),
                Optional.ofNullable(item.getContractType()).orElse("N/A").trim(),
                Optional.ofNullable(item.getContractTerm()).orElse("N/A").trim()
        );
    }
}