package org.example.seasontonebackend.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDongBuildingRequestDto {
    public String dong;
    public String detailAddress;
    public String building;
    public String buildingType;
    public String contractType;
    public Long security; // 보증금
    public Integer rent; // 월세
    public Integer maintenanceFee; // 관리비

}
