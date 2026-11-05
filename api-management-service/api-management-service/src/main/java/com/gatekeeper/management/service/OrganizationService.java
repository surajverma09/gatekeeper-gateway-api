package com.gatekeeper.management.service;


import com.gatekeeper.management.Exception.ApiException;
import com.gatekeeper.management.dto.OrganizationStatusRequest;
import com.gatekeeper.management.dto.TenantOrganizationRequest;
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
    private final TenantContext tenantContext;
    private final AuditService auditService;


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

        organizationRepository.save(org);


        auditService.recordAudit(org, "CREATE_ORGANIZATION", "ORGANIZATION",
                org.getId().toString(), instantTime, "Organization status in PENDING state" );

        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAll(){
            return organizationRepository.findByStatusNot(OrganizationStatus.DELETED)
                    .stream()
                    .map(OrganizationResponse::from)
                    .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(Long id){
        return OrganizationResponse.from(getOrganization(id));
    }

    @Transactional
    public OrganizationResponse update(OrganizationRequest request, Long id){
        Long startedAt = System.nanoTime();
        Organization organization = getOrganization(id);
        checkUnique(request.getName(), request.getSlug(), id);
        organization.setName(request.getName());
        organization.setSlug(request.getSlug());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.recordAudit(organization, "UPDATE_ORGANIZATION", "ORGANIZATION",
                id.toString(), startedAt, "Organization name and slug is updated" );

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

        auditService.recordAudit(organization, "STATUS_UPDATE_ORGANIZATION", "ORGANIZATION",
                id.toString(), instantTime, "Organization status is updated" );


        return OrganizationResponse.from(organization);
    }

    @Transactional
    public void softDelete(Long id){

        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);

        if(organization.getStatus() == OrganizationStatus.DELETED){
            throw new ApiException(HttpStatus.NOT_FOUND, "Organization is not found to delete");
        }

        organization.setStatus(OrganizationStatus.DELETED);
        organization.setDeletedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.recordAudit(organization, "DELETE_ORGANIZATION", "ORGANIZATION",
                id.toString() , startedAt, "Organization soft-deleted" );
    }

    // CRUD for TENANT admin

    @Transactional
    public TenantAdminResponse createTenantAdmin(Long organizationId, TenantAdminRequest req){

        Long instantTime = System.nanoTime();

        Organization organization = getOrganization(organizationId);

        if(managementUserRepository.existsByOrganization_Id(organizationId)){
            throw new ApiException(HttpStatus.CONFLICT, "Organization already has a tenant administrator");
        }

        if(managementUserRepository.existsByUsername(req.getUsername())){
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        Roles role = roleRepository.findByName("TENANT_ADMIN")
                .orElseThrow(()-> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "TENANT_ADMIN is missing"));


        ManagementUser user = new ManagementUser();
        user.setOrganization(organization);
        user.setRole(role);
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        user.setEnabled(true);

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        ManagementUser savedUser = managementUserRepository.save(user);

        auditService.recordAudit(organization, "CREATE_TENANT_ADMIN", "MANAGEMENT_USER",
                 savedUser.getId().toString(), instantTime, "Tenant admin is created" );

        return TenantAdminResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public TenantAdminResponse getTenantAdminById(Long organizationId){
        return TenantAdminResponse.from(getManagementUser(organizationId));
    }

    @Transactional
    public TenantAdminResponse updateTenantAdmin(Long organizationId, TenantAdminRequest req){
        Long startedAt = System.nanoTime();
        ManagementUser user = getManagementUser(organizationId);

        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEnabled(
                req.getEnabled() == null || req.getEnabled()
        );
        user.setUpdatedAt(LocalDateTime.now());

        auditService.recordAudit(user.getOrganization(), "UPDATE_TENANT_ADMIN", "TENANT_ADMIN",
                user.getId().toString(), startedAt, "Tenant admin is updated" );

        return TenantAdminResponse.from(managementUserRepository.save(user));
    }

    @Transactional
    public String deleteTenantAdmin(Long organizationId){
        Long startedAt = System.nanoTime();
        ManagementUser user = getManagementUser(organizationId);

        auditService.recordAudit(user.getOrganization(), "DELETE_TENANT_ADMIN", "TENANT_ADMIN",
                user.getId().toString(), startedAt, "Tenant admin is deleted" );

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

    @Transactional(readOnly = true)
    public OrganizationResponse getOwnOrganization(){

        return OrganizationResponse.from(tenantContext.getOrganization());
    }

    @Transactional
    public OrganizationResponse updateOwnOrganization(TenantOrganizationRequest request){

        Long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();

        if(organizationRepository.existsByNameIgnoreCase(request.getName()) &&
                !organization.getName().equalsIgnoreCase(request.getName())){
            throw new ApiException(HttpStatus.CONFLICT, "Organization name already exists");
        }

        organization.setName(request.getName());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.recordAudit(organization, "UPDATE_OWN_ORGANIZATION", "ORGANIZATION",
                organization.getId().toString(), startedAt, "Tenant updates its own name" );

        return OrganizationResponse.from(organization);
    }
}

