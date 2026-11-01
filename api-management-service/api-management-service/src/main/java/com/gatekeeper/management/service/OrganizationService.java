package com.gatekeeper.management.service;


import com.gatekeeper.management.Exception.ApiException;
import com.gatekeeper.management.dto.OrganizationStatusRequest;
import com.gatekeeper.management.dto.organization.OrganizationRequest;
import com.gatekeeper.management.dto.organization.OrganizationResponse;
import com.gatekeeper.management.dto.tenantadmin.TenantAdminRequest;
import com.gatekeeper.management.dto.tenantadmin.TenantAdminResponse;
import com.gatekeeper.management.entity.ManagementUser;
import com.gatekeeper.management.entity.Organization;
import com.gatekeeper.management.entity.OrganizationStatus;
import com.gatekeeper.management.entity.Roles;
import com.gatekeeper.management.repository.ManagementUserRepository;
import com.gatekeeper.management.repository.OrganizationRepository;
import com.gatekeeper.management.repository.RolesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ManagementUserRepository managementUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolesRepository roleRepository;


    // CRUD for organization
    @Transactional
    public OrganizationResponse create(OrganizationRequest req){

        Long instantTime = System.nanoTime();

        checkUnique(req.getName(), req.getSlug(), null);

        Organization org = new Organization();

        org.setName(req.getName());
        org.setSlug(req.getSlug());
        org.setStatus(OrganizationStatus.PENDING);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());

        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAll(){
            return organizationRepository.findByStatusNot(OrganizationStatus.DELETED)
                    .stream()
                    .map(OrganizationResponse::from)
                    .toList();
    }

    @Transactional
    public OrganizationResponse getById(Long id){
        return OrganizationResponse.from(getOrganization(id));
    }

    @Transactional
    public OrganizationResponse update(OrganizationRequest request, Long id){
        Long startedAt = System.nanoTime();
        Organization organization = getOrganization(id);
        checkUnique(request.getName(), request.getName(), id);
        organization.setName(request.getName());
        organization.setSlug(request.getSlug());
        organization.setUpdatedAt(LocalDateTime.now());

        return OrganizationResponse.from(organization);
    }

    @Transactional
    public OrganizationResponse changeStatus(Long id, OrganizationStatusRequest request){

        Long instantTime = System.nanoTime();

        Organization organization = getOrganization(id);

        if(request.getStatus() == OrganizationStatus.DELETED){
            throw new ApiException
                    (HttpStatus.BAD_REQUEST, "Please Use DELETE or soft-delete an organization");
        }
        organization.setStatus(request.getStatus());
        organization.setUpdatedAt(LocalDateTime.now());

        return OrganizationResponse.from(organization);
    }

    @Transactional
    public void softDelete(long id){

        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);

        if(organization.getStatus() == OrganizationStatus.DELETED){
            throw new ApiException(HttpStatus.NOT_FOUND, "Organization is not found to delete");
        }

        organization.setStatus(OrganizationStatus.DELETED);
        organization.setDeletedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());
    }

    // CRUD for TENANT admin

    @Transactional
    public TenantAdminResponse createTenantAdmin(Long organizationId, TenantAdminRequest req){

        Long instantTime = System.nanoTime();

        Organization organization = getOrganization(organizationId);

        if(managementUserRepository.existsByOrganization_Id(organizationId)){
            throw new ApiException(HttpStatus.CONFLICT, "Organization is already exist with this name");
        }

        if(managementUserRepository.existsByUsername(req.getUsername())){
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        Roles role = roleRepository.findByName("TENANT_ADMIN")
                .orElseThrow(()-> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TENANT_ADMIN is missing"));


        ManagementUser user = new ManagementUser();
        user.setOrganization(organization);
        user.setRole(role);
        user.setEnabled(true);
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return TenantAdminResponse.from(managementUserRepository.save(user));
    }

    @Transactional
    public TenantAdminResponse getTenantAdminById(Long organizationId){
        return TenantAdminResponse.from(getManagementUser(organizationId));
    }

    @Transactional
    public TenantAdminResponse updateTenantAdmin(Long organizationId, TenantAdminRequest req){
        ManagementUser user = getManagementUser(organizationId);

        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());

        return TenantAdminResponse.from(managementUserRepository.save(user));
    }

    @Transactional
    public String deleteTenantAdmin(Long organizationId){
        ManagementUser user = getManagementUser(organizationId);

        managementUserRepository.delete(user);

        return "Deleted successfully" ;
    }

    public Organization getOrganization(Long id){
        return organizationRepository.findByIdAndStatusNot(id, OrganizationStatus.DELETED)
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    public ManagementUser getManagementUser(Long id){
        return managementUserRepository.findByOrganizationId(id)
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND , "Tenant admin is not found"));
    }



    private void checkUnique(String name, String slug, Long currentId) {

        if (organizationRepository.existsByNameIgnoreCase(name)) {

            Organization current = currentId == null
                    ? null
                    : organizationRepository.findById(currentId).orElse(null);

            if (current == null ||
                    !current.getName().equalsIgnoreCase(name)) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Organization name already exists"
                );
            }
        }

        if (organizationRepository.existsBySlugIgnoreCase(slug)) {

            Organization current = currentId == null
                    ? null
                    : organizationRepository.findById(currentId).orElse(null);

            if (current == null ||
                    !current.getSlug().equalsIgnoreCase(slug)) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Organization slug already exists"
                );
            }
        }
    }
}
