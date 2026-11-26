package peps.peps_back.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.repositories.IoTModuleRepository;

/**
 * Service layer responsible for managing physical IoT enrichment modules,
 * including inventory queries, audio/playback configuration updates, and
 * device heartbeat tracking within the PEP'S platform.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Service
public class IoTModuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IoTModuleService.class);

    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_ERROR = "ERROR";

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 100;

    public static final Set<String> ALLOWED_MODES = Set.of(
            "MANUAL",
            "RANDOM",
            "PROGRAMMED",
            "SEQUENTIAL"
    );

    private final IoTModuleRepository moduleRepository;

    /**
     * Constructs an {@code IoTModuleService} with its required persistence repository.
     *
     * @param moduleRepository repository managing {@link IoTModule} entities
     */
    @Autowired
    public IoTModuleService(IoTModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    /**
     * Retrieves all registered IoT modules in the system.
     *
     * @return a list of all {@link IoTModule} entities
     */
    @Transactional(readOnly = true)
    public List<IoTModule> getAllModules() {
        return moduleRepository.findAll();
    }

    /**
     * Retrieves a single IoT module by its unique primary identifier.
     *
     * @param id the primary key of the target {@link IoTModule}
     * @return the matching {@link IoTModule} entity
     * @throws IllegalArgumentException if {@code id} is null/non-positive or no matching module exists
     */
    @Transactional(readOnly = true)
    public IoTModule getModuleById(Integer id) {
        validateModuleId(id);

        return moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("IoT module not found with ID: " + id));
    }

    /**
     * Updates the audio volume and playback operating mode of an existing IoT module.
     *
     * @param id     the primary key of the {@link IoTModule} to configure
     * @param volume the target audio output volume (must be between {@code 0} and {@code 100})
     * @param mode   the target playback operating mode (e.g., {@code "MANUAL"}, {@code "RANDOM"},
     *               {@code "PROGRAMMED"}, {@code "SEQUENTIAL"})
     * @return the updated and persisted {@link IoTModule} entity
     * @throws IllegalArgumentException if any argument is invalid or the module does not exist
     */
    @Transactional
    public IoTModule updateModuleConfig(Integer id, Integer volume, String mode) {
        validateModuleId(id);

        if (volume == null || volume < MIN_VOLUME || volume > MAX_VOLUME) {
            throw new IllegalArgumentException(
                    "Volume must be between " + MIN_VOLUME + " and " + MAX_VOLUME);
        }
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Playback mode must not be null or blank");
        }

        String normalizedMode = mode.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_MODES.contains(normalizedMode)) {
            throw new IllegalArgumentException("Unsupported playback mode: " + mode);
        }

        IoTModule module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("IoT module not found with ID: " + id));

        module.setVolume(volume);
        module.setCurrentMode(normalizedMode);

        IoTModule savedModule = moduleRepository.save(module);
        LOGGER.info("Updated configuration for IoT module '{}' (id={}): volume={}, mode='{}'",
                savedModule.getName(), savedModule.getModuleId(), volume, normalizedMode);
        return savedModule;
    }

    /**
     * Records a connectivity heartbeat from a physical IoT module, updating its
     * {@code lastSeenAt} timestamp to the current time and marking its status as {@code ONLINE}.
     *
     * @param moduleId the primary key of the reporting {@link IoTModule}
     * @throws IllegalArgumentException if {@code moduleId} is null/non-positive or the module does not exist
     */
    @Transactional
    public void registerHeartbeat(Integer moduleId) {
        validateModuleId(moduleId);

        IoTModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("IoT module not found with ID: " + moduleId));

        module.setLastSeenAt(LocalDateTime.now());
        module.setStatus(STATUS_ONLINE);

        moduleRepository.save(module);
        LOGGER.debug("Registered heartbeat for IoT module '{}' (id={})",
                module.getName(), module.getModuleId());
    }

    /**
     * Validates that a module identifier is non-null and strictly positive.
     *
     * @param id the module identifier to validate
     * @throws IllegalArgumentException if {@code id} is null or less than or equal to zero
     */
    private void validateModuleId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Module ID must be a positive integer");
        }
    }
}