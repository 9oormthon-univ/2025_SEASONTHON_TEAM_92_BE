package org.example.seasontonebackend.diagnosis.domain.repository;

import org.example.seasontonebackend.diagnosis.domain.entity.DiagnosisResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiagnosisResponseRepository extends JpaRepository<DiagnosisResponse, Long> {
    List<DiagnosisResponse> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM DiagnosisResponse dr WHERE dr.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}