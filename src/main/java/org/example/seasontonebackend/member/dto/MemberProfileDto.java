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
    private String name;
    private String email;
    private String dong;
    private String building;
    private String buildingType;
    private String contractType;
    private Long security;
    private Integer rent;
    private Integer maintenanceFee;
    private boolean gpsVerified;
    private boolean contractVerified;
    private boolean onboardingCompleted;
}
