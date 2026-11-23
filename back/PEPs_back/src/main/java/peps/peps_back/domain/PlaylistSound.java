package peps.peps_back.domain;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Associative entity linking a {@link Playlist} with a {@link Sound} and defining
 * its playback sequence position, mapping to the {@code peps.playlist_sound} table.
 *
 * @author Franco Valdebenito
 * @version 1.1
 */
@Entity
@Table(
        name = "playlist_sound",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_playlist", "play_order"})
)
public class PlaylistSound implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique surrogate primary identifier of the playlist-sound association.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_playlist_sound")
    private Integer playlistSoundId;

    /**
     * Parent playlist containing this sound entry.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_playlist", nullable = false)
    private Playlist playlist;

    /**
     * Referenced audio file included in the playlist.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sound", nullable = false)
    private Sound sound;

    /**
     * Sequential playback order index of the sound within the playlist (1-based).
     */
    @Column(name = "play_order", nullable = false)
    private Integer playOrder = 1;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public PlaylistSound() {
    }

    /**
     * Constructs a new {@code PlaylistSound} entry associating a sound with a playlist
     * at a specific playback order.
     *
     * @param playlist  the target playlist; must not be {@code null}
     * @param sound     the audio file to include; must not be {@code null}
     * @param playOrder the playback position within the playlist
     */
    public PlaylistSound(Playlist playlist, Sound sound, Integer playOrder) {
        this.playlist = playlist;
        this.sound = sound;
        this.playOrder = playOrder;
    }

    /**
     * Returns the unique identifier of this playlist-sound association.
     *
     * @return the primary key {@code playlistSoundId}, or {@code null} if not yet persisted
     */
    public Integer getPlaylistSoundId() {
        return playlistSoundId;
    }

    /**
     * Sets the unique identifier of this playlist-sound association.
     *
     * @param playlistSoundId the primary key to assign
     */
    public void setPlaylistSoundId(Integer playlistSoundId) {
        this.playlistSoundId = playlistSoundId;
    }

    /**
     * Returns the playlist associated with this entry.
     *
     * @return the parent {@code Playlist}
     */
    public Playlist getPlaylist() {
        return playlist;
    }

    /**
     * Sets the playlist associated with this entry.
     *
     * @param playlist the parent {@code Playlist} to assign
     */
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    /**
     * Returns the sound referenced by this entry.
     *
     * @return the associated {@code Sound}
     */
    public Sound getSound() {
        return sound;
    }

    /**
     * Sets the sound referenced by this entry.
     *
     * @param sound the {@code Sound} to assign
     */
    public void setSound(Sound sound) {
        this.sound = sound;
    }

    /**
     * Returns the playback order position of the sound inside the playlist.
     *
     * @return the playback order index
     */
    public Integer getPlayOrder() {
        return playOrder;
    }

    /**
     * Sets the playback order position of the sound inside the playlist.
     *
     * @param playOrder the playback order index to assign
     */
    public void setPlayOrder(Integer playOrder) {
        this.playOrder = playOrder;
    }

    /**
     * Compares this entry to another object for equality based on {@code playlistSoundId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted entry; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlaylistSound that = (PlaylistSound) o;
        return Objects.equals(playlistSoundId, that.playlistSoundId);
    }

    /**
     * Returns a hash code value for this entry based on {@code playlistSoundId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(playlistSoundId);
    }

    /**
     * Returns a string representation of this {@code PlaylistSound} entity without
     * triggering lazy-loaded associations.
     *
     * @return a formatted string containing the association ID and play order
     */
    @Override
    public String toString() {
        return "PlaylistSound{" +
                "playlistSoundId=" + playlistSoundId +
                ", playOrder=" + playOrder +
                '}';
    }
}