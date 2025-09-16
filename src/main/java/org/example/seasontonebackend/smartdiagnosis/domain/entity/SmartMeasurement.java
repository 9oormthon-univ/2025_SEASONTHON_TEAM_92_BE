package org.example.seasontonebackend.smartdiagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.seasontonebackend.member.domain.Member;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "smart_measurements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long measurementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeasurementType measurementType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal measuredValue;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(length = 255)
    private String locationInfo;

    @Column(length = 255)
    private String deviceInfo;

    private Integer measurementDuration;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum MeasurementType {
        NOISE, LEVEL, INTERNET
    }
}