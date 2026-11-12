package peps.peps_back.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link AuditLog} domain entity.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
class AuditLogTest {

    private User adminUser;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        adminUser = new User("franco.admin", "$2a$10$hashedPassword", "ROLE_ADMIN");
        adminUser.setUserId(1);

        auditLog = new AuditLog(
                "UPDATE",
                "IoTModule",
                5,
                "{\"volume\":{\"old\":50,\"new\":80}}",
                adminUser
        );
        auditLog.setLogId(10);
    }

    @Test
    @DisplayName("Default constructor should initialize all fields and user association to null")
    void testDefaultConstructor() {
        AuditLog emptyLog = new AuditLog();

        assertNull(emptyLog.getLogId());
        assertNull(emptyLog.getAction());
        assertNull(emptyLog.getEntityType());
        assertNull(emptyLog.getEntityId());
        assertNull(emptyLog.getLoggedAt());
        assertNull(emptyLog.getDetails());
        assertNull(emptyLog.getUser());
    }

    @Test
    @DisplayName("Parameterized constructor should assign audit metadata and optional actor")
    void testParameterizedConstructor() {
        AuditLog systemLog = new AuditLog("SYSTEM_CLEANUP", null, null, "Archived expired records", null);

        assertNull(systemLog.getLogId());
        assertEquals("SYSTEM_CLEANUP", systemLog.getAction());
        assertNull(systemLog.getEntityType());
        assertNull(systemLog.getEntityId());
        assertNull(systemLog.getLoggedAt());
        assertEquals("Archived expired records", systemLog.getDetails());
        assertNull(systemLog.getUser());
    }

    @Test
    @DisplayName("onCreate() should populate loggedAt only when it is null")
    void testOnCreateLifecycleCallback() {
        // Branch 1: loggedAt is null -> should be initialized automatically
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        auditLog.onCreate();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(auditLog.getLoggedAt());
        assertTrue(auditLog.getLoggedAt().isAfter(before));
        assertTrue(auditLog.getLoggedAt().isBefore(after));

        // Branch 2: loggedAt is already set -> should preserve the explicit timestamp
        LocalDateTime explicitTimestamp = LocalDateTime.of(2026, 11, 12, 19, 0, 0);
        auditLog.setLoggedAt(explicitTimestamp);
        auditLog.onCreate();

        assertEquals(explicitTimestamp, auditLog.getLoggedAt());
    }

    @Test
    @DisplayName("Getters and setters should read and update all attributes and user relationship")
    void testGettersAndSetters() {
        LocalDateTime loggedTime = LocalDateTime.of(2026, 11, 12, 14, 15, 0);
        auditLog.setLoggedAt(loggedTime);

        assertEquals(10, auditLog.getLogId());
        assertEquals("UPDATE", auditLog.getAction());
        assertEquals("IoTModule", auditLog.getEntityType());
        assertEquals(5, auditLog.getEntityId());
        assertEquals(loggedTime, auditLog.getLoggedAt());
        assertEquals("{\"volume\":{\"old\":50,\"new\":80}}", auditLog.getDetails());
        assertSame(adminUser, auditLog.getUser());

        LocalDateTime updatedLoggedTime = LocalDateTime.of(2026, 11, 12, 20, 0, 0);
        auditLog.setLogId(11);
        auditLog.setAction("LOGIN");
        auditLog.setEntityType(null);
        auditLog.setEntityId(null);
        auditLog.setLoggedAt(updatedLoggedTime);
        auditLog.setDetails("User authenticated via web portal");
        auditLog.setUser(null);

        assertEquals(11, auditLog.getLogId());
        assertEquals("LOGIN", auditLog.getAction());
        assertNull(auditLog.getEntityType());
        assertNull(auditLog.getEntityId());
        assertEquals(updatedLoggedTime, auditLog.getLoggedAt());
        assertEquals("User authenticated via web portal", auditLog.getDetails());
        assertNull(auditLog.getUser());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(auditLog.equals(auditLog));

        // Null and different class comparisons
        assertFalse(auditLog.equals(null));
        assertFalse(auditLog.equals("UPDATE"));

        // Different instance with the same ID
        AuditLog sameIdLog = new AuditLog();
        sameIdLog.setLogId(10);
        assertTrue(auditLog.equals(sameIdLog));

        // Different instance with a different ID
        AuditLog differentIdLog = new AuditLog("DELETE", "Sound", 3, "Deleted sound", adminUser);
        differentIdLog.setLogId(99);
        assertFalse(auditLog.equals(differentIdLog));

        // Both instances transient (null IDs)
        AuditLog transientOne = new AuditLog();
        AuditLog transientTwo = new AuditLog();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with logId")
    void testHashCode() {
        assertEquals(Objects.hash(10), auditLog.hashCode());

        AuditLog sameIdLog = new AuditLog();
        sameIdLog.setLogId(10);
        assertEquals(auditLog.hashCode(), sameIdLog.hashCode());

        AuditLog differentIdLog = new AuditLog();
        differentIdLog.setLogId(88);
        assertNotEquals(auditLog.hashCode(), differentIdLog.hashCode());
    }

    @Test
    @DisplayName("toString() should format metadata safely without dumping details or lazy user")
    void testToString() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 11, 12, 20, 5, 0);
        auditLog.setLoggedAt(fixedTime);

        String output = auditLog.toString();
        String expected = "AuditLog{logId=10, action='UPDATE', entityType='IoTModule', " +
                "loggedAt=2026-11-12T20:05}";

        assertEquals(expected, output);
        assertFalse(output.contains("volume"));

        AuditLog transientLog = new AuditLog();
        assertEquals(
                "AuditLog{logId=null, action='null', entityType='null', loggedAt=null}",
                transientLog.toString()
        );
    }
}