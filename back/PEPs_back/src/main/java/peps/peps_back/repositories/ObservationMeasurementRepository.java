package peps.peps_back.repositories;

import java.util.List;

import peps.peps_back.domain.ObservationMeasurement;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author mariapazmunoz version 2.0
 */

public interface ObservationMeasurementRepository extends JpaRepository<Module, Integer> {

    List<ObservationMeasurement> findBySpecies_SpeciesIdAndMeasurementType(Integer speciesId, String type);

    List<ObservationMeasurement> findByRecordedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

}
