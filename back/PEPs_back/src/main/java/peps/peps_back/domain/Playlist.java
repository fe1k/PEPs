package peps.peps_back.domain;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents an audio enrichment playlist grouping multiple sounds with a
 * configured playback strategy (e.g., RANDOM, SEQUENTIAL) and maps to the
 * {@code playlist} table.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Entity
@Table(name = "playlist")
public class Playlist implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the playlist.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_playlist")
    private Integer playlistId;

    /**
     * Display name of the playlist (maximum 255 characters).
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Playback mode strategy for the playlist (e.g., RANDOM, SEQUENTIAL).
     */
    @Column(name = "playback_mode", nullable = false, length = 255)
    private String playbackMode;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public Playlist() {
    }

    /**
     * Constructs a new {@code Playlist} instance with the specified name and playback mode.
     *
     * @param name         the display name of the playlist; must not be {@code null}
     * @param playbackMode the playback strategy (e.g., {@code "RANDOM"} or {@code "SEQUENTIAL"})
     */
    public Playlist(String name, String playbackMode) {
        this.name = name;
        this.playbackMode = playbackMode;
    }

    /**
     * Returns the unique identifier of this playlist.
     *
     * @return the primary key {@code playlistId}, or {@code null} if not yet persisted
     */
    public Integer getPlaylistId() {
        return playlistId;
    }

    /**
     * Sets the unique identifier of this playlist.
     *
     * @param playlistId the primary key to assign
     */
    public void setPlaylistId(Integer playlistId) {
        this.playlistId = playlistId;
    }

    /**
     * Returns the display name of this playlist.
     *
     * @return the playlist name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of this playlist.
     *
     * @param name the playlist name to set (maximum 255 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the playback mode configured for this playlist.
     *
     * @return the playback mode string
     */
    public String getPlaybackMode() {
        return playbackMode;
    }

    /**
     * Sets the playback mode for this playlist.
     *
     * @param playbackMode the playback mode string to assign
     */
    public void setPlaybackMode(String playbackMode) {
        this.playbackMode = playbackMode;
    }

    /**
     * Compares this playlist to another object for equality based on {@code playlistId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted playlist; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Playlist playlist = (Playlist) o;
        return Objects.equals(playlistId, playlist.playlistId);
    }

    /**
     * Returns a hash code value for this playlist based on {@code playlistId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(playlistId);
    }

    /**
     * Returns a string representation of this {@code Playlist} entity.
     *
     * @return a formatted string containing the playlist ID, name, and playback mode
     */
    @Override
    public String toString() {
        return "Playlist{" +
                "playlistId=" + playlistId +
                ", name='" + name + '\'' +
                ", playbackMode='" + playbackMode + '\'' +
                '}';
    }
}