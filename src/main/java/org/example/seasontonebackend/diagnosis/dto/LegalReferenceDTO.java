package org.example.seasontonebackend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// LegalReferenceDTO.java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalReferenceDTO {
    private String primary;
    private String secondary;
}
