package org.example.seasontonebackend.smartdiagnosis.domain.repository;

import org.example.seasontonebackend.smartdiagnosis.domain.entity.InternetSpeedDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InternetSpeedDetailRepository extends JpaRepository<InternetSpeedDetail, Long> {
    Optional<InternetSpeedDetail> findByMeasurementMeasurementId(Long measurementId);
}