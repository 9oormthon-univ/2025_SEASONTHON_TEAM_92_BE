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
        List<VillaPublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        return allItems.stream()
                .map(villaConverter::convertToTransactionDTO)
                .collect(Collectors.groupingBy(VillaTransactionResponseDTO::getBuildingName));
    }

    @Override
    public List<VillaMarketDataResponseDTO> getJeonseMarketData(String lawdCd) {
        List<VillaPublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        List<VillaPublicApiResponseDTO.Item> jeonseItems = allItems.stream()
                .filter(this::isValidNeighborhood)
                .filter(this::isJeonseTransaction)
                .collect(Collectors.toList());

        return groupByNeighborhoodAndCalculate(jeonseItems, villaConverter::calculateJeonseMarketData);
    }

    @Override
    public List<VillaMarketDataResponseDTO> getMonthlyRentMarketData(String lawdCd) {
        List<VillaPublicApiResponseDTO.Item> allItems = fetchAllItemsForPeriod(lawdCd);

        List<VillaPublicApiResponseDTO.Item> monthlyRentItems = allItems.stream()
                .filter(this::isValidNeighborhood)
                .filter(this::isMonthlyRentTransaction)
                .collect(Collectors.toList());

        return groupByNeighborhoodAndCalculate(monthlyRentItems, villaConverter::calculateMonthlyRentMarketData);
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
}