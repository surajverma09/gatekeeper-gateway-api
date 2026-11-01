package com.gatekeeper.management.service;

import com.gatekeeper.management.Exception.ApiException;
import com.gatekeeper.management.entity.Organization;
import com.gatekeeper.management.entity.OrganizationStatus;
import com.gatekeeper.management.repository.OrganizationRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TenantContext {

    private final OrganizationRepository organizationRepository;

    public Organization getOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant identity is missing");
        }

        Long organizationId = ((JwtAuthenticationToken) authentication).getToken()
                .getClaim("organizationId");

        if (organizationId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant identity is missing");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(()-> new ApiException(HttpStatus.FORBIDDEN, "Organization doesn't exists"));

        if(organization.getStatus() != OrganizationStatus.ACTIVE){
            throw new ApiException(HttpStatus.FORBIDDEN, "The organization is not active");
        }

        return organization;
    }
}
