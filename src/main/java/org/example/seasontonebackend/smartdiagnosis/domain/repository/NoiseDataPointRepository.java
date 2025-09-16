package org.example.seasontonebackend.smartdiagnosis.domain.repository;

import org.example.seasontonebackend.smartdiagnosis.domain.entity.NoiseDataPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoiseDataPointRepository extends JpaRepository<NoiseDataPoint, Long> {
    List<NoiseDataPoint> findByMeasurementMeasurementIdOrderByTimestamp(Long measurementId);
}