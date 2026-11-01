package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.TenantOrganizationRequest;
import com.gatekeeper.management.dto.organization.OrganizationResponse;
import com.gatekeeper.management.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/tenant/organization")
public class TenantOrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<OrganizationResponse> getOwnOrganization(){
        return ResponseEntity.ok(organizationService.getOwnOrganization());
    }

    @PatchMapping
    public ResponseEntity<OrganizationResponse> updateOwnOrganization(
            @Valid @RequestBody TenantOrganizationRequest req){
        return ResponseEntity.ok(organizationService.updateOwnOrganization(req));
    }
}
