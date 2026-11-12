package peps.peps_back.domain;

import java.time.LocalDateTime;
import java.util.Objects;
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
 * Unit tests for the {@link Sound} domain entity.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
class SoundTest {

    private Sound sound;

    @BeforeEach
    void setUp() {
        sound = new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12);
        sound.setSoundId(7);
    }

    @Test
    @DisplayName("Default constructor should initialize all fields to null")
    void testDefaultConstructor() {
        Sound emptySound = new Sound();

        assertNull(emptySound.getSoundId());
        assertNull(emptySound.getName());
        assertNull(emptySound.getSoundType());
        assertNull(emptySound.getExtension());
        assertNull(emptySound.getMinioKey());
        assertNull(emptySound.getDurationSeconds());
        assertNull(emptySound.getCreatedAt());
    }

    @Test
    @DisplayName("Parameterized constructor should assign metadata and MinIO attributes")
    void testParameterizedConstructor() {
        Sound rainSound = new Sound("Jungle Rain", "Ambient", "wav", "sounds/jungle-rain.wav", 45);

        assertNull(rainSound.getSoundId());
        assertEquals("Jungle Rain", rainSound.getName());
        assertEquals("Ambient", rainSound.getSoundType());
        assertEquals("wav", rainSound.getExtension());
        assertEquals("sounds/jungle-rain.wav", rainSound.getMinioKey());
        assertEquals(45, rainSound.getDurationSeconds());
        assertNull(rainSound.getCreatedAt());
    }

    @Test
    @DisplayName("onCreate() should populate createdAt only when it is null")
    void testOnCreateLifecycleCallback() {
        // Branch 1: createdAt is null -> should be initialized automatically
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        sound.onCreate();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(sound.getCreatedAt());
        assertTrue(sound.getCreatedAt().isAfter(before));
        assertTrue(sound.getCreatedAt().isBefore(after));

        // Branch 2: createdAt is already set -> should preserve the explicit value
        LocalDateTime explicitTimestamp = LocalDateTime.of(2026, 5, 10, 14, 30, 0);
        sound.setCreatedAt(explicitTimestamp);
        sound.onCreate();

        assertEquals(explicitTimestamp, sound.getCreatedAt());
    }

    @Test
    @DisplayName("Getters and setters should read and update all entity attributes")
    void testGettersAndSetters() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 12, 10, 0, 0);
        sound.setCreatedAt(now);

        assertEquals(7, sound.getSoundId());
        assertEquals("Macaw Call", sound.getName());
        assertEquals("Vocalization", sound.getSoundType());
        assertEquals("mp3", sound.getExtension());
        assertEquals("sounds/macaw-call.mp3", sound.getMinioKey());
        assertEquals(12, sound.getDurationSeconds());
        assertEquals(now, sound.getCreatedAt());

        LocalDateTime updatedTime = LocalDateTime.of(2026, 11, 12, 12, 0, 0);
        sound.setSoundId(8);
        sound.setName("Waterfall");
        sound.setSoundType("Nature");
        sound.setExtension("ogg");
        sound.setMinioKey("sounds/waterfall.ogg");
        sound.setDurationSeconds(30);
        sound.setCreatedAt(updatedTime);

        assertEquals(8, sound.getSoundId());
        assertEquals("Waterfall", sound.getName());
        assertEquals("Nature", sound.getSoundType());
        assertEquals("ogg", sound.getExtension());
        assertEquals("sounds/waterfall.ogg", sound.getMinioKey());
        assertEquals(30, sound.getDurationSeconds());
        assertEquals(updatedTime, sound.getCreatedAt());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(sound.equals(sound));

        // Null and different class comparisons
        assertFalse(sound.equals(null));
        assertFalse(sound.equals("Macaw Call"));

        // Different instance with the same ID
        Sound sameIdSound = new Sound("Other", "Other", "wav", "other.wav", 5);
        sameIdSound.setSoundId(7);
        assertTrue(sound.equals(sameIdSound));

        // Different instance with a different ID
        Sound differentIdSound = new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12);
        differentIdSound.setSoundId(99);
        assertFalse(sound.equals(differentIdSound));

        // Both instances transient (null IDs)
        Sound transientOne = new Sound();
        Sound transientTwo = new Sound();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with soundId")
    void testHashCode() {
        assertEquals(Objects.hash(7), sound.hashCode());

        Sound sameIdSound = new Sound();
        sameIdSound.setSoundId(7);
        assertEquals(sound.hashCode(), sameIdSound.hashCode());

        Sound differentIdSound = new Sound();
        differentIdSound.setSoundId(15);
        assertNotEquals(sound.hashCode(), differentIdSound.hashCode());
    }

    @Test
    @DisplayName("toString() should format core sound attributes without errors")
    void testToString() {
        String expected = "Sound{soundId=7, name='Macaw Call', soundType='Vocalization', " +
                "extension='mp3', minioKey='sounds/macaw-call.mp3'}";
        assertEquals(expected, sound.toString());

        Sound transientSound = new Sound();
        assertEquals(
                "Sound{soundId=null, name='null', soundType='null', extension='null', minioKey='null'}",
                transientSound.toString()
        );
    }
}