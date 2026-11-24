package peps.peps_back.repositories;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.AuditLog;
import peps.peps_back.domain.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link AuditLogRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private User keeper;

    @BeforeEach
    void setUp() {
        // saveAndFlush(): EclipseLink only runs the INSERT (and assigns the IDENTITY id) on flush,
        // so a plain save() would leave the id null when it is used below.
        admin = userRepository.saveAndFlush(new User("admin", "hashed-password-1", "ADMIN"));
        keeper = userRepository.saveAndFlush(new User("keeper.anna", "hashed-password-2", "KEEPER"));

        auditLogRepository.save(new AuditLog("LOGIN", "User", admin.getUserId(), null, admin));
        auditLogRepository.save(new AuditLog("CREATE", "Sound", 7, "Uploaded macaw-call.mp3", admin));
        auditLogRepository.save(new AuditLog("LOGIN", "User", keeper.getUserId(), null, keeper));
        auditLogRepository.save(new AuditLog("CLEANUP", "System", null, "Automated task", null));
    }

    @Test
    @DisplayName("findByUser_UserId() should return only the entries performed by the given user")
    void testFindByUserId() {
        List<AuditLog> result = auditLogRepository.findByUser_UserId(admin.getUserId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(log -> log.getUser().getUserId().equals(admin.getUserId())));
    }

    @Test
    @DisplayName("findByUser_UserId() should return an empty list for a user without entries")
    void testFindByUserIdNoMatch() {
        User newcomer = userRepository.saveAndFlush(new User("newcomer", "hashed-password-3", "KEEPER"));

        assertTrue(auditLogRepository.findByUser_UserId(newcomer.getUserId()).isEmpty());
    }
}
