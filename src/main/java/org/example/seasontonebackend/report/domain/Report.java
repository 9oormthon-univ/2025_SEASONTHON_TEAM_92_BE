package org.example.seasontonebackend.report.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.seasontonebackend.member.domain.Member;

import java.util.UUID;

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

    // UUID를 사용한 공유 가능한 고유 식별자
    @Column(name = "public_id", unique = true, nullable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 리포트 생성 시 사용자가 입력한 텍스트
    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;
    
    // 리포트 타입 ('free' 또는 'premium')
    @Column(name = "report_type")
    private String reportType;

    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
    }
}
