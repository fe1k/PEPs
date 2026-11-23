package peps.peps_back.repositories;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.ObservationMeasurement;

/**
 * Spring Data JPA repository for {@link ObservationMeasurement} entities, mapping to the
 * {@code observation_measurement} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 1.0
 */
@Repository
public interface ObservationMeasurementRepository extends JpaRepository<ObservationMeasurement, Integer> {

    /**
     * Retrieves all measurements of a given type recorded for a species.
     *
     * @param speciesId the identifier of the observed species
     * @param type      the measurement type to filter by
     * @return the list of matching measurements; empty if none match
     */
    List<ObservationMeasurement> findBySpecies_SpeciesIdAndMeasurementType(Integer speciesId, String type);

    /**
     * Retrieves all measurements recorded within a time range.
     *
     * @param start the start of the time range (inclusive)
     * @param end   the end of the time range (inclusive)
     * @return the list of matching measurements; empty if none match
     */
    List<ObservationMeasurement> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);
}
