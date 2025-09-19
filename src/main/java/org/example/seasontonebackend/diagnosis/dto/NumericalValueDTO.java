package org.example.seasontonebackend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// NumericalValueDTO.java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumericalValueDTO {
    private String value;
    private String unit;
    private String comparison;
}
