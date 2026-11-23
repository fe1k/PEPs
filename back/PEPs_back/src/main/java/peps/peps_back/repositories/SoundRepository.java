package peps.peps_back.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.Sound;

/**
 * Spring Data JPA repository for {@link Sound} entities, mapping to the {@code sound} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@Repository
public interface SoundRepository extends JpaRepository<Sound, Integer> {

    /**
     * Retrieves a sound by the object key referencing its audio file in MinIO storage.
     *
     * @param minioKey the MinIO object key of the sound
     * @return an {@link Optional} containing the matching sound, or empty if none exists
     */
    public Optional<Sound> findByMinioKey(String minioKey);

    /**
     * Retrieves all sounds belonging to the given category.
     *
     * @param soundType the functional category of the sounds to find
     * @return the list of matching sounds; empty if none match
     */
    public List<Sound> findBySoundType(String soundType);
}
