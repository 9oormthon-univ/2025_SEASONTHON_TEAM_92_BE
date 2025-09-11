package org.example.seasontonebackend.report.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.seasontonebackend.member.domain.Member;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 리포트 생성 시 사용자가 입력한 텍스트
    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;
}
