package peps.peps_back.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.AuditLog;

/**
 * Spring Data JPA repository for {@link AuditLog} entities, mapping to the {@code audit_log} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    /**
     * Retrieves all audit log entries performed by a given user.
     *
     * @param userId the identifier of the user
     * @return the list of matching audit log entries; empty if none match
     */
    public List<AuditLog> findByUser_UserId(Integer userId);
}
