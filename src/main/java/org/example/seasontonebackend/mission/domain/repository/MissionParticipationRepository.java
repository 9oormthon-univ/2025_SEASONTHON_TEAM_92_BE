package org.example.seasontonebackend.mission.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.example.seasontonebackend.mission.domain.entity.MissionParticipation;

import java.util.List;

@Repository
public interface MissionParticipationRepository extends JpaRepository<MissionParticipation, Long> {

    // 중복 참여 확인
    boolean existsByMemberIdAndMissionId(Long memberId, Long missionId);

    // 미션별 총 참여자 수
    @Query("SELECT COUNT(DISTINCT mp.memberId) FROM MissionParticipation mp WHERE mp.missionId = :missionId")
    Integer countParticipantsByMissionId(@Param("missionId") Long missionId);

    // 특정 미션의 모든 참여 기록
    List<MissionParticipation> findByMissionId(Long missionId);

    // 사용자의 참여한 미션 ID 목록
    @Query("SELECT DISTINCT mp.missionId FROM MissionParticipation mp WHERE mp.memberId = :memberId")
    List<Long> findDistinctMissionIdsByMemberId(@Param("memberId") Long memberId);
}