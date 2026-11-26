package peps.peps_back.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.AuditLog;
import peps.peps_back.domain.User;
import peps.peps_back.repositories.AuditLogRepository;
import peps.peps_back.repositories.UserRepository;

import java.util.List;

/**
 * Service layer responsible for recording and querying security and operational
 * audit trails across the PEP'S backend.
 *
 * Supports both user-initiated operations and automated background/system events
 * where {@code userId} is {@code null}.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
@Service
public class AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Constructs an {@code AuditService} with its required persistence repositories.
     *
     * @param auditLogRepository repository managing {@link AuditLog} entries
     * @param userRepository     repository used to resolve acting {@link User} entities
     */
    @Autowired
    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Records a general audit log entry without target entity metadata.
     *
     * @param userId  optional identifier of the acting user ({@code null} for system events)
     * @param action  action type performed (e.g., {@code "LOGIN"}, {@code "EXPORT"})
     * @param details human-readable or JSON description of the event
     */
    @Transactional
    public void logAction(Integer userId, String action, String details) {
        logEntityAction(userId, action, null, null, details);
    }

    /**
     * Records an audit log entry associated with a specific domain entity type and ID.
     *
     * @param userId     optional identifier of the acting user ({@code null} for system events)
     * @param action     action type performed (e.g., {@code "CREATE"}, {@code "UPDATE"}, {@code "DELETE"})
     * @param entityType optional target entity name (e.g., {@code "SOUND"}, {@code "IOT_MODULE"})
     * @param entityId   optional primary key of the affected entity
     * @param details    human-readable or JSON description of the change
     */
    @Transactional
    public void logEntityAction(
            Integer userId,
            String action,
            String entityType,
            Integer entityId,
            String details) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Audit action must not be null or blank");
        }

        User actor = null;
        if (userId != null) {
            if (userId <= 0) {
                throw new IllegalArgumentException("User ID must be a positive integer");
            }
            actor = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        }

        String normalizedEntityType = (entityType == null || entityType.isBlank())
                ? null
                : entityType.trim();

        AuditLog entry = new AuditLog(
                action.trim(),
                normalizedEntityType,
                entityId,
                details,
                actor
        );

        auditLogRepository.save(entry);
        LOGGER.debug("Recorded audit entry: action='{}', entityType='{}', entityId={}, userId={}",
                entry.getAction(), normalizedEntityType, entityId, userId);
    }

    /**
     * Retrieves audit log entries optionally filtered by acting user ID.
     *
     * @param userId optional user ID filter; when {@code null}, returns all audit logs
     * @return list of matching {@link AuditLog} records
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(Integer userId) {
        if (userId == null) {
            return auditLogRepository.findAll();
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive integer");
        }
        return auditLogRepository.findByUser_UserId(userId);
    }
}