package org.example.seasontonebackend.member.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class MemberProfileDto {
    private String profileName;
    private String profileDong;
    private String profileBuilding;
    private String profileEmail;
}
