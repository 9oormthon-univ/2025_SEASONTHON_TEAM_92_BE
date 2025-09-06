package org.example.seasontonebackend.mission.domain.repository;

import org.example.seasontonebackend.mission.domain.entity.UserMissionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMissionResponseRepository extends JpaRepository<UserMissionResponse, Long> {

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM UserMissionResponse r WHERE r.member.id = :memberId AND r.mission.missionId = :missionId")
    Boolean existsByMemberIdAndMissionId(@Param("memberId") Long memberId, @Param("missionId") Long missionId);

    @Query("SELECT SUM(r.score) FROM UserMissionResponse r WHERE r.member.id = :memberId AND r.mission.missionId = :missionId")
    Integer getTotalScoreByMemberAndMission(@Param("memberId") Long memberId, @Param("missionId") Long missionId);
}