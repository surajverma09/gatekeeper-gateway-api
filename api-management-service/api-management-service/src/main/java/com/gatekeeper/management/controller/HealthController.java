package com.gatekeeper.management.controller;

import com.gatekeeper.management.entity.HealthResponse;
import com.gatekeeper.management.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    public ResponseEntity<HealthResponse> health(){

        String dbStatus = healthService.getHealth();

        return ResponseEntity.ok(new HealthResponse("UP", dbStatus, LocalDateTime.now()));
    }

}
