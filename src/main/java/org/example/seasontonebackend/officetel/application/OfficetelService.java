package org.example.seasontonebackend.officetel.application;

import org.example.seasontonebackend.officetel.dto.OfficetelMarketDataResponseDTO;
import org.example.seasontonebackend.officetel.dto.OfficetelTransactionResponseDTO;

import java.util.List;
import java.util.Map;

public interface OfficetelService {
    Map<String, List<OfficetelTransactionResponseDTO>> getOfficetelRentData(String lawdCd);
    List<OfficetelMarketDataResponseDTO> getJeonseMarketData(String lawdCd);
    List<OfficetelMarketDataResponseDTO> getMonthlyRentMarketData(String lawdCd);
}