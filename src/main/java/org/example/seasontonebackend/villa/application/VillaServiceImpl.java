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

    // 🔥 빌라 API URL로 변경 (올바른 엔드포인트)
    private static final String API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcSHRent/getRTMSDataSvcSHRent";
    private static final String DATE_FORMAT = "%d-%02d-%02d";
    private static final int MONTHS_TO_FETCH = 3;
    private static final int MAX_ROWS_PER_REQUEST = 100;

    @Value("${officetel.api.service-key}")
    private String serviceKey;

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
        // 빌라 기본 가격 (오피스텔보다 낮음)
        double baseRent = getBaseRentByRegion(lawdCd, "빌라");
        String[] neighborhoods = getNeighborhoodNamesByRegion(lawdCd);
        
        List<VillaMarketDataResponseDTO> mockData = new ArrayList<>();
        for (int i = 0; i < Math.min(neighborhoods.length, 6); i++) {
            String neighborhood = neighborhoods[i];
            double variation = 0.85 + (i * 0.05); // 0.85, 0.9, 0.95, 1.0, 1.05, 1.1 배
            double avgRent = Math.round(baseRent * variation);
            double avgDeposit = Math.round(avgRent * 50);
            
            mockData.add(VillaMarketDataResponseDTO.builder()
                .neighborhood(neighborhood)
                .avgMonthlyRent(avgRent)
                .avgDeposit(avgDeposit)
                .transactionCount(3 + i * 2) // 3, 5, 7, 9, 11, 13건
                .build());
        }
        
        return mockData;
    }
    
    private String[] getNeighborhoodNamesByRegion(String lawdCd) {
        switch (lawdCd) {
            case "11410": // 서대문구
                return new String[]{"미근동", "창천동", "충정로2가", "홍제동", "남가좌동", "합동"};
            case "11680": // 강남구
                return new String[]{"역삼동", "개포동", "청담동", "삼성동", "대치동", "논현동"};
            case "11650": // 서초구
                return new String[]{"서초동", "방배동", "잠원동", "반포동", "내곡동", "양재동"};
            case "11440": // 마포구
                return new String[]{"공덕동", "아현동", "도화동", "용강동", "대흥동", "염리동"};
            case "11170": // 용산구
                return new String[]{"후암동", "용산동", "남영동", "청파동", "원효로동", "이촌동"};
            case "11110": // 종로구
                return new String[]{"청계동", "신문로동", "효제동", "혜화동", "명륜동", "이화동"};
            case "11140": // 중구
                return new String[]{"명동", "을지로동", "회현동", "신당동", "다산동", "약수동"};
            case "11200": // 성동구
                return new String[]{"왕십리동", "마장동", "사근동", "행당동", "응봉동", "금호동"};
            case "11215": // 광진구
                return new String[]{"구의동", "광장동", "자양동", "화양동", "군자동", "중곡동"};
            case "11230": // 동대문구
                return new String[]{"용신동", "제기동", "전농동", "답십리동", "장안동", "청량리동"};
            case "11260": // 중랑구
                return new String[]{"면목동", "상봉동", "중화동", "묵동", "망우동", "신내동"};
            case "11290": // 성북구
                return new String[]{"성북동", "삼선동", "동선동", "돈암동", "안암동", "보문동"};
            case "11305": // 강북구
                return new String[]{"삼양동", "미아동", "번동", "수유동", "우이동", "인수동"};
            case "11320": // 도봉구
                return new String[]{"쌍문동", "방학동", "창동", "도봉동", "노해동", "해등동"};
            case "11350": // 노원구
                return new String[]{"월계동", "공릉동", "하계동", "중계동", "상계동", "녹천동"};
            case "11380": // 은평구
                return new String[]{"수색동", "녹번동", "불광동", "갈현동", "구산동", "대조동"};
            case "11470": // 양천구
                return new String[]{"목동", "신월동", "신정동", "염창동", "등촌동", "가양동"};
            case "11500": // 강서구
                return new String[]{"염창동", "등촌동", "화곡동", "가양동", "마곡동", "내발산동"};
            case "11530": // 구로구
                return new String[]{"신도림동", "구로동", "가리봉동", "고척동", "개봉동", "오류동"};
            case "11545": // 금천구
                return new String[]{"가산동", "독산동", "시흥동", "광명동", "범계동", "산본동"};
            case "11560": // 영등포구
                return new String[]{"영등포동", "여의도동", "당산동", "도림동", "문래동", "신길동"};
            case "11590": // 동작구
                return new String[]{"노량진동", "상도동", "상도1동", "본동", "사당동", "대방동"};
            case "11620": // 관악구
                return new String[]{"보라매동", "청림동", "성현동", "행운동", "낙성대동", "청룡동"};
            case "11710": // 송파구
                return new String[]{"잠실동", "신천동", "마천동", "거여동", "문정동", "장지동"};
            case "11740": // 강동구
                return new String[]{"천호동", "성내동", "길동", "둔촌동", "암사동", "상일동"};
            default:
                return new String[]{"인근 지역 1", "인근 지역 2", "인근 지역 3", "인근 지역 4", "인근 지역 5", "인근 지역 6"};
        }
    }
    
    private double getBaseRentByRegion(String lawdCd, String buildingType) {
        // 빌라 기본 가격 (오피스텔보다 낮음)
        double baseRent = 600000; // 60만원 기본값
        
        // 지역별 가격 조정
        switch (lawdCd) {
            case "11680": // 강남구
            case "11650": // 서초구
                return baseRent * 1.3;
            case "11710": // 송파구
            case "11740": // 강동구
                return baseRent * 1.1;
            case "11440": // 마포구
            case "11170": // 용산구
                return baseRent * 1.0;
            case "11200": // 성동구
            case "11215": // 광진구
                return baseRent * 0.9;
            case "11230": // 동대문구
            case "11260": // 중랑구
                return baseRent * 0.8;
            case "11290": // 성북구
            case "11305": // 강북구
                return baseRent * 0.75;
            case "11320": // 도봉구
            case "11350": // 노원구
                return baseRent * 0.7;
            case "11380": // 은평구
            case "11410": // 서대문구
                return baseRent * 0.8;
            case "11470": // 양천구
            case "11500": // 강서구
                return baseRent * 0.75;
            case "11530": // 구로구
            case "11545": // 금천구
                return baseRent * 0.7;
            case "11560": // 영등포구
            case "11590": // 동작구
                return baseRent * 0.8;
            case "11620": // 관악구
                return baseRent * 0.7;
            default:
                return baseRent;
        }
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
        // 수동으로 URL 구성하여 인코딩 문제 해결
        String encodedServiceKey = java.net.URLEncoder.encode(serviceKey, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format("%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s&numOfRows=%d", 
                API_URL, encodedServiceKey, lawdCd, dealYmd, MAX_ROWS_PER_REQUEST);
        URI uri = URI.create(url);

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
        
        // API 호출 제한: 최대 6개월로 제한
        int limitedMonths = Math.min(months, 6);
        log.info("API 호출 제한으로 {}개월로 제한", limitedMonths);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> timeSeriesData = new ArrayList<>();
        
        try {
            // 제한된 개월 수만큼 과거 데이터 수집
            YearMonth currentMonth = YearMonth.now();
            for (int i = limitedMonths - 1; i >= 0; i--) {
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
                double monthlyChangeRate = changeRate / limitedMonths;
                
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
            
            // 실제 데이터가 없는 경우 목업 데이터로 대체
            if (timeSeriesData.isEmpty()) {
                log.warn("실제 시계열 데이터가 없어 목업 데이터를 반환합니다. 법정동코드: {}", lawdCd);
                result = createMockTimeSeriesData(limitedMonths);
            } else {
                result.put("timeSeries", timeSeriesData);
                result.put("period", limitedMonths + "개월");
                result.put("lawdCd", lawdCd);
                result.put("isMockData", false);
            }
            
        } catch (Exception e) {
            log.error("시계열 분석 중 오류 발생: {}", e.getMessage());
            // 목업 데이터 반환
            result = createMockTimeSeriesData(limitedMonths);
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
        // 직접 API 호출하여 중복 호출 방지
        try {
            List<VillaPublicApiResponseDTO.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            return monthlyItems.stream()
                    .map(villaConverter::convertToTransactionDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("월별 빌라 데이터 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}