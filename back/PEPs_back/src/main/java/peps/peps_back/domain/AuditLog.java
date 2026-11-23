package peps.peps_back.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * Represents a security and system audit log entry tracking sensitive operations
 * (e.g., LOGIN, CREATE, UPDATE, DELETE) performed by users or automated background
 * tasks, mapping to the {@code audit_logs} table.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
@Entity
@Table(name = "audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the audit log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Integer logId;

    /**
     * Action type performed (e.g., LOGIN, CREATE, UPDATE, DELETE).
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /**
     * Optional target domain entity type affected by the action (e.g., IoTModule, Sound, User).
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Optional primary key identifier of the affected entity record.
     */
    @Column(name = "entity_id")
    private Integer entityId;

    /**
     * Timestamp indicating when the audited action was recorded.
     */
    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    /**
     * Detailed description or serialized JSON context describing the state change.
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * User who triggered the audited action, or {@code null} for automated system tasks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    private User user;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public AuditLog() {
    }

    /**
     * Constructs a new {@code AuditLog} entry with the specified action details and actor.
     *
     * @param action     the action type performed; must not be {@code null}
     * @param entityType the target entity name, or {@code null} if not tied to a specific entity
     * @param entityId   the identifier of the target entity, or {@code null} if not applicable
     * @param details    descriptive text or JSON payload of the operation
     * @param user       the {@link User} who performed the action, or {@code null} for system events
     */
    public AuditLog(String action, String entityType, Integer entityId, String details, User user) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.user = user;
    }

    /**
     * Automatically sets {@code loggedAt} to the current timestamp before persisting
     * if it has not been explicitly initialized.
     */
    @PrePersist
    protected void onCreate() {
        if (this.loggedAt == null) {
            this.loggedAt = LocalDateTime.now();
        }
    }

    /**
     * Returns the unique identifier of this audit log entry.
     *
     * @return the primary key {@code logId}, or {@code null} if not yet persisted
     */
    public Integer getLogId() {
        return logId;
    }

    /**
     * Sets the unique identifier of this audit log entry.
     *
     * @param logId the primary key to assign
     */
    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    /**
     * Returns the type of action recorded.
     *
     * @return the action name (e.g., {@code "CREATE"}, {@code "UPDATE"}, {@code "DELETE"})
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the type of action recorded.
     *
     * @param action the action name to assign (maximum 50 characters)
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Returns the type of entity affected by the action, if applicable.
     *
     * @return the target entity type name, or {@code null}
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Sets the type of entity affected by the action.
     *
     * @param entityType the target entity type name to assign (maximum 50 characters)
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * Returns the identifier of the affected entity record, if applicable.
     *
     * @return the target entity ID, or {@code null}
     */
    public Integer getEntityId() {
        return entityId;
    }

    /**
     * Sets the identifier of the affected entity record.
     *
     * @param entityId the target entity ID to assign
     */
    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    /**
     * Returns the timestamp when this log entry was recorded.
     *
     * @return the recording {@code LocalDateTime}
     */
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    /**
     * Sets the timestamp when this log entry was recorded.
     *
     * @param loggedAt the recording {@code LocalDateTime} to assign
     */
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    /**
     * Returns the detailed description or JSON payload of the audited event.
     *
     * @return the details string, or {@code null}
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the detailed description or JSON payload of the audited event.
     *
     * @param details the details string to assign
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Returns the user who performed the audited action, if applicable.
     *
     * @return the responsible {@link User}, or {@code null} for system-generated events
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who performed the audited action.
     *
     * @param user the responsible {@link User} to assign, or {@code null}
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Compares this audit log entry to another object for equality based on {@code logId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted log; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(logId, auditLog.logId);
    }

    /**
     * Returns a hash code value for this audit log entry based on {@code logId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(logId);
    }

    /**
     * Returns a string representation of this {@code AuditLog} entity without
     * triggering lazy-loaded associations or dumping large payloads.
     *
     * @return a formatted string containing the log ID, action, entity type, and timestamp
     */
    @Override
    public String toString() {
        return "AuditLog{" +
                "logId=" + logId +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", loggedAt=" + loggedAt +
                '}';
    }
}