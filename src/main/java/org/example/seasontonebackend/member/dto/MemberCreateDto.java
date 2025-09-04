package org.example.seasontonebackend.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberCreateDto {
    private String email;
    private String name;
    private String password;
}
