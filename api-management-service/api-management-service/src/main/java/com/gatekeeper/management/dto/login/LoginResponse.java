package com.gatekeeper.management.dto.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long tokenExpiry;
    private String role;
    private Long organization;
}
