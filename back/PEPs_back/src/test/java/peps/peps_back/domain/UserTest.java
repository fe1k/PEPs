package peps.peps_back.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link User} domain entity.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("franco.admin", "$2a$10$hashedPasswordExample", "ROLE_ADMIN");
        user.setUserId(1);
    }

    @Test
    @DisplayName("Default constructor should initialize default active state and empty species set")
    void testDefaultConstructor() {
        User emptyUser = new User();

        assertNull(emptyUser.getUserId());
        assertNull(emptyUser.getLogin());
        assertNull(emptyUser.getPasswordHash());
        assertNull(emptyUser.getRole());
        assertTrue(emptyUser.getIsActive());
        assertNull(emptyUser.getCreatedAt());
        assertNull(emptyUser.getUpdatedAt());
        assertNotNull(emptyUser.getSupervisedSpecies());
        assertTrue(emptyUser.getSupervisedSpecies().isEmpty());
    }

    @Test
    @DisplayName("Parameterized constructor should assign credentials, role, and active flag")
    void testParameterizedConstructor() {
        User keeper = new User("soigneur.ara", "$2a$10$anotherHash", "ROLE_SOIGNEUR");

        assertNull(keeper.getUserId());
        assertEquals("soigneur.ara", keeper.getLogin());
        assertEquals("$2a$10$anotherHash", keeper.getPasswordHash());
        assertEquals("ROLE_SOIGNEUR", keeper.getRole());
        assertTrue(keeper.getIsActive());
        assertNull(keeper.getCreatedAt());
        assertNull(keeper.getUpdatedAt());
        assertNotNull(keeper.getSupervisedSpecies());
        assertTrue(keeper.getSupervisedSpecies().isEmpty());
    }

    @Test
    @DisplayName("onCreate() should populate null timestamps and default isActive without overwriting explicit values")
    void testOnCreateLifecycleCallback() {
        // Branch 1: createdAt, updatedAt, and isActive are null -> should be initialized automatically
        User newUser = new User();
        newUser.setIsActive(null);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        newUser.onCreate();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(newUser.getCreatedAt());
        assertNotNull(newUser.getUpdatedAt());
        assertTrue(newUser.getCreatedAt().isAfter(before));
        assertTrue(newUser.getCreatedAt().isBefore(after));
        assertEquals(newUser.getCreatedAt(), newUser.getUpdatedAt());
        assertTrue(newUser.getIsActive());

        // Branch 2: values are already explicitly set -> should preserve them
        LocalDateTime explicitCreated = LocalDateTime.of(2026, 1, 15, 9, 0, 0);
        LocalDateTime explicitUpdated = LocalDateTime.of(2026, 2, 20, 14, 30, 0);
        newUser.setCreatedAt(explicitCreated);
        newUser.setUpdatedAt(explicitUpdated);
        newUser.setIsActive(false);

        newUser.onCreate();

        assertEquals(explicitCreated, newUser.getCreatedAt());
        assertEquals(explicitUpdated, newUser.getUpdatedAt());
        assertFalse(newUser.getIsActive());
    }

    @Test
    @DisplayName("onUpdate() should refresh updatedAt timestamp without altering createdAt")
    void testOnUpdateLifecycleCallback() {
        LocalDateTime initialTime = LocalDateTime.of(2026, 3, 1, 8, 0, 0);
        user.setCreatedAt(initialTime);
        user.setUpdatedAt(initialTime);

        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        user.onUpdate();
        LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);

        assertEquals(initialTime, user.getCreatedAt());
        assertTrue(user.getUpdatedAt().isAfter(beforeUpdate));
        assertTrue(user.getUpdatedAt().isBefore(afterUpdate));
    }

    @Test
    @DisplayName("Getters and setters should read and update all attributes and supervised species")
    void testGettersAndSetters() {
        LocalDateTime created = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 4, 11, 11, 0, 0);

        Species macaws = new Species("Macaws", "Parrots");
        macaws.setSpeciesId(1);
        Species dolphins = new Species("Dolphins", "Marine mammals");
        dolphins.setSpeciesId(3);

        Set<Species> speciesSet = new HashSet<>();
        speciesSet.add(macaws);
        speciesSet.add(dolphins);

        user.setUserId(10);
        user.setLogin("chercheur.etho");
        user.setPasswordHash("$2a$10$updatedHash");
        user.setRole("ROLE_CHERCHEUR");
        user.setIsActive(false);
        user.setCreatedAt(created);
        user.setUpdatedAt(updated);
        user.setSupervisedSpecies(speciesSet);

        assertEquals(10, user.getUserId());
        assertEquals("chercheur.etho", user.getLogin());
        assertEquals("$2a$10$updatedHash", user.getPasswordHash());
        assertEquals("ROLE_CHERCHEUR", user.getRole());
        assertFalse(user.getIsActive());
        assertEquals(created, user.getCreatedAt());
        assertEquals(updated, user.getUpdatedAt());
        assertEquals(2, user.getSupervisedSpecies().size());
        assertTrue(user.getSupervisedSpecies().contains(macaws));
        assertTrue(user.getSupervisedSpecies().contains(dolphins));
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(user.equals(user));

        // Null and different class comparisons
        assertFalse(user.equals(null));
        assertFalse(user.equals("franco.admin"));

        // Different instance with the same ID
        User sameIdUser = new User("other.login", "otherHash", "ROLE_SOIGNEUR");
        sameIdUser.setUserId(1);
        assertTrue(user.equals(sameIdUser));

        // Different instance with a different ID
        User differentIdUser = new User("franco.admin", "$2a$10$hashedPasswordExample", "ROLE_ADMIN");
        differentIdUser.setUserId(2);
        assertFalse(user.equals(differentIdUser));

        // Both instances transient (null IDs)
        User transientOne = new User();
        User transientTwo = new User();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with userId")
    void testHashCode() {
        assertEquals(Objects.hash(1), user.hashCode());

        User sameIdUser = new User();
        sameIdUser.setUserId(1);
        assertEquals(user.hashCode(), sameIdUser.hashCode());

        User differentIdUser = new User();
        differentIdUser.setUserId(77);
        assertNotEquals(user.hashCode(), differentIdUser.hashCode());
    }

    @Test
    @DisplayName("toString() should format core attributes without exposing passwordHash")
    void testToString() {
        String output = user.toString();
        String expected = "User{userId=1, login='franco.admin', role='ROLE_ADMIN', isActive=true}";

        assertEquals(expected, output);
        assertFalse(output.contains("$2a$10$hashedPasswordExample"));

        User transientUser = new User();
        assertEquals("User{userId=null, login='null', role='null', isActive=true}", transientUser.toString());
    }
}