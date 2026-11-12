package peps.peps_back.domain;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link PlaylistSound} associative domain entity.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
class PlaylistSoundTest {

    private Playlist playlist;
    private Sound sound;
    private PlaylistSound playlistSound;

    @BeforeEach
    void setUp() {
        playlist = new Playlist("Macaws Morning Enrichment", "SEQUENTIAL");
        playlist.setPlaylistId(1);

        sound = new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12);
        sound.setSoundId(7);

        playlistSound = new PlaylistSound(playlist, sound, 1);
        playlistSound.setPlaylistSoundId(10);
    }

    @Test
    @DisplayName("Default constructor should initialize relationships to null and playOrder to 1")
    void testDefaultConstructor() {
        PlaylistSound emptyEntry = new PlaylistSound();

        assertNull(emptyEntry.getPlaylistSoundId());
        assertNull(emptyEntry.getPlaylist());
        assertNull(emptyEntry.getSound());
        assertEquals(1, emptyEntry.getPlayOrder());
    }

    @Test
    @DisplayName("Parameterized constructor should assign playlist, sound, and playOrder")
    void testParameterizedConstructor() {
        PlaylistSound secondEntry = new PlaylistSound(playlist, sound, 2);

        assertNull(secondEntry.getPlaylistSoundId());
        assertSame(playlist, secondEntry.getPlaylist());
        assertSame(sound, secondEntry.getSound());
        assertEquals(2, secondEntry.getPlayOrder());
    }

    @Test
    @DisplayName("Getters and setters should read and update all attributes and relationships")
    void testGettersAndSetters() {
        assertEquals(10, playlistSound.getPlaylistSoundId());
        assertSame(playlist, playlistSound.getPlaylist());
        assertSame(sound, playlistSound.getSound());
        assertEquals(1, playlistSound.getPlayOrder());

        Playlist updatedPlaylist = new Playlist("Afternoon Calm", "RANDOM");
        updatedPlaylist.setPlaylistId(2);

        Sound updatedSound = new Sound("Rainforest Ambience", "Ambient", "wav", "sounds/rain.wav", 60);
        updatedSound.setSoundId(9);

        playlistSound.setPlaylistSoundId(25);
        playlistSound.setPlaylist(updatedPlaylist);
        playlistSound.setSound(updatedSound);
        playlistSound.setPlayOrder(4);

        assertEquals(25, playlistSound.getPlaylistSoundId());
        assertSame(updatedPlaylist, playlistSound.getPlaylist());
        assertSame(updatedSound, playlistSound.getSound());
        assertEquals(4, playlistSound.getPlayOrder());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(playlistSound.equals(playlistSound));

        // Null and different class comparisons
        assertFalse(playlistSound.equals(null));
        assertFalse(playlistSound.equals("PlaylistSound"));

        // Different instance with the same ID
        PlaylistSound sameIdEntry = new PlaylistSound();
        sameIdEntry.setPlaylistSoundId(10);
        assertTrue(playlistSound.equals(sameIdEntry));

        // Different instance with a different ID
        PlaylistSound differentIdEntry = new PlaylistSound(playlist, sound, 1);
        differentIdEntry.setPlaylistSoundId(99);
        assertFalse(playlistSound.equals(differentIdEntry));

        // Both instances transient (null IDs)
        PlaylistSound transientOne = new PlaylistSound();
        PlaylistSound transientTwo = new PlaylistSound();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with playlistSoundId")
    void testHashCode() {
        assertEquals(Objects.hash(10), playlistSound.hashCode());

        PlaylistSound sameIdEntry = new PlaylistSound();
        sameIdEntry.setPlaylistSoundId(10);
        assertEquals(playlistSound.hashCode(), sameIdEntry.hashCode());

        PlaylistSound differentIdEntry = new PlaylistSound();
        differentIdEntry.setPlaylistSoundId(55);
        assertNotEquals(playlistSound.hashCode(), differentIdEntry.hashCode());
    }

    @Test
    @DisplayName("toString() should format playlistSoundId and playOrder safely")
    void testToString() {
        String expected = "PlaylistSound{playlistSoundId=10, playOrder=1}";
        assertEquals(expected, playlistSound.toString());

        PlaylistSound transientEntry = new PlaylistSound();
        assertEquals("PlaylistSound{playlistSoundId=null, playOrder=1}", transientEntry.toString());
    }
}