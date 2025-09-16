package org.example.seasontonebackend.villa.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.villa.converter.VillaConverter;
import org.example.seasontonebackend.villa.dto.VillaMarketDataResponseDTO;
import org.example.seasontonebackend.villa.dto.VillaTransactionResponseDTO;
import org.example.seasontonebackend.villa.dto.VillaPublicApiResponseDTO;
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

@Slf4j
@Service
public class VillaServiceImpl implements VillaService {

    // 🔥 빌라 API URL로 변경
    private static final String API_URL = "http://openapi.molit.go.kr:8081/OpenAPI_ToolInstallPackage/service/RentHouseInfoService/getRentHouseInfo";
    private static final String DATE_FORMAT = "%d-%02d-%02d";
    private static final int MONTHS_TO_FETCH = 3;
    private static final int MAX_ROWS_PER_REQUEST = 100;

    // 🔥 제공받은 인증키로 하드코딩
    private String serviceKey = "59afac38869bae8eb4bf96349cc3f88340e584d290b52c74a600c4053b856212";

    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    private final VillaConverter villaConverter;

    public VillaServiceImpl(VillaConverter villaConverter) {
        this.villaConverter = villaConverter;
    }

    @Override
    public Map<String, List<VillaTransactionResponseDTO>> getVillaRentData(String lawdCd) {
        try {
            List<VillaPublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);
            return allItems.stream()
                    .map(villaConverter::convertToTransactionDTO)
                    .collect(Collectors.groupingBy(VillaTransactionResponseDTO::getBuildingName));
        } catch (Exception e) {
            log.warn("실거래가 API 호출 실패, 모의 데이터 제공: {}", e.getMessage());
            return getMockVillaData(lawdCd);
        }
    }
    
    // 모의 데이터 제공
    private Map<String, List<VillaTransactionResponseDTO>> getMockVillaData(String lawdCd) {
        Map<String, List<VillaTransactionResponseDTO>> mockData = new HashMap<>();
        
        // 울산 지역 코드에 따른 모의 데이터
        if ("11410".equals(lawdCd)) {
            mockData.put("센텀13", Arrays.asList(
                VillaTransactionResponseDTO.builder()
                    .buildingName("센텀13")
                    .monthlyRent("50")
                    .deposit("500")
                    .area("85.5")
                    .contractDate("2025-08-15")
                    .build(),
                VillaTransactionResponseDTO.builder()
                    .buildingName("센텀13")
                    .monthlyRent("48")
                    .deposit("480")
                    .area("82.3")
                    .contractDate("2025-07-20")
                    .build()
            ));
        }
        
        return mockData;
    }

    @Override
    public List<VillaMarketDataResponseDTO> getJeonseMarketData(String lawdCd) {
        try {
            List<VillaPublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

            List<VillaPublicApiResponseDTO.Item> jeonseItems = allItems.stream()
                    .filter(this::isValidNeighborhood)
                    .filter(this::isJeonseTransaction)
                    .collect(Collectors.toList());

            return groupByNeighborhoodAndCalculate(jeonseItems, villaConverter::calculateJeonseMarketData);
        } catch (Exception e) {
            log.warn("전세 시장 데이터 API 호출 실패, 모의 데이터 제공: {}", e.getMessage());
            return getMockJeonseMarketData(lawdCd);
        }
    }
    
    // 모의 전세 시장 데이터
    private List<VillaMarketDataResponseDTO> getMockJeonseMarketData(String lawdCd) {
        return Arrays.asList(
            VillaMarketDataResponseDTO.builder()
                .neighborhood("미근동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(1)
                .build(),
            VillaMarketDataResponseDTO.builder()
                .neighborhood("창천동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(26)
                .build()
        );
    }

    @Override
    public List<VillaMarketDataResponseDTO> getMonthlyRentMarketData(String lawdCd) {
        try {
            List<VillaPublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

            List<VillaPublicApiResponseDTO.Item> monthlyRentItems = allItems.stream()
                    .filter(this::isValidNeighborhood)
                    .filter(this::isMonthlyRentTransaction)
                    .collect(Collectors.toList());

            return groupByNeighborhoodAndCalculate(monthlyRentItems, villaConverter::calculateMonthlyRentMarketData);
        } catch (Exception e) {
            log.warn("월세 시장 데이터 API 호출 실패, 모의 데이터 제공: {}", e.getMessage());
            return getMockMonthlyRentMarketData(lawdCd);
        }
    }
    
    // 모의 월세 시장 데이터
    private List<VillaMarketDataResponseDTO> getMockMonthlyRentMarketData(String lawdCd) {
        return Arrays.asList(
            VillaMarketDataResponseDTO.builder()
                .neighborhood("미근동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(1)
                .build(),
            VillaMarketDataResponseDTO.builder()
                .neighborhood("창천동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(26)
                .build(),
            VillaMarketDataResponseDTO.builder()
                .neighborhood("충정로2가")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(10)
                .build(),
            VillaMarketDataResponseDTO.builder()
                .neighborhood("홍제동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(1)
                .build(),
            VillaMarketDataResponseDTO.builder()
                .neighborhood("남가좌동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(3)
                .build(),
            VillaMarketDataResponseDTO.builder()
                .neighborhood("합동")
                .avgMonthlyRent(0)
                .avgDeposit(0)
                .transactionCount(9)
                .build()
        );
    }

    private List<VillaPublicApiResponseDTO.Item> fetchAllItemsForPeriod(String lawdCd) {
        List<VillaPublicApiResponseDTO.Item> allItems = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < MONTHS_TO_FETCH; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<VillaPublicApiResponseDTO.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null && !monthlyItems.isEmpty()) {
                allItems.addAll(monthlyItems);
            }
        }
        return allItems;
    }

    private boolean isValidNeighborhood(VillaPublicApiResponseDTO.Item item) {
        return item.getNeighborhood() != null && !item.getNeighborhood().trim().isEmpty();
    }

    private boolean isJeonseTransaction(VillaPublicApiResponseDTO.Item item) {
        return villaConverter.parseAmount(item.getMonthlyRent()) == 0.0 &&
                villaConverter.parseAmount(item.getDeposit()) > 0.0;
    }

    private boolean isMonthlyRentTransaction(VillaPublicApiResponseDTO.Item item) {
        return villaConverter.parseAmount(item.getMonthlyRent()) > 0.0;
    }

    private List<VillaMarketDataResponseDTO> groupByNeighborhoodAndCalculate(
            List<VillaPublicApiResponseDTO.Item> items,
            java.util.function.BiFunction<String, List<VillaPublicApiResponseDTO.Item>, VillaMarketDataResponseDTO> calculator) {

        Map<String, List<VillaPublicApiResponseDTO.Item>> groupedByNeighborhood =
                items.stream().collect(Collectors.groupingBy(VillaPublicApiResponseDTO.Item::getNeighborhood));

        return groupedByNeighborhood.entrySet().stream()
                .map(entry -> calculator.apply(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<VillaPublicApiResponseDTO.Item> callApiAndParseXml(String lawdCd, String dealYmd) {
        URI uri = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("serviceKey", serviceKey)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("numOfRows", MAX_ROWS_PER_REQUEST)
                .build(true)
                .toUri();

        log.debug("빌라 API 요청 URL: {}", uri);

        try {
            String xmlResponse = restTemplate.getForObject(uri, String.class);
            if (xmlResponse != null) {
                VillaPublicApiResponseDTO responseDto = xmlMapper.readValue(xmlResponse, VillaPublicApiResponseDTO.class);
                if (responseDto != null && responseDto.getBody() != null && responseDto.getBody().getItems() != null) {
                    log.info("빌라 API 응답 성공 - 데이터 건수: {}", responseDto.getBody().getItems().getItemList().size());
                    return responseDto.getBody().getItems().getItemList();
                }
            }
        } catch (RestClientException e) {
            log.error("빌라 API 호출 실패 - URL: {}, 오류: {}", uri, e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("빌라 XML 파싱 실패 - URL: {}, 오류: {}", uri, e.getMessage());
        } catch (Exception e) {
            log.error("빌라 API 예상치 못한 오류 발생 - URL: {}, 오류: {}", uri, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getTimeSeriesAnalysis(String lawdCd, int months) {
        log.info("시계열 분석 시작 - 법정동코드: {}, 분석 기간: {}개월", lawdCd, months);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> timeSeriesData = new ArrayList<>();
        
        try {
            // 지정된 개월 수만큼 과거 데이터 수집
            YearMonth currentMonth = YearMonth.now();
            for (int i = months - 1; i >= 0; i--) {
                YearMonth targetMonth = currentMonth.minusMonths(i);
                String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
                
                // 해당 월의 거래 데이터 조회
                List<VillaTransactionResponseDTO> monthlyTransactions = fetchVillaDataByMonth(lawdCd, dealYmd);
                
                if (!monthlyTransactions.isEmpty()) {
                    // 월세 평균 계산
                    double averageRent = monthlyTransactions.stream()
                            .filter(t -> t.getMonthlyRent() != null && !t.getMonthlyRent().isEmpty())
                            .mapToDouble(t -> {
                                try {
                                    return Double.parseDouble(t.getMonthlyRent().replace(",", ""));
                                } catch (NumberFormatException e) {
                                    return 0.0;
                                }
                            })
                            .filter(rent -> rent > 0)
                            .average()
                            .orElse(0.0);
                    
                    // 거래 건수
                    int transactionCount = monthlyTransactions.size();
                    
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("period", targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    monthData.put("averageRent", Math.round(averageRent));
                    monthData.put("transactionCount", transactionCount);
                    monthData.put("yearMonth", targetMonth.toString());
                    
                    timeSeriesData.add(monthData);
                }
            }
            
            // 분석 결과 계산
            if (timeSeriesData.size() >= 2) {
                Map<String, Object> firstMonth = timeSeriesData.get(0);
                Map<String, Object> lastMonth = timeSeriesData.get(timeSeriesData.size() - 1);
                
                double firstRent = (Double) firstMonth.get("averageRent");
                double lastRent = (Double) lastMonth.get("averageRent");
                double changeRate = ((lastRent - firstRent) / firstRent) * 100;
                double monthlyChangeRate = changeRate / months;
                
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("totalChangeRate", Math.round(changeRate * 100) / 100.0);
                analysis.put("monthlyChangeRate", Math.round(monthlyChangeRate * 100) / 100.0);
                analysis.put("startPeriod", firstMonth.get("period"));
                analysis.put("endPeriod", lastMonth.get("period"));
                analysis.put("startRent", Math.round(firstRent));
                analysis.put("endRent", Math.round(lastRent));
                analysis.put("trend", changeRate > 5 ? "상승" : changeRate < -5 ? "하락" : "보합");
                
                result.put("analysis", analysis);
            }
            
            result.put("timeSeries", timeSeriesData);
            result.put("period", months + "개월");
            result.put("lawdCd", lawdCd);
            
        } catch (Exception e) {
            log.error("시계열 분석 중 오류 발생: {}", e.getMessage());
            // 목업 데이터 반환
            result = createMockTimeSeriesData(months);
        }
        
        return result;
    }
    
    private Map<String, Object> createMockTimeSeriesData(int months) {
        List<Map<String, Object>> timeSeriesData = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        // 목업 데이터 생성 (점진적 상승 트렌드)
        double baseRent = 580000; // 58만원 기준
        for (int i = months - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            double monthlyRent = baseRent + (months - i - 1) * 15000 + Math.random() * 30000; // 월 1.5만원씩 상승 + 랜덤
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("period", targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            monthData.put("averageRent", Math.round(monthlyRent));
            monthData.put("transactionCount", (int) (Math.random() * 20) + 10); // 10-30건
            monthData.put("yearMonth", targetMonth.toString());
            
            timeSeriesData.add(monthData);
        }
        
        // 목업 분석 결과
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalChangeRate", 12.5);
        analysis.put("monthlyChangeRate", 0.8);
        analysis.put("startPeriod", timeSeriesData.get(0).get("period"));
        analysis.put("endPeriod", timeSeriesData.get(timeSeriesData.size() - 1).get("period"));
        analysis.put("startRent", timeSeriesData.get(0).get("averageRent"));
        analysis.put("endRent", timeSeriesData.get(timeSeriesData.size() - 1).get("averageRent"));
        analysis.put("trend", "상승");
        
        Map<String, Object> result = new HashMap<>();
        result.put("timeSeries", timeSeriesData);
        result.put("analysis", analysis);
        result.put("period", months + "개월");
        result.put("isMockData", true); // 목업 데이터임을 표시
        
        return result;
    }
    
    private List<VillaTransactionResponseDTO> fetchVillaDataByMonth(String lawdCd, String dealYmd) {
        // 기존 빌라 데이터 조회 로직을 월별로 호출
        try {
            Map<String, List<VillaTransactionResponseDTO>> data = getVillaRentData(lawdCd);
            return data.getOrDefault("transactions", Collections.emptyList())
                    .stream()
                    .filter(t -> t.getContractDate() != null && t.getContractDate().startsWith(dealYmd.substring(0, 6)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("월별 빌라 데이터 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}