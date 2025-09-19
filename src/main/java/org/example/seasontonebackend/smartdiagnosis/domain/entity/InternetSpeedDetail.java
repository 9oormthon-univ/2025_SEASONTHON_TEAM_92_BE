package org.example.seasontonebackend.smartdiagnosis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "internet_speed_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternetSpeedDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private SmartMeasurement measurement;

    @Column(precision = 8, scale = 2)
    private BigDecimal downloadSpeed;

    @Column(precision = 8, scale = 2)
    private BigDecimal uploadSpeed;

    @Column(precision = 6, scale = 2)
    private BigDecimal ping;

    @Column(length = 50)
    private String connectionType;

    @Column(length = 100)
    private String serverLocation;

    @Column(length = 50)
    private String speedGrade;

    @Column(length = 255)
    private String comparison;

    @CreationTimestamp
    private LocalDateTime createdAt;
}