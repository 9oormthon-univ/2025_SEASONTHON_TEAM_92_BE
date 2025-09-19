package org.example.seasontonebackend.mission.domain.repository;

import org.example.seasontonebackend.mission.domain.entity.WeeklyMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeeklyMissionRepository extends JpaRepository<WeeklyMission, Long> {

    @Query("SELECT m FROM WeeklyMission m WHERE m.isActive = true AND CURRENT_DATE BETWEEN m.startDate AND m.endDate")
    Optional<WeeklyMission> findCurrentActiveMission();

    @Query("SELECT m FROM WeeklyMission m LEFT JOIN FETCH m.questions WHERE m.isActive = true AND CURRENT_DATE BETWEEN m.startDate AND m.endDate")
    Optional<WeeklyMission> findCurrentActiveMissionWithQuestions();

    @Query("SELECT COUNT(DISTINCT r.member.id) FROM UserMissionResponse r WHERE r.mission.missionId = :missionId")
    Integer countParticipantsByMissionId(@Param("missionId") Long missionId);
}