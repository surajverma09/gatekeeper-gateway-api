package com.gatekeeper.management.dto;

import com.gatekeeper.management.dto.route.RouteResponse;
import com.gatekeeper.management.entity.RouteConfigurationVersion;
import com.gatekeeper.management.repository.VersionRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RouteVersionResponse {

    private Long id;
    private String routeId;
    private Long versionNumber;
    private String changeType;
    private String ConfigurationSnapshot;
    private LocalDateTime  createdAt;

    public static RouteVersionResponse from(RouteConfigurationVersion version){
        return new RouteVersionResponse(
                version.getId(),
                version.getRouteId(),
                version.getVersionNumber(),
                version.getChangeType(),
                version.getConfigurationSnapshot(),
                version.getCreatedAt()
        );
    }
}
