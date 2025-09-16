package org.example.seasontonebackend.smartdiagnosis.domain.repository;

import org.example.seasontonebackend.smartdiagnosis.domain.entity.LevelMeasurementDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LevelMeasurementDetailRepository extends JpaRepository<LevelMeasurementDetail, Long> {

    Optional<LevelMeasurementDetail> findByMeasurementMeasurementId(Long measurementId);
}