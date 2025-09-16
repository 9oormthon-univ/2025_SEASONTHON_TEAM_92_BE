package org.example.seasontonebackend.member.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModifyMemberProfileDto {
    private String profileName;
    private String profileDong;
    private String profileBuilding;
    private String profileEmail;

    public String dong;
    public String detailAddress;


}
