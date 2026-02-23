package com.revature.revplaydemo.auth.service;

import com.revature.revplaydemo.auth.entity.AdminAuditLogEntity;
import com.revature.revplaydemo.auth.repository.AdminAuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    public void log(Long adminUserId, String actionType, String targetEntity, Long targetEntityId, String details) {
        AdminAuditLogEntity entity = new AdminAuditLogEntity();
        entity.setAdminUserId(adminUserId);
        entity.setActionType(actionType);
        entity.setTargetEntity(targetEntity);
        entity.setTargetEntityId(targetEntityId);
        entity.setDetails(details);
        entity.setCreatedAt(Instant.now());
        adminAuditLogRepository.save(entity);
    }
}
