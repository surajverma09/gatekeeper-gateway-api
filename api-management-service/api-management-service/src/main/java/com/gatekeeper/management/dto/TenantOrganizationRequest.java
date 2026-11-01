package com.gatekeeper.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TenantOrganizationRequest {

    @Size(max = 150)
    @NotBlank
    private String name;
}
