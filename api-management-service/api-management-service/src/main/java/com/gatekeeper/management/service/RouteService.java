package com.gatekeeper.management.service;

import com.gatekeeper.management.Exception.ApiException;
import com.gatekeeper.management.dto.AuditRecordResponse;
import com.gatekeeper.management.dto.RouteStatusRequest;
import com.gatekeeper.management.dto.RouteVersionResponse;
import com.gatekeeper.management.dto.route.RouteRequest;
import com.gatekeeper.management.dto.route.RouteResponse;
import com.gatekeeper.management.entity.*;
import com.gatekeeper.management.repository.ApiRouteRepository;
import com.gatekeeper.management.repository.AuditRecordRepository;
import com.gatekeeper.management.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final ApiRouteRepository apiRouteRepository;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;
    private final VersionRepository versionRepository;
    private final AuditService auditService;
    private final AuditRecordRepository auditRecordRepository;

    @Transactional
    public RouteResponse create(RouteRequest request){

        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();

        validate(request, organization);

        if(apiRouteRepository.existsByOrganizationIdAndRouteId(organization.getId(), request.getRouteId())){
            throw new ApiException(HttpStatus.CONFLICT, "Route Id already exists for this organization");
        }


        ApiRoute route = new ApiRoute();
        route.setOrganization(organization);
        route.setConfigurationVersion(1L);
        route.setCreatedAt(LocalDateTime.now());

        apply(route, request);

        saveVersion(route, "CREATED");

        apiRouteRepository.save(route);

        auditService.recordAudit(organization, "CREATE_ROUTE", "API_ROUTE",
                route.getId().toString(), startedAt, "Route configuration version 1 is created" );

        return RouteResponse.from(route);
    }


    @Transactional
    public RouteResponse getById(Long id){

        Organization org = tenantContext.getOrganization();

        return RouteResponse.from(apiRouteRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND, "Route not found")));
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getAll(){

        Organization organization = tenantContext.getOrganization();

        List<ApiRoute> routes = apiRouteRepository.findAllByOrganizationId(organization.getId());


        return routes.stream().map(RouteResponse::from).toList();
    }

    @Transactional
    public RouteResponse updateById(RouteRequest req , Long id){

        long startedAt = System.nanoTime();

        Organization org = tenantContext.getOrganization();

        ApiRoute route = apiRouteRepository.findByIdAndOrganizationId(id, org.getId()).orElseThrow(
                ()-> new ApiException(HttpStatus.NOT_FOUND, "Api route doesn't exist")
        );

        if(!Objects.equals(route.getRouteId(), req.getRouteId())){
            throw new ApiException(HttpStatus.BAD_REQUEST, "Route id cannot be changed");
        }

        validate(req, org);

        route.setConfigurationVersion(route.getConfigurationVersion() + 1);

        apply(route, req);
        saveVersion(route, "UPDATED");

        auditService.recordAudit(org, "UPDATE_ROUTE", "API_ROUTE",
                route.getId().toString(), startedAt, "Route configuration version " + route.getConfigurationVersion() + " is created" );

        return  RouteResponse.from(route);
    }

    @Transactional
    public RouteResponse changeStatus(RouteStatusRequest req, Long id){

        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();

        ApiRoute route = apiRouteRepository.findByIdAndOrganizationId(id, organization.getId())
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND, "Route doesn't exists"));

        route.setConfigurationVersion(route.getConfigurationVersion()+1);
        route.setActive(req.getActive());
        route.setUpdatedAt(LocalDateTime.now());

        String changeType = req.getActive() ? "ACTIVATED" : "DEACTIVATED";

        saveVersion(route, changeType);

        auditService.recordAudit(organization, "UPDATE_ROUTE_STATUS", "API_ROUTE",
                route.getId().toString(), startedAt, "Route configuration version " +
                        route.getConfigurationVersion() + " is created" );

        return RouteResponse.from(route);
    }

    @Transactional
    public void deleteRoute(Long id){
        Organization org = tenantContext.getOrganization();

        long startedAt = System.nanoTime();

        ApiRoute route = apiRouteRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND, "Route doesn't exists"));

        route.setConfigurationVersion(route.getConfigurationVersion()+1);
        route.setUpdatedAt(LocalDateTime.now());

        saveVersion(route, "DELETED");

        auditService.recordAudit(org, " DELETE_ROUTE", "API_ROUTE",
                route.getId().toString(), startedAt, "Route deleted and configuration version " +
                        route.getConfigurationVersion() + " is created" );

        apiRouteRepository.delete(route);
    }


    // GATEWAY

    @Transactional(readOnly = true)
    public List<RouteResponse> getAllActiveRoutesForGateway(){

        List<ApiRoute> routes =  apiRouteRepository.findAllByActiveTrueAndOrganizationStatus
                (OrganizationStatus.ACTIVE);

        return routes.stream().map(RouteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteVersionResponse> getAllVersions(String routeId){
        Organization organization = tenantContext.getOrganization();

        List<RouteConfigurationVersion> version =  versionRepository.
                findAllByOrganizationIdAndRouteIdOrderByVersionNumberDesc(organization.getId(), routeId);

        return version
                .stream()
                .map(versionResponse -> RouteVersionResponse.from(versionResponse))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getAllAuditRecords(){

        Organization organization = tenantContext.getOrganization();


        List<ControlPlaneAuditRecord> records = auditRecordRepository.
                getAllAuditRecordsByOrganizationIdAndOrganization_status(organization.getId(), OrganizationStatus.ACTIVE);

        return records.stream().
                map(AuditRecords -> AuditRecordResponse.from(AuditRecords)).toList();
    }


    @Transactional
    public void validate(RouteRequest request, Organization organization) {
        String tenantPrefix = "/" + organization.getSlug() + "/";

        if (!request.getPathPattern().startsWith(tenantPrefix)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid pathPattern");
        }
        if (!request.getTargetPathPrefix().startsWith("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "targetPathPrefix must start with /");
        }

        try {
            URI uri = URI.create(request.getTargetBaseUrl());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "targetBaseUrl must be a valid HTTP or HTTPS URL");
        }

        normalizeMethods(request.getAllowedMethods());

        for (RouteHeaderRule rule : request.getHeaderRules()) {
            String direction = rule.getDirection().toUpperCase(Locale.ENGLISH);
            String action = rule.getAction().toUpperCase(Locale.ENGLISH);

            if(!"REQUEST".equals(direction) && !"RESPONSE".equals(direction)){
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Header direction must be REQUEST or RESPONSE");
            }
            if(!"ADD".equals(action) && !"REMOVE".equals(action)){
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Header action must be ADD or REMOVE");
            }
            if("ADD".equals(action) && (rule.getHeaderValue() == null || rule.getHeaderValue().trim().isEmpty())){
                throw new ApiException(HttpStatus.BAD_REQUEST, "ADD header rules require headerValue");
            }
        }
    }

    @Transactional
    public Set<String> normalizeMethods(Set<String> methods) {
        Set<String> normalized = new HashSet<>();

        for (String method : methods) {
            String value = method.toUpperCase(Locale.ENGLISH);
            try {
                HttpMethod.valueOf(value);
            }
            catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported HTTP method : " + method);
            }
            normalized.add(value);
        }
        return normalized;
    }

    @Transactional
    private void apply(ApiRoute route, RouteRequest request) {
        route.setRouteId(request.getRouteId());
        route.setName(request.getName());
        route.setPathPattern(request.getPathPattern());
        route.setTargetBaseUrl(request.getTargetBaseUrl());
        route.setTargetPathPrefix(request.getTargetPathPrefix());
        route.setRequestsPerMinute(request.getRequestsPerMinute());
        route.setResponseTimeoutMs(request.getResponseTimeoutMs());
        route.setIdempotencyEnabled(request.isIdempotencyEnabled());
        route.setActive(request.isActive());
        route.setUpdatedAt(LocalDateTime.now());
        route.setAllowedMethods(normalizeMethods(request.getAllowedMethods()));

        route.getHeaderRules().clear();
        for(RouteHeaderRule ruleRequest : request.getHeaderRules()) {
            RouteHeaderRule rule = new RouteHeaderRule();
            rule.setRoute(route);
            rule.setDirection(ruleRequest.getDirection().toUpperCase(Locale.ENGLISH));
            rule.setAction(ruleRequest.getAction().toUpperCase(Locale.ENGLISH));
            rule.setHeaderName(ruleRequest.getHeaderName());
            rule.setHeaderValue(ruleRequest.getHeaderValue());
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
            route.getHeaderRules().add(rule);
        }
    }

    @Transactional
    public void saveVersion(ApiRoute route, String changeMessage){

        RouteConfigurationVersion version = new RouteConfigurationVersion();

        version.setOrganization(route.getOrganization());
        version.setChangeType(changeMessage);
        version.setRouteId(route.getRouteId());
        version.setCreatedAt(LocalDateTime.now());
        version.setConfigurationSnapshot(
                objectMapper.writeValueAsString(RouteResponse.from(route)));
        version.setVersionNumber(route.getConfigurationVersion());

    }
}
