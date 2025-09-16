package org.example.seasontonebackend.smartdiagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "noise_measurement_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoiseMeasurementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private SmartMeasurement measurement;

    @Column(precision = 5, scale = 2)
    private BigDecimal avgDecibel;

    @Column(precision = 5, scale = 2)
    private BigDecimal minDecibel;

    @Column(precision = 5, scale = 2)
    private BigDecimal maxDecibel;

    @Column(length = 50)
    private String category;

    @Column(length = 255)
    private String comparisonText;

    @CreationTimestamp
    private LocalDateTime createdAt;
}