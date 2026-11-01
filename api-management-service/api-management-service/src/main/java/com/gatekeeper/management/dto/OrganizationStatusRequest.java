package com.gatekeeper.management.dto;

import com.gatekeeper.management.entity.OrganizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrganizationStatusRequest {

    @NotNull
    private OrganizationStatus status;
}
