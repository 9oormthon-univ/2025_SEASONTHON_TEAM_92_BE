package org.example.seasontonebackend.smartdiagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "level_measurement_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelMeasurementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private SmartMeasurement measurement;

    @Column(precision = 8, scale = 4)
    private BigDecimal xAxis;

    @Column(precision = 8, scale = 4)
    private BigDecimal yAxis;

    @Column(precision = 8, scale = 4)
    private BigDecimal zAxis;

    @Column(precision = 8, scale = 4)
    private BigDecimal totalTilt;

    @Column(nullable = false)
    private Boolean isLevel;

    @Column(length = 50)
    private String levelStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;
}