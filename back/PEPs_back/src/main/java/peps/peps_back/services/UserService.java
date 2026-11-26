package peps.peps_back.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Species;
import peps.peps_back.domain.User;
import peps.peps_back.repositories.SpeciesRepository;
import peps.peps_back.repositories.UserRepository;
import peps.peps_back.security.JwtUtil;

/**
 * Service layer responsible for user authentication, account management,
 * and species-level access resolution within the PEP'S platform.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SOIGNEUR = "ROLE_SOIGNEUR";
    public static final String ROLE_CHERCHEUR = "ROLE_CHERCHEUR";

    private static final Set<String> ALLOWED_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SOIGNEUR,
            ROLE_CHERCHEUR
    );

    private final UserRepository userRepository;
    private final SpeciesRepository speciesRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a {@code UserService} using BCrypt password hashing.
     *
     * @param userRepository    repository for {@link User} persistence
     * @param speciesRepository repository for {@link Species} persistence
     */
    @Autowired
    public UserService(UserRepository userRepository, SpeciesRepository speciesRepository) {
        this(userRepository, speciesRepository, new BCryptPasswordEncoder());
    }

    /**
     * Package-private constructor used for injecting a custom or mock {@link PasswordEncoder}
     * in unit tests.
     *
     * @param userRepository    repository for {@link User} persistence
     * @param speciesRepository repository for {@link Species} persistence
     * @param passwordEncoder   password encoder used for BCrypt verification and hashing
     */
    UserService(
            UserRepository userRepository,
            SpeciesRepository speciesRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.speciesRepository = speciesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user with raw credentials and generates a signed JWT token.
     *
     * @param login    the unique username
     * @param password the raw plain-text password
     * @return a signed JWT token string
     * @throws IllegalArgumentException if {@code login} or {@code password} is null or blank
     * @throws BadCredentialsException  if the user does not exist, is inactive, or the password does not match
     */
    @Transactional(readOnly = true)
    public String authenticate(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login must not be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or blank");
        }

        String normalizedLogin = login.trim();
        User user = userRepository.findByLogin(normalizedLogin)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials or inactive user account"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            LOGGER.warn("Authentication rejected for disabled account '{}'", normalizedLogin);
            throw new BadCredentialsException("Invalid credentials or inactive user account");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            LOGGER.warn("Authentication failed due to password mismatch for user '{}'", normalizedLogin);
            throw new BadCredentialsException("Invalid credentials or inactive user account");
        }

        long userId = user.getUserId() != null ? user.getUserId() : 0L;
        String token = JwtUtil.generateToken(userId, user.getLogin(), user.getRole());

        LOGGER.info("User '{}' authenticated successfully with role '{}'", user.getLogin(), user.getRole());
        return token;
    }

    /**
     * Retrieves all registered users in the system.
     *
     * @return a list of all {@link User} entities
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Creates and persists a new user account with a BCrypt-hashed password.
     *
     * @param user the {@link User} entity to register; {@code passwordHash} may hold the raw password
     * @return the persisted {@link User} entity
     * @throws IllegalArgumentException if required fields are invalid, the role is unsupported,
     *                                  or the login already exists
     */
    @Transactional
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new IllegalArgumentException("User login must not be null or blank");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("User password must not be null or blank");
        }
        if (user.getRole() == null || !ALLOWED_ROLES.contains(user.getRole())) {
            throw new IllegalArgumentException("Unsupported user role: " + user.getRole());
        }

        String normalizedLogin = user.getLogin().trim();
        if (userRepository.existsByLogin(normalizedLogin)) {
            throw new IllegalArgumentException("Login already exists: " + normalizedLogin);
        }

        user.setLogin(normalizedLogin);
        if (!isBcryptHash(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }

        User savedUser = userRepository.save(user);
        LOGGER.info("Created new user '{}' with role '{}'", savedUser.getLogin(), savedUser.getRole());
        return savedUser;
    }

    /**
     * Resolves the list of animal species accessible to a given user.
     * Administrators receive global access to all species, whereas caretakers and
     * researchers receive only their assigned species.
     *
     * @param userId the primary identifier of the user
     * @return a list of authorized {@link Species} entities
     * @throws IllegalArgumentException if {@code userId} is null or no matching user exists
     */
    @Transactional(readOnly = true)
    public List<Species> getUserSpecies(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (ROLE_ADMIN.equals(user.getRole())) {
            return speciesRepository.findAll();
        }

        return new ArrayList<>(user.getSupervisedSpecies());
    }

    /**
     * Checks whether a string already follows the standard BCrypt modular crypt format.
     */
    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}