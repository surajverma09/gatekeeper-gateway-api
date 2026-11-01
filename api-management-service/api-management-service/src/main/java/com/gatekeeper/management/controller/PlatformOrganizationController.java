package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.OrganizationStatusRequest;
import com.gatekeeper.management.dto.organization.OrganizationRequest;
import com.gatekeeper.management.dto.organization.OrganizationResponse;
import com.gatekeeper.management.dto.tenantadmin.TenantAdminRequest;
import com.gatekeeper.management.dto.tenantadmin.TenantAdminResponse;
import com.gatekeeper.management.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/platform/organization")
public class PlatformOrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create( @Valid @RequestBody OrganizationRequest req){

        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable Long id){

        return ResponseEntity.ok(organizationService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAll(){
        return ResponseEntity.ok(organizationService.getAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrganizationResponse> changeStatus(@PathVariable Long id,
                                                             @Valid @RequestBody OrganizationStatusRequest req){
        return ResponseEntity.ok(organizationService.changeStatus(id, req));
    }

    @PutMapping("{id}")
    public ResponseEntity<OrganizationResponse> updateData(@PathVariable Long id,
                                                           @Valid @RequestBody OrganizationRequest req){

        return ResponseEntity.ok(organizationService.update(req, id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){

        organizationService.softDelete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> createTenantAdmin(@PathVariable Long organizationId,
                                                                 @Valid @RequestBody
                                                                 TenantAdminRequest req){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createTenantAdmin(organizationId, req));
    }

    @GetMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> getTenantAdminById(@PathVariable long organizationId){

        return ResponseEntity.ok(organizationService.getTenantAdminById(organizationId));
    }

    @PutMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> updateTenantAdmin(@PathVariable long organizationId,
                                                      @Valid @RequestBody TenantAdminRequest req ){
        return ResponseEntity.ok(organizationService.updateTenantAdmin(organizationId, req));
    }

    @DeleteMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<Void> deleteTenantAdmin(@PathVariable Long organizationId){
        organizationService.deleteTenantAdmin(organizationId);
        return ResponseEntity.noContent().build();
    }
}