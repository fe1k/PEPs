package peps.peps_back.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Sound;
import peps.peps_back.repositories.SoundRepository;

/**
 * Service layer responsible for managing acoustic enrichment audio files,
 * coordinating binary persistence in MinIO object storage, metadata persistence
 * in PostgreSQL, and asynchronous job dispatch via Redis Streams.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Service
public class SoundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoundService.class);

    public static final String DEFAULT_SOUND_TYPE = "GENERAL";
    public static final String DEFAULT_EXTENSION = "mp3";

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3",
            "wav",
            "ogg",
            "m4a"
    );

    private final SoundRepository soundRepository;
    private final MinioStorageService minioStorageService;
    private final AudioJobPublisher audioJobPublisher;

    /**
     * Constructs a {@code SoundService} with its required persistence and infrastructure collaborators.
     *
     * @param soundRepository     repository managing {@link Sound} metadata entities
     * @param minioStorageService service managing binary audio objects in MinIO S3 storage
     * @param audioJobPublisher   publisher dispatching asynchronous audio jobs to Redis Streams
     */
    @Autowired
    public SoundService(
            SoundRepository soundRepository,
            MinioStorageService minioStorageService,
            AudioJobPublisher audioJobPublisher) {
        this.soundRepository = soundRepository;
        this.minioStorageService = minioStorageService;
        this.audioJobPublisher = audioJobPublisher;
    }

    /**
     * Retrieves all registered audio records from the database.
     *
     * @return a list of all {@link Sound} entities
     */
    @Transactional(readOnly = true)
    public List<Sound> getAllSounds() {
        return soundRepository.findAll();
    }

    /**
     * Retrieves a single sound entity by its unique primary identifier.
     *
     * @param id the primary key of the target {@link Sound}
     * @return the matching {@link Sound} entity
     * @throws IllegalArgumentException if {@code id} is invalid or no matching sound exists
     */
    @Transactional(readOnly = true)
    public Sound getSoundById(Integer id) {
        validatePositiveId(id, "Sound ID");

        return soundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sound not found with ID: " + id));
    }

    /**
     * Retrieves all sounds belonging to a specific functional category/type.
     *
     * @param soundType the sound category to filter by (e.g., {@code "Vocalization"}, {@code "Ambient"})
     * @return a list of matching {@link Sound} entities
     * @throws IllegalArgumentException if {@code soundType} is null or blank
     */
    @Transactional(readOnly = true)
    public List<Sound> getSoundsByType(String soundType) {
        if (soundType == null || soundType.isBlank()) {
            throw new IllegalArgumentException("Sound type must not be null or blank");
        }
        return soundRepository.findBySoundType(soundType.trim());
    }

    /**
     * Downloads the raw binary content of an existing sound from MinIO object storage.
     *
     * @param id the primary key of the target {@link Sound}
     * @return raw audio bytes stored in MinIO
     * @throws IllegalArgumentException if {@code id} is invalid or the sound does not exist
     * @throws IllegalStateException    if downloading from MinIO fails
     */
    @Transactional(readOnly = true)
    public byte[] downloadSoundFile(Integer id) {
        Sound sound = getSoundById(id);
        try {
            return minioStorageService.downloadSound(sound.getMinioKey());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to download audio file from MinIO for sound ID: " + id, e);
        }
    }

    /**
     * Registers a new audio file using default classification ({@code GENERAL}) and format ({@code mp3}),
     * uploading the binary payload to MinIO, persisting metadata in PostgreSQL, and publishing
     * an asynchronous processing job to Redis Streams.
     *
     * @param name      logical display name of the sound
     * @param fileBytes raw binary content of the audio file
     * @return the newly persisted {@link Sound} entity
     * @throws IllegalArgumentException if {@code name} or {@code fileBytes} is invalid
     */
    @Transactional
    public Sound registerNewSound(String name, byte[] fileBytes) {
        return registerNewSound(name, DEFAULT_SOUND_TYPE, DEFAULT_EXTENSION, null, fileBytes);
    }

    /**
     * Registers a new audio file with explicit classification, extension, and optional duration.
     *
     * If metadata persistence or Redis job publication fails after MinIO upload succeeds,
     * a compensating deletion is executed in MinIO to prevent orphaned binary objects.
     *
     * @param name            logical display name of the sound
     * @param soundType       functional category (defaults to {@code "GENERAL"} when null or blank)
     * @param extension       audio format extension ({@code mp3}, {@code wav}, {@code ogg}, {@code m4a};
     *                        defaults to {@code "mp3"} when null or blank)
     * @param durationSeconds optional audio duration in seconds (must be non-negative if provided)
     * @param fileBytes       raw binary content of the audio file
     * @return the newly persisted {@link Sound} entity
     * @throws IllegalArgumentException if any argument fails validation
     * @throws IllegalStateException    if MinIO upload, database persistence, or Redis publishing fails
     */
    @Transactional
    public Sound registerNewSound(
            String name,
            String soundType,
            String extension,
            Integer durationSeconds,
            byte[] fileBytes) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Sound name must not be null or blank");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Audio file content must not be null or empty");
        }
        if (durationSeconds != null && durationSeconds < 0) {
            throw new IllegalArgumentException("Audio duration must not be negative");
        }

        String normalizedName = name.trim();
        String resolvedType = (soundType == null || soundType.isBlank())
                ? DEFAULT_SOUND_TYPE
                : soundType.trim();

        String normalizedExtension = (extension == null || extension.isBlank())
                ? DEFAULT_EXTENSION
                : extension.replaceFirst("^\\.+", "").trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("Unsupported audio extension: " + extension);
        }

        String contentType = resolveContentType(normalizedExtension);
        String minioKey;
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            minioKey = minioStorageService.uploadSound(
                    normalizedName,
                    resolvedType,
                    normalizedExtension,
                    contentType,
                    inputStream,
                    fileBytes.length
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload audio file to MinIO: " + normalizedName, e);
        }

        try {
            Sound sound = new Sound(
                    normalizedName,
                    resolvedType,
                    normalizedExtension,
                    minioKey,
                    durationSeconds
            );
            Sound savedSound = soundRepository.save(sound);

            String jobId = UUID.randomUUID().toString();
            String audioBase64 = Base64.getEncoder().encodeToString(fileBytes);

            audioJobPublisher.publishUploadJob(
                    jobId,
                    savedSound.getSoundId(),
                    savedSound.getName(),
                    audioBase64,
                    contentType
            );

            LOGGER.info("Registered new sound '{}' (id={}, ext='{}', minioKey='{}', jobId='{}')",
                    savedSound.getName(), savedSound.getSoundId(), normalizedExtension, minioKey, jobId);
            return savedSound;
        } catch (RuntimeException e) {
            LOGGER.warn("Rolling back MinIO object '{}' due to registration failure: {}",
                    minioKey, e.getMessage());
            try {
                minioStorageService.deleteSound(minioKey);
            } catch (Exception cleanupEx) {
                LOGGER.error("Failed to delete orphaned MinIO object '{}': {}",
                        minioKey, cleanupEx.getMessage());
            }
            throw e;
        }
    }

    /**
     * Triggers an asynchronous device retrieval and transcoding job
     * for an existing sound so an ESP32 module can fetch prepared PCM audio.
     *
     * @param soundId the primary key of the target {@link Sound}
     * @return the generated correlation {@code jobId} used to poll Redis
     * @throws IllegalArgumentException if {@code soundId} is invalid or the sound does not exist
     */
    @Transactional(readOnly = true)
    public String requestDeviceAudioPreparation(Integer soundId) {
        Sound sound = getSoundById(soundId);
        String jobId = UUID.randomUUID().toString();
        audioJobPublisher.publishRetrievalJob(jobId, sound.getSoundId());
        LOGGER.info("Requested device audio preparation for sound '{}' (id={}, jobId='{}')",
                sound.getName(), sound.getSoundId(), jobId);
        return jobId;
    }

    /**
     * Deletes an existing sound from both MinIO object storage and the PostgreSQL database.
     *
     * @param id the primary key of the {@link Sound} to delete
     * @throws IllegalArgumentException if {@code id} is invalid or the sound does not exist
     * @throws IllegalStateException    if deleting the object from MinIO fails
     */
    @Transactional
    public void deleteSound(Integer id) {
        validatePositiveId(id, "Sound ID");

        Sound sound = soundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sound not found with ID: " + id));

        try {
            minioStorageService.deleteSound(sound.getMinioKey());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to delete audio file from MinIO: " + sound.getMinioKey(), e);
        }

        soundRepository.delete(sound);

        LOGGER.info("Deleted sound '{}' (id={}, minioKey='{}')",
                sound.getName(), sound.getSoundId(), sound.getMinioKey());
    }

    /**
     * Resolves the MIME content type for validated audio extensions.
     */
    private String resolveContentType(String normalizedExtension) {
        return switch (normalizedExtension) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            default -> "audio/mp4";
        };
    }

    /**
     * Validates that an entity identifier is non-null and strictly positive.
     */
    private void validatePositiveId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer");
        }
    }
}