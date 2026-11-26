package peps.peps_back.services;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import peps.peps_back.domain.Species;
import peps.peps_back.domain.User;
import peps.peps_back.repositories.SpeciesRepository;
import peps.peps_back.repositories.UserRepository;
import peps.peps_back.security.JwtUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpeciesRepository speciesRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, speciesRepository, passwordEncoder);
    }

    @Test
    void publicConstructor_ShouldInitializeDependenciesWithoutThrowing() {
        UserService defaultService = new UserService(userRepository, speciesRepository);
        assertNotNull(defaultService);
    }

    @Test
    void authenticate_WithNullOrBlankInputs_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate(null, "secret"));
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate("   ", "secret"));
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate("admin", null));
        assertThrows(IllegalArgumentException.class, () -> userService.authenticate("admin", "   "));

        verify(userRepository, never()).findByLogin(any());
    }

    @Test
    void authenticate_WhenUserNotFound_ShouldThrowBadCredentialsException() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> userService.authenticate("unknown", "password123"));
    }

    @Test
    void authenticate_WhenUserIsInactive_ShouldThrowBadCredentialsException() {
        User inactiveUser = new User("soigneur1", passwordEncoder.encode("secret123"), UserService.ROLE_SOIGNEUR);
        inactiveUser.setUserId(2);
        inactiveUser.setIsActive(false);

        when(userRepository.findByLogin("soigneur1")).thenReturn(Optional.of(inactiveUser));

        assertThrows(BadCredentialsException.class,
                () -> userService.authenticate("soigneur1", "secret123"));
    }

    @Test
    void authenticate_WhenPasswordDoesNotMatch_ShouldThrowBadCredentialsException() {
        User activeUser = new User("soigneur1", passwordEncoder.encode("correctPass"), UserService.ROLE_SOIGNEUR);
        activeUser.setUserId(2);
        activeUser.setIsActive(true);

        when(userRepository.findByLogin("soigneur1")).thenReturn(Optional.of(activeUser));

        assertThrows(BadCredentialsException.class,
                () -> userService.authenticate("soigneur1", "wrongPass"));
    }

    @Test
    void authenticate_WithValidAdminCredentials_ShouldReturnValidJwtWithEditorPermission() {
        User admin = new User("admin", passwordEncoder.encode("adminPass"), UserService.ROLE_ADMIN);
        admin.setUserId(1);
        admin.setIsActive(true);

        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(admin));

        String token = userService.authenticate("  admin  ", "adminPass");

        assertNotNull(token);
        Claims claims = JwtUtil.validateToken(token);
        assertNotNull(claims);
        assertEquals("admin", claims.getSubject());
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals(UserService.ROLE_ADMIN, claims.get("role", String.class));
        assertEquals("editor", claims.get("permission", String.class));
    }

    @Test
    void authenticate_WithValidCaretakerCredentials_ShouldReturnValidJwtWithEditorPermission() {
        User caretaker = new User("soigneur_ara", passwordEncoder.encode("keepPass"), UserService.ROLE_SOIGNEUR);
        caretaker.setUserId(2);
        caretaker.setIsActive(true);

        when(userRepository.findByLogin("soigneur_ara")).thenReturn(Optional.of(caretaker));

        String token = userService.authenticate("soigneur_ara", "keepPass");

        Claims claims = JwtUtil.validateToken(token);
        assertNotNull(claims);
        assertEquals("soigneur_ara", claims.getSubject());
        assertEquals(2L, claims.get("userId", Long.class));
        assertEquals(UserService.ROLE_SOIGNEUR, claims.get("role", String.class));
        assertEquals("editor", claims.get("permission", String.class));
    }

    @Test
    void authenticate_WithValidResearcherCredentials_ShouldReturnValidJwtWithViewerPermission() {
        User researcher = new User("chercheur1", passwordEncoder.encode("resPass"), UserService.ROLE_CHERCHEUR);
        researcher.setUserId(3);
        researcher.setIsActive(true);

        when(userRepository.findByLogin("chercheur1")).thenReturn(Optional.of(researcher));

        String token = userService.authenticate("chercheur1", "resPass");

        Claims claims = JwtUtil.validateToken(token);
        assertNotNull(claims);
        assertEquals("chercheur1", claims.getSubject());
        assertEquals(3L, claims.get("userId", Long.class));
        assertEquals(UserService.ROLE_CHERCHEUR, claims.get("role", String.class));
        assertEquals("viewer", claims.get("permission", String.class));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsersFromRepository() {
        User u1 = new User("admin", "hash1", UserService.ROLE_ADMIN);
        User u2 = new User("soigneur", "hash2", UserService.ROLE_SOIGNEUR);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void createUser_WithInvalidInputs_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(null));

        User blankLogin = new User("   ", "rawPass", UserService.ROLE_SOIGNEUR);
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(blankLogin));

        User blankPassword = new User("newuser", "   ", UserService.ROLE_SOIGNEUR);
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(blankPassword));

        User invalidRole = new User("newuser", "rawPass", "ROLE_INVALID");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(invalidRole));

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WhenLoginAlreadyExists_ShouldThrowIllegalArgumentException() {
        User duplicate = new User("admin", "rawPass", UserService.ROLE_ADMIN);
        when(userRepository.existsByLogin("admin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(duplicate));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithRawPassword_ShouldHashPasswordNormalizeLoginAndSave() {
        User newUser = new User("  soigneur_ara  ", "plainSecret", UserService.ROLE_SOIGNEUR);
        newUser.setIsActive(null);

        when(userRepository.existsByLogin("soigneur_ara")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(newUser);

        assertEquals("soigneur_ara", saved.getLogin());
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("plainSecret", saved.getPasswordHash()));
        assertTrue(saved.getIsActive());
        verify(userRepository).save(newUser);
    }

    @Test
    void createUser_WithPreHashedPassword_ShouldKeepExistingHash() {
        String preHashed = passwordEncoder.encode("alreadyHashed");
        User newUser = new User("chercheur_dauphin", preHashed, UserService.ROLE_CHERCHEUR);

        when(userRepository.existsByLogin("chercheur_dauphin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(newUser);

        assertEquals(preHashed, saved.getPasswordHash());
    }

    @Test
    void getUserSpecies_WithNullOrUnknownId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserSpecies(null));

        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.getUserSpecies(99));
    }

    @Test
    void getUserSpecies_ForAdmin_ShouldReturnAllSpeciesGlobally() {
        User admin = new User("admin", "hash", UserService.ROLE_ADMIN);
        admin.setUserId(1);

        Species macaws = new Species("Macaws", "Aras");
        Species saimiris = new Species("Saimiris", "Saïmiris");
        Species dolphins = new Species("Dolphins", "Dauphins");

        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(speciesRepository.findAll()).thenReturn(List.of(macaws, saimiris, dolphins));

        List<Species> result = userService.getUserSpecies(1);

        assertEquals(3, result.size());
        verify(speciesRepository).findAll();
    }

    @Test
    void getUserSpecies_ForCaretaker_ShouldReturnOnlySupervisedSpecies() {
        Species macaws = new Species("Macaws", "Aras");
        macaws.setSpeciesId(1);

        User caretaker = new User("soigneur_ara", "hash", UserService.ROLE_SOIGNEUR);
        caretaker.setUserId(4);
        caretaker.setSupervisedSpecies(Set.of(macaws));

        when(userRepository.findById(4)).thenReturn(Optional.of(caretaker));

        List<Species> result = userService.getUserSpecies(4);

        assertEquals(1, result.size());
        assertEquals("Macaws", result.get(0).getName());
        verify(speciesRepository, never()).findAll();
    }
}