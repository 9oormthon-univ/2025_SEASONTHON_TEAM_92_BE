package org.example.seasontonebackend.smartdiagnosis.domain.repository;

import org.example.seasontonebackend.smartdiagnosis.domain.entity.SmartMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartMeasurementRepository extends JpaRepository<SmartMeasurement, Long> {

    List<SmartMeasurement> findByMemberIdAndMeasurementTypeOrderByCreatedAtDesc(
            Long memberId, SmartMeasurement.MeasurementType measurementType);

    @Query("SELECT s FROM SmartMeasurement s WHERE s.member.id = :memberId " +
            "AND s.measurementType = :type ORDER BY s.createdAt DESC")
    List<SmartMeasurement> findRecentMeasurements(
            @Param("memberId") Long memberId,
            @Param("type") SmartMeasurement.MeasurementType type);
}