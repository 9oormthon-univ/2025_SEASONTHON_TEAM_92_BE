package org.example.seasontonebackend.smartdiagnosis.domain.repository;

import org.example.seasontonebackend.smartdiagnosis.domain.entity.NoiseMeasurementDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoiseMeasurementDetailRepository extends JpaRepository<NoiseMeasurementDetail, Long> {
    Optional<NoiseMeasurementDetail> findByMeasurementMeasurementId(Long measurementId);
}