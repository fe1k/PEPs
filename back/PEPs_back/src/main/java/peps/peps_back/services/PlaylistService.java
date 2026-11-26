package peps.peps_back.services;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.Playlist;
import peps.peps_back.domain.PlaylistSound;
import peps.peps_back.domain.Sound;
import peps.peps_back.repositories.IoTModuleRepository;
import peps.peps_back.repositories.PlaylistRepository;
import peps.peps_back.repositories.PlaylistSoundRepository;
import peps.peps_back.repositories.SoundRepository;

/**
 * Service layer responsible for managing audio enrichment playlists, associating
 * sounds in playback order, and assigning playlists to physical IoT modules.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Service
public class PlaylistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistService.class);

    public static final String MODE_SEQUENTIAL = "SEQUENTIAL";
    public static final String MODE_RANDOM = "RANDOM";

    public static final Set<String> ALLOWED_PLAYBACK_MODES = Set.of(
            MODE_SEQUENTIAL,
            MODE_RANDOM
    );

    private final PlaylistRepository playlistRepository;
    private final PlaylistSoundRepository playlistSoundRepository;
    private final SoundRepository soundRepository;
    private final IoTModuleRepository moduleRepository;

    /**
     * Constructs a {@code PlaylistService} with its required persistence repositories.
     *
     * @param playlistRepository      repository managing {@link Playlist} entities
     * @param playlistSoundRepository repository managing {@link PlaylistSound} associations
     * @param soundRepository         repository managing {@link Sound} entities
     * @param moduleRepository        repository managing {@link IoTModule} entities
     */
    @Autowired
    public PlaylistService(
            PlaylistRepository playlistRepository,
            PlaylistSoundRepository playlistSoundRepository,
            SoundRepository soundRepository,
            IoTModuleRepository moduleRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistSoundRepository = playlistSoundRepository;
        this.soundRepository = soundRepository;
        this.moduleRepository = moduleRepository;
    }

    /**
     * Retrieves all configured playlists in the system.
     *
     * @return a list of all {@link Playlist} entities
     */
    @Transactional(readOnly = true)
    public List<Playlist> getAllPlaylists() {
        return playlistRepository.findAll();
    }

    /**
     * Retrieves a single playlist by its unique primary identifier.
     *
     * @param playlistId the primary key of the target {@link Playlist}
     * @return the matching {@link Playlist} entity
     * @throws IllegalArgumentException if {@code playlistId} is invalid or not found
     */
    @Transactional(readOnly = true)
    public Playlist getPlaylistById(Integer playlistId) {
        validatePositiveId(playlistId, "Playlist ID");

        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with ID: " + playlistId));
    }

    /**
     * Retrieves all sound associations belonging to a playlist ordered by {@code playOrder} ascending.
     *
     * @param playlistId the primary key of the target {@link Playlist}
     * @return an ordered list of {@link PlaylistSound} entries
     * @throws IllegalArgumentException if {@code playlistId} is invalid or the playlist does not exist
     */
    @Transactional(readOnly = true)
    public List<PlaylistSound> getPlaylistSounds(Integer playlistId) {
        validatePositiveId(playlistId, "Playlist ID");

        if (!playlistRepository.existsById(playlistId)) {
            throw new IllegalArgumentException("Playlist not found with ID: " + playlistId);
        }

        return playlistSoundRepository.findByPlaylist_PlaylistIdOrderByPlayOrderAsc(playlistId);
    }

    /**
     * Creates and persists a new playlist with the given name and default {@code SEQUENTIAL} playback mode.
     *
     * @param name the unique display name of the playlist
     * @return the newly persisted {@link Playlist} entity
     * @throws IllegalArgumentException if {@code name} is null, blank, or already exists
     */
    @Transactional
    public Playlist createPlaylist(String name) {
        return createPlaylist(name, MODE_SEQUENTIAL);
    }

    /**
     * Creates and persists a new playlist with the given name and playback mode.
     *
     * @param name         the unique display name of the playlist
     * @param playbackMode the playback strategy ({@code "SEQUENTIAL"} or {@code "RANDOM"});
     *                     defaults to {@code "SEQUENTIAL"} when null or blank
     * @return the newly persisted {@link Playlist} entity
     * @throws IllegalArgumentException if {@code name} is invalid/duplicate or {@code playbackMode} is unsupported
     */
    @Transactional
    public Playlist createPlaylist(String name, String playbackMode) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name must not be null or blank");
        }

        String normalizedName = name.trim();
        if (playlistRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Playlist already exists with name: " + normalizedName);
        }

        String normalizedMode = (playbackMode == null || playbackMode.isBlank())
                ? MODE_SEQUENTIAL
                : playbackMode.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_PLAYBACK_MODES.contains(normalizedMode)) {
            throw new IllegalArgumentException("Unsupported playback mode: " + playbackMode);
        }

        Playlist playlist = new Playlist(normalizedName, normalizedMode);
        Playlist savedPlaylist = playlistRepository.save(playlist);
        LOGGER.info("Created playlist '{}' (id={}, mode='{}')",
                savedPlaylist.getName(), savedPlaylist.getPlaylistId(), savedPlaylist.getPlaybackMode());
        return savedPlaylist;
    }

    /**
     * Appends an existing audio file to the end of a playlist using a 1-based sequential {@code playOrder}.
     *
     * @param playlistId the primary key of the target {@link Playlist}
     * @param soundId    the primary key of the {@link Sound} to append
     * @throws IllegalArgumentException if either ID is invalid or does not exist
     */
    @Transactional
    public void addSoundToPlaylist(Integer playlistId, Integer soundId) {
        validatePositiveId(playlistId, "Playlist ID");
        validatePositiveId(soundId, "Sound ID");

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with ID: " + playlistId));

        Sound sound = soundRepository.findById(soundId)
                .orElseThrow(() -> new IllegalArgumentException("Sound not found with ID: " + soundId));

        int nextOrder = (int) playlistSoundRepository.countByPlaylist_PlaylistId(playlistId) + 1;
        PlaylistSound entry = new PlaylistSound(playlist, sound, nextOrder);
        playlistSoundRepository.save(entry);

        LOGGER.info("Added sound '{}' (id={}) to playlist '{}' (id={}) at position {}",
                sound.getName(), sound.getSoundId(), playlist.getName(), playlist.getPlaylistId(), nextOrder);
    }

    /**
     * Assigns a playlist to a physical IoT module, or unassigns the current playlist
     * when {@code playlistId} is {@code null}.
     *
     * @param moduleId   the primary key of the target {@link IoTModule}
     * @param playlistId the primary key of the {@link Playlist} to assign, or {@code null} to clear assignment
     * @throws IllegalArgumentException if {@code moduleId} is invalid/not found, or if {@code playlistId}
     *                                  is non-null and invalid/not found
     */
    @Transactional
    public void assignPlaylistToModule(Integer moduleId, Integer playlistId) {
        validatePositiveId(moduleId, "Module ID");

        IoTModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("IoT module not found with ID: " + moduleId));

        if (playlistId == null) {
            module.setPlaylist(null);
            moduleRepository.save(module);
            LOGGER.info("Unassigned playlist from IoT module '{}' (id={})",
                    module.getName(), module.getModuleId());
            return;
        }

        validatePositiveId(playlistId, "Playlist ID");
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found with ID: " + playlistId));

        module.setPlaylist(playlist);
        moduleRepository.save(module);

        LOGGER.info("Assigned playlist '{}' (id={}) to IoT module '{}' (id={})",
                playlist.getName(), playlist.getPlaylistId(), module.getName(), module.getModuleId());
    }

    /**
     * Validates that an entity identifier is non-null and strictly positive.
     *
     * @param id        the identifier to validate
     * @param fieldName human-readable parameter label for error reporting
     * @throws IllegalArgumentException if {@code id} is null or less than or equal to zero
     */
    private void validatePositiveId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer");
        }
    }
}