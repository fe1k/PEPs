package peps.peps_back.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.ObservationMeasurement;
import peps.peps_back.domain.Species;
import peps.peps_back.domain.User;
import peps.peps_back.repositories.ObservationMeasurementRepository;
import peps.peps_back.repositories.SpeciesRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ObservationMeasurementService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class ObservationMeasurementServiceTest {

    @Mock
    private ObservationMeasurementRepository observationRepository;

    @Mock
    private SpeciesRepository speciesRepository;

    @InjectMocks
    private ObservationMeasurementService observationService;

    private Species saimiris;
    private Species dolphins;
    private ObservationMeasurement manualObservation;
    private ObservationMeasurement waterTelemetry;

    @BeforeEach
    void setUp() {
        saimiris = new Species("Saimiris", "Squirrel monkey behavioral enrichment zone");
        saimiris.setSpeciesId(2);

        dolphins = new Species("Dolphins", "Marine lagoon acoustic and telemetry zone");
        dolphins.setSpeciesId(3);

        manualObservation = new ObservationMeasurement(
                "MANUAL",
                "BEHAVIOR_RATING",
                4.5,
                "Active foraging near wooden puzzle box",
                null,
                saimiris);
        manualObservation.setObservationId(10);
        manualObservation.setRecordedAt(LocalDateTime.of(2026, 11, 25, 9, 45));

        waterTelemetry = new ObservationMeasurement(
                "EXTERNAL_API",
                "WATER_TEMP",
                21.8,
                null,
                "{\"salinity\": 35.2, \"ph\": 7.9}",
                dolphins);
        waterTelemetry.setObservationId(11);
        waterTelemetry.setRecordedAt(LocalDateTime.of(2026, 11, 25, 11, 0));
    }

    @Test
    @DisplayName("ingestMeasurement() should resolve species by ID, normalize fields, and persist")
    void testIngestMeasurementBySpeciesIdSuccess() {
        Species speciesRef = new Species();
        speciesRef.setSpeciesId(2);

        User caretakerRef = new User();
        caretakerRef.setUserId(5);

        ObservationMeasurement incoming = new ObservationMeasurement(
                "  manual  ",
                "  FORAGING_DURATION  ",
                18.5,
                "  Calm group interaction  ",
                null,
                speciesRef);
        incoming.setUser(caretakerRef);

        when(speciesRepository.findById(2)).thenReturn(Optional.of(saimiris));
        when(observationRepository.save(any(ObservationMeasurement.class))).thenAnswer(invocation -> {
            ObservationMeasurement saved = invocation.getArgument(0);
            saved.setObservationId(50);
            return saved;
        });

        ObservationMeasurement result = observationService.ingestMeasurement(incoming);

        assertEquals(50, result.getObservationId());
        assertEquals("MANUAL", result.getDataSource());
        assertEquals("FORAGING_DURATION", result.getMeasurementType());
        assertEquals(18.5, result.getNumericValue());
        assertEquals("Calm group interaction", result.getTextValue());
        assertSame(saimiris, result.getSpecies());
        assertNotNull(result.getRecordedAt());
        verify(observationRepository).save(incoming);
    }

    @Test
    @DisplayName("ingestMeasurement() should resolve species by name and preserve explicit timestamp")
    void testIngestMeasurementBySpeciesNameSuccess() {
        Species speciesByNameRef = new Species("  Dolphins  ", null);
        LocalDateTime explicitTimestamp = LocalDateTime.of(2026, 11, 24, 16, 30);

        ObservationMeasurement incoming = new ObservationMeasurement(
                "EXTERNAL_API",
                "WATER_QUALITY",
                null,
                null,
                "  {\"turbidity\": 1.2, \"oxygen\": 8.4}  ",
                speciesByNameRef);
        incoming.setRecordedAt(explicitTimestamp);

        when(speciesRepository.findByName("Dolphins")).thenReturn(Optional.of(dolphins));
        when(observationRepository.save(any(ObservationMeasurement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ObservationMeasurement result = observationService.ingestMeasurement(incoming);

        assertSame(dolphins, result.getSpecies());
        assertEquals(explicitTimestamp, result.getRecordedAt());
        assertEquals("{\"turbidity\": 1.2, \"oxygen\": 8.4}", result.getJsonData());
        verify(speciesRepository, never()).findById(any());
        verify(observationRepository).save(incoming);
    }

    @Test
    @DisplayName("ingestMeasurement() should normalize blank text/JSON to null when numeric value is present")
    void testIngestMeasurementNormalizesBlankOptionalFields() {
        Species speciesRef = new Species();
        speciesRef.setSpeciesId(3);

        ObservationMeasurement incoming = new ObservationMeasurement(
                "IOT_MODULE",
                "ACOUSTIC_DB",
                64.2,
                "   ",
                "   ",
                speciesRef);

        when(speciesRepository.findById(3)).thenReturn(Optional.of(dolphins));
        when(observationRepository.save(any(ObservationMeasurement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ObservationMeasurement result = observationService.ingestMeasurement(incoming);

        assertEquals(64.2, result.getNumericValue());
        assertNull(result.getTextValue());
        assertNull(result.getJsonData());
        verify(observationRepository).save(incoming);
    }

    @Test
    @DisplayName("ingestMeasurement() should reject null entity or invalid dataSource/measurementType")
    void testIngestMeasurementInvalidMetadata() {
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(null));

        ObservationMeasurement blankSource = new ObservationMeasurement(
                "   ", "WATER_TEMP", 20.0, null, null, dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(blankSource));

        ObservationMeasurement unknownSource = new ObservationMeasurement(
                "CSV_UPLOAD", "WATER_TEMP", 20.0, null, null, dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(unknownSource));

        ObservationMeasurement blankType = new ObservationMeasurement(
                "MANUAL", "   ", 20.0, null, null, dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(blankType));

        String overlyLongType = "M".repeat(ObservationMeasurementService.MAX_MEASUREMENT_TYPE_LENGTH + 1);
        ObservationMeasurement tooLongType = new ObservationMeasurement(
                "MANUAL", overlyLongType, 20.0, null, null, dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(tooLongType));

        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingestMeasurement() should reject empty, non-finite numeric, or malformed JSON payloads")
    void testIngestMeasurementInvalidValues() {
        ObservationMeasurement emptyValues = new ObservationMeasurement(
                "MANUAL", "BEHAVIOR", null, "   ", "   ", saimiris);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(emptyValues));

        ObservationMeasurement nanValue = new ObservationMeasurement(
                "EXTERNAL_API", "WATER_TEMP", Double.NaN, null, null, dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(nanValue));

        ObservationMeasurement infiniteValue = new ObservationMeasurement(
                "EXTERNAL_API", "WATER_TEMP", Double.POSITIVE_INFINITY, null, null, dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(infiniteValue));

        ObservationMeasurement invalidJson = new ObservationMeasurement(
                "EXTERNAL_API", "TELEMETRY", null, null, "not-valid-json", dolphins);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(invalidJson));

        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingestMeasurement() should reject missing, transient, or unknown species")
    void testIngestMeasurementSpeciesValidationAndNotFound() {
        ObservationMeasurement nullSpecies = new ObservationMeasurement(
                "MANUAL", "BEHAVIOR", 3.0, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(nullSpecies));

        Species emptySpeciesRef = new Species();
        ObservationMeasurement transientSpecies = new ObservationMeasurement(
                "MANUAL", "BEHAVIOR", 3.0, null, null, emptySpeciesRef);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(transientSpecies));

        Species invalidIdSpecies = new Species();
        invalidIdSpecies.setSpeciesId(0);
        ObservationMeasurement zeroSpeciesId = new ObservationMeasurement(
                "MANUAL", "BEHAVIOR", 3.0, null, null, invalidIdSpecies);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(zeroSpeciesId));

        Species unknownIdSpecies = new Species();
        unknownIdSpecies.setSpeciesId(99);
        when(speciesRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(
                new ObservationMeasurement("MANUAL", "BEHAVIOR", 3.0, null, null, unknownIdSpecies)));

        Species unknownNameSpecies = new Species("Penguins", null);
        when(speciesRepository.findByName("Penguins")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(
                new ObservationMeasurement("EXTERNAL_API", "WATER_TEMP", 12.0, null, null, unknownNameSpecies)));

        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("ingestMeasurement() should reject transient user or module references without valid IDs")
    void testIngestMeasurementInvalidOptionalAssociations() {
        User transientUser = new User();
        ObservationMeasurement withTransientUser = new ObservationMeasurement(
                "MANUAL", "BEHAVIOR", 4.0, null, null, saimiris);
        withTransientUser.setUser(transientUser);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(withTransientUser));

        IoTModule transientModule = new IoTModule();
        transientModule.setModuleId(-1);
        ObservationMeasurement withInvalidModule = new ObservationMeasurement(
                "IOT_MODULE", "WATER_TEMP", 22.0, null, null, dolphins);
        withInvalidModule.setModule(transientModule);
        assertThrows(IllegalArgumentException.class, () -> observationService.ingestMeasurement(withInvalidModule));

        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMeasurementsBySpecies() should return measurements filtered by species ID")
    void testGetMeasurementsBySpeciesSuccess() {
        when(observationRepository.findAll()).thenReturn(List.of(manualObservation, waterTelemetry));

        List<ObservationMeasurement> dolphinResults = observationService.getMeasurementsBySpecies(3);

        assertEquals(1, dolphinResults.size());
        assertSame(waterTelemetry, dolphinResults.get(0));
        verify(observationRepository).findAll();
    }

    @Test
    @DisplayName("getMeasurementsBySpecies() should reject null or non-positive species ID")
    void testGetMeasurementsBySpeciesInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> observationService.getMeasurementsBySpecies(null));
        assertThrows(IllegalArgumentException.class, () -> observationService.getMeasurementsBySpecies(0));
        assertThrows(IllegalArgumentException.class, () -> observationService.getMeasurementsBySpecies(-4));

        verify(observationRepository, never()).findAll();
    }

    @Test
    @DisplayName("getMeasurementsBySpeciesAndType() should delegate to repository with trimmed type")
    void testGetMeasurementsBySpeciesAndTypeSuccess() {
        when(observationRepository.findBySpecies_SpeciesIdAndMeasurementType(3, "WATER_TEMP"))
                .thenReturn(List.of(waterTelemetry));

        List<ObservationMeasurement> results =
                observationService.getMeasurementsBySpeciesAndType(3, "  WATER_TEMP  ");

        assertEquals(1, results.size());
        assertSame(waterTelemetry, results.get(0));
        verify(observationRepository).findBySpecies_SpeciesIdAndMeasurementType(3, "WATER_TEMP");
    }

    @Test
    @DisplayName("getMeasurementsBySpeciesAndType() should reject invalid species ID or measurement type")
    void testGetMeasurementsBySpeciesAndTypeValidationFailures() {
        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsBySpeciesAndType(null, "WATER_TEMP"));
        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsBySpeciesAndType(0, "WATER_TEMP"));
        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsBySpeciesAndType(3, null));
        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsBySpeciesAndType(3, "   "));

        verify(observationRepository, never())
                .findBySpecies_SpeciesIdAndMeasurementType(any(), any());
    }

    @Test
    @DisplayName("getMeasurementsByPeriod() should query repository for valid date window")
    void testGetMeasurementsByPeriodSuccess() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 18, 0);

        when(observationRepository.findByRecordedAtBetween(start, end))
                .thenReturn(List.of(manualObservation, waterTelemetry));

        List<ObservationMeasurement> results = observationService.getMeasurementsByPeriod(start, end);

        assertEquals(2, results.size());
        verify(observationRepository).findByRecordedAtBetween(start, end);
    }

    @Test
    @DisplayName("getMeasurementsByPeriod() should reject null or inverted date bounds")
    void testGetMeasurementsByPeriodValidationFailures() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 15, 0);

        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsByPeriod(null, end));
        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsByPeriod(start, null));
        assertThrows(IllegalArgumentException.class,
                () -> observationService.getMeasurementsByPeriod(end, start));

        verify(observationRepository, never()).findByRecordedAtBetween(any(), any());
    }
}