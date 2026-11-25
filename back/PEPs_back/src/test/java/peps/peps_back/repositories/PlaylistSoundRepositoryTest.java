package peps.peps_back.repositories;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Playlist;
import peps.peps_back.domain.PlaylistSound;
import peps.peps_back.domain.Sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link PlaylistSoundRepository} against an H2 in-memory database.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class PlaylistSoundRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlaylistSoundRepository playlistSoundRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private SoundRepository soundRepository;

    private Playlist morningPlaylist;
    private Playlist nightPlaylist;
    private Sound macawSound;
    private Sound rainSound;
    private Sound waterfallSound;

    @BeforeEach
    void setUp() {
        morningPlaylist = playlistRepository.save(new Playlist("Morning Calls", "SEQUENTIAL"));
        nightPlaylist = playlistRepository.save(new Playlist("Night Ambience", "RANDOM"));

        macawSound = soundRepository.save(
                new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12)
        );
        rainSound = soundRepository.save(
                new Sound("Jungle Rain", "Ambient", "wav", "sounds/jungle-rain.wav", 45)
        );
        waterfallSound = soundRepository.save(
                new Sound("Waterfall", "Nature", "ogg", "sounds/waterfall.ogg", 30)
        );

        // Insert out of playOrder sequence and repeat macawSound at positions 1 and 3
        playlistSoundRepository.save(new PlaylistSound(morningPlaylist, macawSound, 3));
        playlistSoundRepository.save(new PlaylistSound(morningPlaylist, macawSound, 1));
        playlistSoundRepository.save(new PlaylistSound(morningPlaylist, rainSound, 2));

        playlistSoundRepository.save(new PlaylistSound(nightPlaylist, waterfallSound, 1));

        // Force EclipseLink to flush pending INSERTs and populate generated IDs in H2
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByPlaylist_PlaylistIdOrderByPlayOrderAsc() should return entries sorted ascending by playOrder")
    void testFindByPlaylistIdOrderByPlayOrderAsc() {
        List<PlaylistSound> entries = playlistSoundRepository
                .findByPlaylist_PlaylistIdOrderByPlayOrderAsc(morningPlaylist.getPlaylistId());

        assertEquals(3, entries.size());

        assertNotNull(entries.get(0).getPlaylistSoundId());
        assertEquals(1, entries.get(0).getPlayOrder());
        assertEquals(macawSound.getSoundId(), entries.get(0).getSound().getSoundId());

        assertEquals(2, entries.get(1).getPlayOrder());
        assertEquals(rainSound.getSoundId(), entries.get(1).getSound().getSoundId());

        assertEquals(3, entries.get(2).getPlayOrder());
        assertEquals(macawSound.getSoundId(), entries.get(2).getSound().getSoundId());
    }

    @Test
    @DisplayName("findByPlaylist_PlaylistIdOrderByPlayOrderAsc() should return an empty list for an unknown playlist")
    void testFindByPlaylistIdOrderByPlayOrderAscEmpty() {
        List<PlaylistSound> entries = playlistSoundRepository
                .findByPlaylist_PlaylistIdOrderByPlayOrderAsc(99999);

        assertTrue(entries.isEmpty());
    }

    @Test
    @DisplayName("findByPlaylist_PlaylistIdAndSound_SoundId() should return all occurrences of a repeated sound")
    void testFindByPlaylistIdAndSoundIdFoundMultiple() {
        List<PlaylistSound> matches = playlistSoundRepository.findByPlaylist_PlaylistIdAndSound_SoundId(
                morningPlaylist.getPlaylistId(),
                macawSound.getSoundId()
        );

        assertEquals(2, matches.size());
    }

    @Test
    @DisplayName("findByPlaylist_PlaylistIdAndSound_SoundId() should return an empty list when sound is not in playlist")
    void testFindByPlaylistIdAndSoundIdNotFound() {
        List<PlaylistSound> matches = playlistSoundRepository.findByPlaylist_PlaylistIdAndSound_SoundId(
                morningPlaylist.getPlaylistId(),
                waterfallSound.getSoundId()
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("countByPlaylist_PlaylistId() should return the exact number of sound entries in a playlist")
    void testCountByPlaylistId() {
        assertEquals(3L, playlistSoundRepository.countByPlaylist_PlaylistId(morningPlaylist.getPlaylistId()));
        assertEquals(1L, playlistSoundRepository.countByPlaylist_PlaylistId(nightPlaylist.getPlaylistId()));
        assertEquals(0L, playlistSoundRepository.countByPlaylist_PlaylistId(99999));
    }
}