package peps.peps_back.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.PlaylistSound;

/**
 * Spring Data JPA repository for {@link PlaylistSound} associative entities,
 * mapping to the {@code playlist_sound} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Repository
public interface PlaylistSoundRepository extends JpaRepository<PlaylistSound, Integer> {

    /**
     * Retrieves all sound entries belonging to a playlist, ordered sequentially
     * by their playback position ({@code playOrder} ascending).
     *
     * @param playlistId the primary key of the target playlist
     * @return an ordered {@link List} of {@link PlaylistSound} entries
     */
    List<PlaylistSound> findByPlaylist_PlaylistIdOrderByPlayOrderAsc(Integer playlistId);

    /**
     * Retrieves all entries matching a specific sound within a given playlist.
     * Returns a list because the same sound may appear multiple times at different positions.
     *
     * @param playlistId the primary key of the target playlist
     * @param soundId    the primary key of the target sound
     * @return a {@link List} of matching {@link PlaylistSound} entries
     */
    List<PlaylistSound> findByPlaylist_PlaylistIdAndSound_SoundId(Integer playlistId, Integer soundId);

    /**
     * Counts how many sound entries currently exist in a given playlist.
     * Useful to compute the next 1-based {@code playOrder} when appending a sound.
     *
     * @param playlistId the primary key of the target playlist
     * @return the number of entries in the playlist
     */
    long countByPlaylist_PlaylistId(Integer playlistId);
}