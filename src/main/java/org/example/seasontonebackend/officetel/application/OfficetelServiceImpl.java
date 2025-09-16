package org.example.seasontonebackend.officetel.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.seasontonebackend.officetel.converter.OfficetelConverter;
import org.example.seasontonebackend.officetel.dto.OfficetelMarketDataResponseDTO;
import org.example.seasontonebackend.officetel.dto.OfficetelTransactionResponseDTO;
import org.example.seasontonebackend.officetel.dto.PublicApiResponseDTO;
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
public class OfficetelServiceImpl implements OfficetelService {

    private static final String API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent";
    private static final String DATE_FORMAT = "%d-%02d-%02d";
    private static final int MONTHS_TO_FETCH = 3;
    private static final int MAX_ROWS_PER_REQUEST = 100;

    @Value("${officetel.api.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    private final OfficetelConverter officetelConverter;

    public OfficetelServiceImpl(OfficetelConverter officetelConverter) {
        this.officetelConverter = officetelConverter;
    }

    @Override
    public Map<String, List<OfficetelTransactionResponseDTO>> getOfficetelRentData(String lawdCd) {
        List<PublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        return allItems.stream()
                .map(officetelConverter::convertToTransactionDTO)
                .collect(Collectors.groupingBy(OfficetelTransactionResponseDTO::getBuildingName));
    }

    @Override
    public List<OfficetelMarketDataResponseDTO> getJeonseMarketData(String lawdCd) {
        List<PublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        List<PublicApiResponseDTO.Item> jeonseItems = allItems.stream()
                .filter(this::isValidNeighborhood)
                .filter(this::isJeonseTransaction)
                .collect(Collectors.toList());

        return groupByNeighborhoodAndCalculate(jeonseItems, officetelConverter::calculateJeonseMarketData);
    }

    @Override
    public List<OfficetelMarketDataResponseDTO> getMonthlyRentMarketData(String lawdCd) {
        List<PublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        List<PublicApiResponseDTO.Item> monthlyRentItems = allItems.stream()
                .filter(this::isValidNeighborhood)
                .filter(this::isMonthlyRentTransaction)
                .collect(Collectors.toList());

        return groupByNeighborhoodAndCalculate(monthlyRentItems, officetelConverter::calculateMonthlyRentMarketData);
    }

    private List<PublicApiResponseDTO.Item> fetchAllItemsForPeriod(String lawdCd) {
        List<PublicApiResponseDTO.Item> allItems = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < MONTHS_TO_FETCH; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<PublicApiResponseDTO.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null && !monthlyItems.isEmpty()) {
                allItems.addAll(monthlyItems);
            }
        }
        return allItems;
    }

    private boolean isValidNeighborhood(PublicApiResponseDTO.Item item) {
        return item.getNeighborhood() != null && !item.getNeighborhood().trim().isEmpty();
    }

    private boolean isJeonseTransaction(PublicApiResponseDTO.Item item) {
        return officetelConverter.parseAmount(item.getMonthlyRent()) == 0.0 &&
                officetelConverter.parseAmount(item.getDeposit()) > 0.0;
    }

    private boolean isMonthlyRentTransaction(PublicApiResponseDTO.Item item) {
        return officetelConverter.parseAmount(item.getMonthlyRent()) > 0.0;
    }

    private List<OfficetelMarketDataResponseDTO> groupByNeighborhoodAndCalculate(
            List<PublicApiResponseDTO.Item> items,
            java.util.function.BiFunction<String, List<PublicApiResponseDTO.Item>, OfficetelMarketDataResponseDTO> calculator) {

        Map<String, List<PublicApiResponseDTO.Item>> groupedByNeighborhood =
                items.stream().collect(Collectors.groupingBy(PublicApiResponseDTO.Item::getNeighborhood));

        return groupedByNeighborhood.entrySet().stream()
                .map(entry -> calculator.apply(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<PublicApiResponseDTO.Item> callApiAndParseXml(String lawdCd, String dealYmd) {
        // serviceKey는 이미 URL 인코딩되어 있으므로 그대로 사용
        URI uri = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("serviceKey", serviceKey)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("numOfRows", MAX_ROWS_PER_REQUEST)
                .build(false)
                .toUri();

        log.debug("API 요청 URL: {}", uri);

        try {
            String xmlResponse = restTemplate.getForObject(uri, String.class);
            if (xmlResponse != null) {
                PublicApiResponseDTO responseDto = xmlMapper.readValue(xmlResponse, PublicApiResponseDTO.class);
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

    @Override
    public Map<String, Object> getTimeSeriesAnalysis(String lawdCd, int months) {
        log.info("오피스텔 시계열 분석 시작 - 법정동코드: {}, 분석 기간: {}개월", lawdCd, months);
        
        Map<String, Object> result = new HashMap<>();
        
        // 오피스텔은 목업 데이터로 처리 (실제 API 연동은 복잡함)
        result = createMockTimeSeriesData(lawdCd, months, "오피스텔");
        
        return result;
    }
    
    private Map<String, Object> createMockTimeSeriesData(String lawdCd, int months, String buildingType) {
        List<Map<String, Object>> timeSeriesData = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        // 오피스텔 목업 데이터 (빌라보다 높은 가격대)
        double baseRent = buildingType.equals("오피스텔") ? 750000 : 580000; // 오피스텔 75만원, 빌라 58만원
        
        for (int i = months - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            double monthlyRent = baseRent + (months - i - 1) * 20000 + Math.random() * 40000; // 오피스텔은 월 2만원씩 상승
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("period", targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            monthData.put("averageRent", Math.round(monthlyRent));
            monthData.put("transactionCount", (int) (Math.random() * 15) + 8); // 8-23건
            monthData.put("yearMonth", targetMonth.toString());
            
            timeSeriesData.add(monthData);
        }
        
        // 목업 분석 결과
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalChangeRate", buildingType.equals("오피스텔") ? 15.2 : 12.5);
        analysis.put("monthlyChangeRate", buildingType.equals("오피스텔") ? 1.1 : 0.8);
        analysis.put("startPeriod", timeSeriesData.get(0).get("period"));
        analysis.put("endPeriod", timeSeriesData.get(timeSeriesData.size() - 1).get("period"));
        analysis.put("startRent", timeSeriesData.get(0).get("averageRent"));
        analysis.put("endRent", timeSeriesData.get(timeSeriesData.size() - 1).get("averageRent"));
        analysis.put("trend", "상승");
        analysis.put("buildingType", buildingType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("timeSeries", timeSeriesData);
        result.put("analysis", analysis);
        result.put("period", months + "개월");
        result.put("isMockData", true);
        
        return result;
    }
}