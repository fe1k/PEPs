package peps.peps_back.domain;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Playlist} domain entity.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
class PlaylistTest {

    private Playlist playlist;

    @BeforeEach
    void setUp() {
        playlist = new Playlist("Macaws Morning Enrichment", "RANDOM");
        playlist.setPlaylistId(1);
    }

    @Test
    @DisplayName("Default constructor should initialize all fields to null")
    void testDefaultConstructor() {
        Playlist emptyPlaylist = new Playlist();

        assertNull(emptyPlaylist.getPlaylistId());
        assertNull(emptyPlaylist.getName());
        assertNull(emptyPlaylist.getPlaybackMode());
    }

    @Test
    @DisplayName("Parameterized constructor should assign name and playbackMode")
    void testParameterizedConstructor() {
        Playlist sequentialPlaylist = new Playlist("Afternoon Calm", "SEQUENTIAL");

        assertNull(sequentialPlaylist.getPlaylistId());
        assertEquals("Afternoon Calm", sequentialPlaylist.getName());
        assertEquals("SEQUENTIAL", sequentialPlaylist.getPlaybackMode());
    }

    @Test
    @DisplayName("Getters and setters should read and update all entity attributes")
    void testGettersAndSetters() {
        assertEquals(1, playlist.getPlaylistId());
        assertEquals("Macaws Morning Enrichment", playlist.getName());
        assertEquals("RANDOM", playlist.getPlaybackMode());

        playlist.setPlaylistId(5);
        playlist.setName("Tropical Rain Ambience");
        playlist.setPlaybackMode("SEQUENTIAL");

        assertEquals(5, playlist.getPlaylistId());
        assertEquals("Tropical Rain Ambience", playlist.getName());
        assertEquals("SEQUENTIAL", playlist.getPlaybackMode());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(playlist.equals(playlist));

        // Null and different class comparisons
        assertFalse(playlist.equals(null));
        assertFalse(playlist.equals("Macaws Morning Enrichment"));

        // Different instance with the same ID
        Playlist sameIdPlaylist = new Playlist("Other Playlist", "SEQUENTIAL");
        sameIdPlaylist.setPlaylistId(1);
        assertTrue(playlist.equals(sameIdPlaylist));

        // Different instance with a different ID
        Playlist differentIdPlaylist = new Playlist("Macaws Morning Enrichment", "RANDOM");
        differentIdPlaylist.setPlaylistId(2);
        assertFalse(playlist.equals(differentIdPlaylist));

        // Both instances transient (null IDs)
        Playlist transientOne = new Playlist();
        Playlist transientTwo = new Playlist();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with playlistId")
    void testHashCode() {
        assertEquals(Objects.hash(1), playlist.hashCode());

        Playlist sameIdPlaylist = new Playlist();
        sameIdPlaylist.setPlaylistId(1);
        assertEquals(playlist.hashCode(), sameIdPlaylist.hashCode());

        Playlist differentIdPlaylist = new Playlist();
        differentIdPlaylist.setPlaylistId(42);
        assertNotEquals(playlist.hashCode(), differentIdPlaylist.hashCode());
    }

    @Test
    @DisplayName("toString() should format playlistId, name, and playbackMode without errors")
    void testToString() {
        String expected = "Playlist{playlistId=1, name='Macaws Morning Enrichment', playbackMode='RANDOM'}";
        assertEquals(expected, playlist.toString());

        Playlist transientPlaylist = new Playlist();
        assertEquals("Playlist{playlistId=null, name='null', playbackMode='null'}", transientPlaylist.toString());
    }
}