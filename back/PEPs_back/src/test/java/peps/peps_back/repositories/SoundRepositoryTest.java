package peps.peps_back.repositories;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link SoundRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class SoundRepositoryTest {

    @Autowired
    private SoundRepository soundRepository;

    @BeforeEach
    void setUp() {
        soundRepository.save(new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12));
        soundRepository.save(new Sound("Lion Roar", "Vocalization", "wav", "sounds/lion-roar.wav", 8));
        soundRepository.save(new Sound("Jungle Rain", "Ambient", "wav", "sounds/jungle-rain.wav", 45));
    }

    @Test
    @DisplayName("findByMinioKey() should return the sound stored under the given key")
    void testFindByMinioKeyFound() {
        Optional<Sound> result = soundRepository.findByMinioKey("sounds/jungle-rain.wav");

        assertTrue(result.isPresent());
        assertEquals("Jungle Rain", result.get().getName());
    }

    @Test
    @DisplayName("findByMinioKey() should return an empty Optional for an unknown key")
    void testFindByMinioKeyNotFound() {
        assertFalse(soundRepository.findByMinioKey("sounds/unknown.mp3").isPresent());
    }

    @Test
    @DisplayName("findBySoundType() should return only the sounds of the given category")
    void testFindBySoundType() {
        List<Sound> result = soundRepository.findBySoundType("Vocalization");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> "Vocalization".equals(s.getSoundType())));
    }

    @Test
    @DisplayName("findBySoundType() should return an empty list when no sound matches")
    void testFindBySoundTypeNoMatch() {
        assertTrue(soundRepository.findBySoundType("Percussion").isEmpty());
    }
}
