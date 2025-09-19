package org.example.seasontonebackend.smartdiagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "noise_data_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoiseDataPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dataPointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private SmartMeasurement measurement;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal decibel;

    @CreationTimestamp
    private LocalDateTime timestamp;
}