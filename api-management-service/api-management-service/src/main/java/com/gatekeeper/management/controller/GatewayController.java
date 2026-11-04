package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.route.RouteResponse;
import com.gatekeeper.management.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gateway/routes")
public class GatewayController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteResponse>> getAllActiveRoutes(){

        return ResponseEntity.ok(routeService.getAllActiveRoutesForGateway());
    }
}
