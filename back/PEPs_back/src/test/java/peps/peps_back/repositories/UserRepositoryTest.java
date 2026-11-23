package peps.peps_back.repositories;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link UserRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("keeper.anna", "hashed-password-1", "KEEPER"));
        userRepository.save(new User("admin", "hashed-password-2", "ADMIN"));
    }

    @Test
    @DisplayName("findByLogin() should return the user with the given login")
    void testFindByLoginFound() {
        Optional<User> result = userRepository.findByLogin("keeper.anna");

        assertTrue(result.isPresent());
        assertEquals("KEEPER", result.get().getRole());
    }

    @Test
    @DisplayName("findByLogin() should return an empty Optional for an unknown login")
    void testFindByLoginNotFound() {
        assertFalse(userRepository.findByLogin("unknown").isPresent());
    }

    @Test
    @DisplayName("existsByLogin() should return true only for an existing login")
    void testExistsByLogin() {
        assertTrue(userRepository.existsByLogin("admin"));
        assertFalse(userRepository.existsByLogin("unknown"));
    }
}
