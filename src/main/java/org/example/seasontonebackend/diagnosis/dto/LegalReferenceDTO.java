package org.example.seasontonebackend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalReferenceDTO {
    private String primary;
    private String secondary;
}