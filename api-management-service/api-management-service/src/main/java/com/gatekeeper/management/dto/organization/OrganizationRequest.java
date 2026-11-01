package com.gatekeeper.management.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrganizationRequest {

    @Size(max = 150)
    @NotBlank
    private String name;

    @Size(max = 150)
    @NotBlank
    private String slug;

}
