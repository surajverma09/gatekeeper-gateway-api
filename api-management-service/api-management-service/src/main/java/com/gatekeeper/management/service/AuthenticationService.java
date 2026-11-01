package com.gatekeeper.management.service;

import com.gatekeeper.management.Exception.ApiException;
import com.gatekeeper.management.dto.login.LoginRequest;
import com.gatekeeper.management.dto.login.LoginResponse;
import com.gatekeeper.management.entity.ManagementUser;
import com.gatekeeper.management.entity.OrganizationStatus;
import com.gatekeeper.management.repository.ManagementUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthenticationService {

    private final ManagementUserRepository managementUserRepository;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;
    @Value("${security.jwt.expiration-seconds}")
    private final long expirationInSeconds;


    public AuthenticationService(ManagementUserRepository managementUserRepository,
                                 JwtEncoder jwtEncoder,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${security.jwt.expiration-seconds}") long expirationInSeconds) {
        this.managementUserRepository = managementUserRepository;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.expirationInSeconds = expirationInSeconds;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest){

        ManagementUser user = managementUserRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(()-> new ApiException(HttpStatus.NOT_FOUND, "User is not authenticated to login"));

        if(!user.getEnabled() && passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())){
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if(user.getOrganizationId() != null && user.getOrganizationId().getStatus() != OrganizationStatus.ACTIVE){
            throw new ApiException(HttpStatus.FORBIDDEN, "The organization is not active");
        }

        Instant issuedAt = Instant.now();
        String role = user.getRoleId().getName();
        Long organizationId = user.getOrganizationId() == null ? null : user.getOrganizationId().getId();


        JwtClaimsSet.Builder Claims = JwtClaimsSet.builder()
                .issuer("gatekeeper-api-management")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirationInSeconds))
                .subject(user.getUsername())
                .claim("role", role);

        if(organizationId != null){
            Claims.claim("organizationId", organizationId);
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, Claims.build())).getTokenValue();

        return new LoginResponse(token, "bearer", expirationInSeconds, role, organizationId);

    }

}
