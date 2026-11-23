package peps.peps_back.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.Species;

/**
 * Spring Data JPA repository for {@link Species} entities, mapping to the {@code species} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data at runtime.
 *
 * @author María Paz Muñoz
 * @version 1.0
 */
@Repository
public interface SpeciesRepository extends JpaRepository<Species, Integer> {

    /**
     * Retrieves a species by its unique name.
     * <p>
     * An explicit JPQL query is used because the entity attribute is {@code name},
     * which does not match the derived method name {@code speciesName}.
     *
     * @param speciesName the name of the species to find
     * @return an {@link Optional} containing the matching species, or empty if none exists
     */
    @Query("SELECT s FROM Species s WHERE s.name = :speciesName")
    public Optional<Species> findBySpeciesName(@Param("speciesName") String speciesName);
}
