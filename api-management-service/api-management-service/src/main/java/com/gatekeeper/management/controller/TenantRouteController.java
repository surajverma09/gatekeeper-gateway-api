package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.AuditRecordResponse;
import com.gatekeeper.management.dto.RouteStatusRequest;
import com.gatekeeper.management.dto.RouteVersionResponse;
import com.gatekeeper.management.dto.route.RouteRequest;
import com.gatekeeper.management.dto.route.RouteResponse;
import com.gatekeeper.management.service.RouteService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/tenant")
public class TenantRouteController {

    private final RouteService routeService;

    @PostMapping("/routes")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteRequest request){

        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.create(request));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<RouteResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(routeService.getById(id));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponse>> getAll(){
        return ResponseEntity.ok(routeService.getAll());
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<RouteResponse> update(@Valid @RequestBody RouteRequest request,
                                                @PathVariable Long id){
        return ResponseEntity.ok(routeService.updateById(request, id));
    }

    @PatchMapping("/routes/{id}")
    public ResponseEntity<RouteResponse> changeStatus(@Valid @RequestBody RouteStatusRequest request,
                                                      @PathVariable Long id){

        return ResponseEntity.ok(routeService.changeStatus(request, id));
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<RouteResponse> deleteRoute(@PathVariable Long id){
        routeService.deleteRoute(id);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/route-versions/{routeId}")
    public ResponseEntity<List<RouteVersionResponse>> getVersion(@PathVariable String routeId){
            return ResponseEntity.ok(routeService.getAllVersions(routeId));
    }

    @GetMapping("/audit-records")
    public ResponseEntity<List<AuditRecordResponse>> getAllAuditRecords(){

        return ResponseEntity.ok(routeService.getAllAuditRecords());
    }
}
