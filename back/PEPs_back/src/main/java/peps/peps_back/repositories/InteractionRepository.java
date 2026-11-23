package peps.peps_back.repositories;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.Interaction;

/**
 * Spring Data JPA repository for {@link Interaction} entities, mapping to the {@code interaction} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Integer> {

    /**
     * Retrieves all interactions triggered by a given IoT module within a time range.
     *
     * @param moduleId the identifier of the IoT module
     * @param start    the start of the time range (inclusive)
     * @param end      the end of the time range (inclusive)
     * @return the list of matching interactions; empty if none match
     */
    List<Interaction> findByModule_ModuleIdAndTriggeredAtBetween(Integer moduleId, LocalDateTime start, LocalDateTime end);

    /**
     * Retrieves all interactions triggered by modules assigned to a given species.
     *
     * @param speciesId the identifier of the species
     * @return the list of matching interactions; empty if none match
     */
    List<Interaction> findByModule_Species_SpeciesId(Integer speciesId);
}
