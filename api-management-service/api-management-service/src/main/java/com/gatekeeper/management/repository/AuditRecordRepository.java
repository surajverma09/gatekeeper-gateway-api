package com.gatekeeper.management.repository;

import com.gatekeeper.management.dto.OrganizationStatusRequest;
import com.gatekeeper.management.entity.ControlPlaneAuditRecord;
import com.gatekeeper.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<ControlPlaneAuditRecord, Long> {

    List<ControlPlaneAuditRecord> getAllAuditRecordsByOrganizationIdAndOrganization_status(Long organizationId, OrganizationStatus statusRequest);
}
