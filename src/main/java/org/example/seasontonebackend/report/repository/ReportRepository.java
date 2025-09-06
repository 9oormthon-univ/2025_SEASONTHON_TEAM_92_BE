package org.example.seasontonebackend.report.repository;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.report.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByReportId(Long reportId);
}
