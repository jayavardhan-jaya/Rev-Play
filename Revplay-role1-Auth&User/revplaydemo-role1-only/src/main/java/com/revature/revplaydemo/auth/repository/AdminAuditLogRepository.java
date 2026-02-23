package com.revature.revplaydemo.auth.repository;

import com.revature.revplaydemo.auth.entity.AdminAuditLogEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long> {

    List<AdminAuditLogEntity> findByOrderByCreatedAtDesc(Pageable pageable);
}
