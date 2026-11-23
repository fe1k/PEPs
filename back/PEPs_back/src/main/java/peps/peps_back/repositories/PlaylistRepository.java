package peps.peps_back.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.Playlist;

/**
 * Spring Data JPA repository for {@link Playlist} entities, mapping to the {@code playlist} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {

    /**
     * Retrieves a playlist by its name.
     *
     * @param name the name of the playlist to find
     * @return an {@link Optional} containing the matching playlist, or empty if none exists
     */
    public Optional<Playlist> findByName(String name);
}
