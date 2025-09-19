package org.example.seasontonebackend.villa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VillaTransactionResponseDTO {
    private String buildingName;
    private String deposit;
    private String monthlyRent;
    private String area;
    private String contractDate;
    private String floor;
    private String buildYear;
    private String contractType;
    private String contractTerm;
}