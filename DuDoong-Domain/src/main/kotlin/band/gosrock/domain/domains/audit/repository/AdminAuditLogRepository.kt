package band.gosrock.domain.domains.audit.repository

import band.gosrock.domain.domains.audit.domain.AdminAuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAuditLogRepository : JpaRepository<AdminAuditLog, Long>
