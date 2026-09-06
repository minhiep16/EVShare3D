package com.example.evshare.repository;

import com.example.evshare.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityId(String entityName, Long entityId);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startTime, Instant endTime);
}
