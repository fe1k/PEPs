package peps.peps_back.repositories;

import java.time.LocalDateTime;
import java.util.List;

import peps.peps_back.domain.ObservationMeasurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author mariapazmunoz version 2.0
 */

@Repository
public interface ObservationMeasurementRepository extends JpaRepository<ObservationMeasurement, Integer> {

    List<ObservationMeasurement> findBySpecies_SpeciesIdAndMeasurementType(Integer speciesId, String type);

    List<ObservationMeasurement> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);

}
