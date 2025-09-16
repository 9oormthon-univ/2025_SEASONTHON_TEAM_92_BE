package org.example.seasontonebackend.report.service;

import org.example.seasontonebackend.member.domain.Member;
import org.example.seasontonebackend.report.ai.AI;
import org.example.seasontonebackend.report.domain.Report;
import org.example.seasontonebackend.report.dto.ReportRequestDto;
import org.example.seasontonebackend.report.dto.ReportResponseDto;
import org.example.seasontonebackend.report.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReportService {
    private final AI ai;
    private final ReportRepository reportRepository;

    public ReportService(AI ai, ReportRepository reportRepository) {
        this.ai = ai;
        this.reportRepository = reportRepository;
    }


    public Long createReport(ReportRequestDto reportRequestDto, Member member) {
        String promptPrimaryNegotiationCard = "사용자의 요구사항: " + reportRequestDto.getReportContent()  + " 프롬프트: 당신은 세입자용 참고용 제안서를 작성하는 AI입니다. 세입자는 재계약을 앞주고 있으며 이 서비스는 세입자가 집주인에게 어떻게 요청할 수 있을지 미리 준비할 수 있도록 돕는 플랫폼입니다. 사용자의 요구사항만 기반으로 작성하며, 절대 지어내지 마세요. 작성 톤은 집주인에게 이렇게 제안하면 어떨까요? 형태로 작성하고 시작해야해. 무조건 그래야 합니다 무조건 제발요 부탁 드릴게요. 시설 관련 내용는 포함하지마 단 절대 고칠 수 없는 하자의 경우에는 월세 조정의 이유로 잡아도 돼 고치는게 가능한 오류는 절대 안돼, 월세 조정 요청만 작성합니다. 서두에 이 서비스에서 제안서를 만드는 이유를 간단히 언급은 절대 하면 안됩니다, 월세 조정 요청 사항을 구체적으로 작성하며, 마지막에 상호 합의 가능성을 언급합니다. 출력은 반드시 월세 조정 요청 사항만 2줄로 작성하며, AI 역할이나 서비스 안내 문장은 절대 포함하지 마세요, 줄넘김 절대 금지야 하면 죽어. 민약 조정하기 어려운 경우에는 현재 사유가 명확하지 않은 경우에만 하향을 기대하기 어려운 상황에는 올리지 말아달라고 제안하는 글을 작성해";
//        String promptPrimaryNegotiationCard2 = "";
        String promptSecondaryNegotiationCard1 =  "사용자의 요구사항: " + reportRequestDto.getReportContent()  + " 프롬프트: 당신은 세입자용 참고용 제안서를 작성하는 AI입니다. 이 서비스는 세입자가 집주인에게 어떻게 요청할 수 있을지 미리 준비할 수 있도록 돕는 플랫폼입니다. 사용자의 요구사항만 기반으로 작성하며, 절대 지어내지 마세요. 작성 톤은 집주인에게 이렇게 제안하면 어떨까요? 형태로 작성하고 시작해야해. 무조건 그래야 합니다 무조건 제발요 부탁 드릴게요. 시설 수리 내용을 포함해야해. 하자 수리 요청만 작성합니다. 서두에 이 서비스에서 제안서를 만드는 이유를 간단히 언급은 절대 하면 안됩니다, 수리 요청을 구체적으로 작성하며, 마지막에 상호 합의 가능성을 언급합니다. 출력은 반드시 하자 수리 요청 사항만 2줄로 작성하며, AI 역할이나 서비스 안내 문장은 절대 포함하지 마세요, 줄넘김 절대 금지야 하면 죽어 제발 하지마 /n 같은거 쓰지마.";
//        String promptSecondaryNegotiationCard2 = "";
//        String promptStep1 = promptPrimaryNegotiationCard + " 이 글의 핵심 단어 하나만 출력해. 문장 금지, 20자 이내, 다른 말 금지.";
//        String promptStep2 = promptSecondaryNegotiationCard1 + " 이 글의 핵심 단어 하나만 출력해. 문장 금지, 20자 이내, 다른 말 금지.";


        Report report = Report.builder()
                .primaryNegotiationCard(ai.getGeminiResponse(promptPrimaryNegotiationCard))
//                .primaryNegotiationCard2(ai.getGeminiResponse(promptPrimaryNegotiationCard2))
                .secondaryNegotiationCard(ai.getGeminiResponse(promptSecondaryNegotiationCard1))
//                .secondaryNegotiationCard2(ai.getGeminiResponse(promptPrimaryNegotiationCard2))
//                .step1(null)
//                .step2(null)
                .reportId(member.getId())
                .build();

        reportRepository.save(report);

        return report.getReportId();
    }

    public ReportResponseDto getReport(Long reportId) {
        Report report = reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new NullPointerException("존재하지 않는 리포트입니다!"));

        ReportResponseDto reportResponseDto = ReportResponseDto.builder()
                .primaryNegotiationCard(report.getPrimaryNegotiationCard())
//                .primaryNegotiationCard2(report.getPrimaryNegotiationCard())
                .secondaryNegotiationCard(report.getSecondaryNegotiationCard())
//                .secondaryNegotiationCard2(report.getSecondaryNegotiationCard2())
//                .step1(report.getStep1())
//                .step2(report.getStep2())
                .build();
        return reportResponseDto;
    }

    public Long createPremiumReport(ReportRequestDto reportRequestDto, Member member) {
    }
}
