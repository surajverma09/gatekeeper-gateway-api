package com.gatekeeper.management.repository;

import com.gatekeeper.management.dto.organization.OrganizationResponse;
import com.gatekeeper.management.entity.Organization;
import com.gatekeeper.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    List<Organization> findByStatusNot(OrganizationStatus status);

    Optional<Organization> findByIdAndStatusNot(Long id , OrganizationStatus status);
}
