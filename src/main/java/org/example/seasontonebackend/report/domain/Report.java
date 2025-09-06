package org.example.seasontonebackend.report.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "primary_negotiation_card")
    private String primaryNegotiationCard;

//    @Column(name = "primary_negotiation_card_2")
//    private String primaryNegotiationCard2;

    @Column(name = "secondary_negotiation_card")
    private String secondaryNegotiationCard;

//    @Column(name = "secondary_negotiation_card_2")
//    private String secondaryNegotiationCard2;

    @Column(name = "step_1")
    private String step1;

    @Column(name = "step_2")
    private String step2;

    @Column(name = "member_id")
    private Long memberId;
}
