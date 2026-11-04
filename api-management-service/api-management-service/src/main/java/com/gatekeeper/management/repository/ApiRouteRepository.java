package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.ApiRoute;
import com.gatekeeper.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {

    boolean existsByOrganizationIdAndRouteId(long organizationId, String routeId);

    List<ApiRoute> findAllByOrganizationId(Long organizationId);


    Optional<ApiRoute> findByIdAndOrganizationId(Long id, Long organizationId);


    List<ApiRoute> findAllByActiveTrueAndOrganizationStatus(OrganizationStatus status);
}
