package peps.peps_back.repositories;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.ObservationMeasurement;
import peps.peps_back.domain.Species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link ObservationMeasurementRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class ObservationMeasurementRepositoryTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 11, 20, 10, 0);

    @Autowired
    private ObservationMeasurementRepository measurementRepository;

    @Autowired
    private SpeciesRepository speciesRepository;

    private Species ara;
    private Species lion;

    @BeforeEach
    void setUp() {
        // saveAndFlush(): EclipseLink only runs the INSERT (and assigns the IDENTITY id) on flush,
        // so a plain save() would leave the id null when it is used below.
        ara = speciesRepository.saveAndFlush(new Species("Ara", "Blue-and-yellow macaw"));
        lion = speciesRepository.saveAndFlush(new Species("Lion", "African lion"));

        saveMeasurement("TEMPERATURE", 24.5, ara, BASE_TIME);
        saveMeasurement("TEMPERATURE", 25.0, ara, BASE_TIME.plusHours(1));
        saveMeasurement("HUMIDITY", 60.0, ara, BASE_TIME.plusHours(2));
        saveMeasurement("TEMPERATURE", 30.0, lion, BASE_TIME.plusDays(1));
    }

    private void saveMeasurement(String type, Double value, Species species, LocalDateTime recordedAt) {
        ObservationMeasurement measurement = new ObservationMeasurement("MANUAL", type, value, null, null, species);
        measurement.setRecordedAt(recordedAt);
        measurementRepository.save(measurement);
    }

    @Test
    @DisplayName("findBySpecies_SpeciesIdAndMeasurementType() should filter on both species and type")
    void testFindBySpeciesAndType() {
        List<ObservationMeasurement> result =
                measurementRepository.findBySpecies_SpeciesIdAndMeasurementType(ara.getSpeciesId(), "TEMPERATURE");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> "TEMPERATURE".equals(m.getMeasurementType())
                && m.getSpecies().getSpeciesId().equals(ara.getSpeciesId())));
    }

    @Test
    @DisplayName("findBySpecies_SpeciesIdAndMeasurementType() should return an empty list when no measurement matches")
    void testFindBySpeciesAndTypeNoMatch() {
        assertTrue(measurementRepository
                .findBySpecies_SpeciesIdAndMeasurementType(lion.getSpeciesId(), "HUMIDITY").isEmpty());
    }

    @Test
    @DisplayName("findByRecordedAtBetween() should return the measurements recorded within the range, boundaries included")
    void testFindByRecordedAtBetween() {
        List<ObservationMeasurement> result =
                measurementRepository.findByRecordedAtBetween(BASE_TIME, BASE_TIME.plusHours(2));

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("findByRecordedAtBetween() should return an empty list outside the range")
    void testFindByRecordedAtBetweenNoMatch() {
        assertTrue(measurementRepository
                .findByRecordedAtBetween(BASE_TIME.minusDays(5), BASE_TIME.minusDays(4)).isEmpty());
    }
}
