package org.example.seasontonebackend.member.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberLoginDto {
    private String email;
    private String password;
}
