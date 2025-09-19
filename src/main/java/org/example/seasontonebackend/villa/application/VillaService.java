package org.example.seasontonebackend.villa.application;

import org.example.seasontonebackend.villa.dto.VillaMarketDataResponseDTO;
import org.example.seasontonebackend.villa.dto.VillaTransactionResponseDTO;

import java.util.List;
import java.util.Map;

public interface VillaService {
    Map<String, List<VillaTransactionResponseDTO>> getVillaRentData(String lawdCd);
    List<VillaMarketDataResponseDTO> getJeonseMarketData(String lawdCd);
    List<VillaMarketDataResponseDTO> getMonthlyRentMarketData(String lawdCd);
    Map<String, Object> getTimeSeriesAnalysis(String lawdCd, int months);
}