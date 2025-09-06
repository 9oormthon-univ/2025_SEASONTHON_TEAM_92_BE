package org.example.seasontonebackend.diagnosis.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.example.seasontonebackend.diagnosis.domain.entity.Diagnosis;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    Optional<Diagnosis> findByMemberId(Long memberId);

    @Query("""
        SELECT d FROM Diagnosis d 
        JOIN Member m ON d.memberId = m.id 
        WHERE m.building = :building
        """)
    List<Diagnosis> findByBuilding(@Param("building") String building);

    @Query("""
        SELECT d FROM Diagnosis d 
        JOIN Member m ON d.memberId = m.id 
        WHERE m.dong = :dong
        """)
    List<Diagnosis> findByDong(@Param("dong") String dong);

    @Query("""
        SELECT AVG(d.totalScore) FROM Diagnosis d 
        JOIN Member m ON d.memberId = m.id 
        WHERE m.building = :building
        """)
    Double getAverageScoreByBuilding(@Param("building") String building);

    @Query("""
        SELECT AVG(d.totalScore) FROM Diagnosis d 
        JOIN Member m ON d.memberId = m.id 
        WHERE m.dong = :dong
        """)
    Double getAverageScoreByDong(@Param("dong") String dong);

    @Query("""
        SELECT COUNT(d) + 1 FROM Diagnosis d 
        JOIN Member m ON d.memberId = m.id 
        WHERE m.building = :building AND d.totalScore > :score
        """)
    Integer getBuildingRank(@Param("building") String building, @Param("score") Integer score);

    @Query("""
        SELECT COUNT(d) + 1 FROM Diagnosis d 
        JOIN Member m ON d.memberId = m.id 
        WHERE m.dong = :dong AND d.totalScore > :score
        """)
    Integer getDongRank(@Param("dong") String dong, @Param("score") Integer score);
}