package org.example.seasontonebackend.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreateDto {
    private String email;
    private String name;
    private String password;
    // 나머지 필드들은 프로필 설정에서 입력받도록 변경
    // private String dong;
    // private String building;
    // private String buildingType;
    // private String contractType;
    // private Long security;
    // private Integer rent;
    // private Integer maintenanceFee;
    // private boolean isGpsVerified;
}