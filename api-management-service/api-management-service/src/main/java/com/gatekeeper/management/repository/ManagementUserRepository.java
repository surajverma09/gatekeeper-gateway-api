package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.ManagementUser;
import com.gatekeeper.management.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagementUserRepository extends JpaRepository<ManagementUser, Long> {

    Optional<ManagementUser> findByUsername(String username);


    boolean existsByOrganization_Id(Long organizationId);

    boolean existsByUsername(String username);

    Optional<Roles> findByRoleName(String role);

    Optional<ManagementUser> findByOrganizationId(Long id);
}
