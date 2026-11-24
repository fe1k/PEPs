package peps.peps_back.repositories;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Playlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link PlaylistRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class PlaylistRepositoryTest {

    @Autowired
    private PlaylistRepository playlistRepository;

    @BeforeEach
    void setUp() {
        playlistRepository.save(new Playlist("Morning Calls", "SEQUENTIAL"));
        playlistRepository.save(new Playlist("Night Ambience", "RANDOM"));
    }

    @Test
    @DisplayName("findByName() should return the playlist with the given name")
    void testFindByNameFound() {
        Optional<Playlist> result = playlistRepository.findByName("Night Ambience");

        assertTrue(result.isPresent());
        assertEquals("RANDOM", result.get().getPlaybackMode());
    }

    @Test
    @DisplayName("findByName() should return an empty Optional for an unknown name")
    void testFindByNameNotFound() {
        assertFalse(playlistRepository.findByName("Unknown").isPresent());
    }
}
