package org.example.seasontonebackend.mission.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.example.seasontonebackend.mission.domain.entity.Mission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    // 활성 미션 조회 (만료되지 않은)
    List<Mission> findByIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now);

    // 모든 미션 조회 (관리자용)
    List<Mission> findAllByOrderByCreatedAtDesc();

    // 유효한 미션 조회 (참여 가능한)
    Optional<Mission> findByIdAndIsActiveTrueAndExpiresAtAfter(Long missionId, LocalDateTime now);
}