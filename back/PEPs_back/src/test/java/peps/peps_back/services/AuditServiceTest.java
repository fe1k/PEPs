package peps.peps_back.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import peps.peps_back.domain.AuditLog;
import peps.peps_back.domain.User;
import peps.peps_back.repositories.AuditLogRepository;
import peps.peps_back.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, userRepository);
    }

    @Test
    void logAction_WithInvalidAction_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAction(1, null, "Login attempt"));
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAction(1, "   ", "Login attempt"));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void logAction_WithNonPositiveUserId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAction(0, "LOGIN", "Invalid user id"));
        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAction(-4, "LOGIN", "Negative user id"));

        verify(userRepository, never()).findById(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void logAction_WhenUserDoesNotExist_ShouldThrowIllegalArgumentException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAction(99, "DELETE", "Attempted delete"));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void logAction_WithValidUser_ShouldPersistAuditLogLinkedToUser() {
        User actor = new User();
        actor.setUserId(5);
        actor.setLogin("soigneur_aras");

        when(userRepository.findById(5)).thenReturn(Optional.of(actor));

        auditService.logAction(5, "  LOGIN  ", "Authenticated via web portal");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertEquals("LOGIN", saved.getAction());
        assertNull(saved.getEntityType());
        assertNull(saved.getEntityId());
        assertEquals("Authenticated via web portal", saved.getDetails());
        assertEquals(actor, saved.getUser());
    }

    @Test
    void logAction_WithNullUserId_ShouldPersistSystemAuditLog() {
        auditService.logAction(null, "SYSTEM_SYNC", "Scheduled heartbeat check");

        verify(userRepository, never()).findById(any());
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertEquals("SYSTEM_SYNC", saved.getAction());
        assertNull(saved.getEntityType());
        assertNull(saved.getEntityId());
        assertEquals("Scheduled heartbeat check", saved.getDetails());
        assertNull(saved.getUser());
    }

    @Test
    void logEntityAction_WithEntityMetadata_ShouldNormalizeEntityTypeAndSave() {
        User actor = new User();
        actor.setUserId(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(actor));

        auditService.logEntityAction(2, "CREATE", "  SOUND  ", 14, "Uploaded Ara vocalization");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertEquals("CREATE", saved.getAction());
        assertEquals("SOUND", saved.getEntityType());
        assertEquals(14, saved.getEntityId());
        assertEquals("Uploaded Ara vocalization", saved.getDetails());
        assertEquals(actor, saved.getUser());
    }

    @Test
    void logEntityAction_WithBlankEntityType_ShouldStoreNullEntityType() {
        auditService.logEntityAction(null, "UPDATE", "   ", 8, "Background status update");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertNull(saved.getEntityType());
        assertEquals(8, saved.getEntityId());
    }

    @Test
    void getAuditLogs_WithNullUserId_ShouldReturnAllLogs() {
        AuditLog log1 = new AuditLog("LOGIN", null, null, "User 1 login", null);
        AuditLog log2 = new AuditLog("CREATE", "SOUND", 3, "Created sound", null);
        when(auditLogRepository.findAll()).thenReturn(List.of(log1, log2));

        List<AuditLog> result = auditService.getAuditLogs(null);

        assertEquals(2, result.size());
        verify(auditLogRepository).findAll();
        verify(auditLogRepository, never()).findByUser_UserId(any());
    }

    @Test
    void getAuditLogs_WithValidUserId_ShouldReturnFilteredLogs() {
        AuditLog log = new AuditLog("UPDATE", "IOT_MODULE", 1, "Updated volume", null);
        when(auditLogRepository.findByUser_UserId(7)).thenReturn(List.of(log));

        List<AuditLog> result = auditService.getAuditLogs(7);

        assertEquals(1, result.size());
        assertEquals("UPDATE", result.get(0).getAction());
        verify(auditLogRepository).findByUser_UserId(7);
        verify(auditLogRepository, never()).findAll();
    }

    @Test
    void getAuditLogs_WithNonPositiveUserId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> auditService.getAuditLogs(0));
        assertThrows(IllegalArgumentException.class, () -> auditService.getAuditLogs(-10));

        verify(auditLogRepository, never()).findAll();
        verify(auditLogRepository, never()).findByUser_UserId(any());
    }
}