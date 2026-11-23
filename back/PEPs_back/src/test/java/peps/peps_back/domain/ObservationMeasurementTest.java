package peps.peps_back.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link ObservationMeasurement} domain entity.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
class ObservationMeasurementTest {

    private Species dolphins;
    private User caretaker;
    private IoTModule poolModule;
    private ObservationMeasurement measurement;

    @BeforeEach
    void setUp() {
        dolphins = new Species("Dolphins", "Marine mammal lagoon telemetry and behavior");
        dolphins.setSpeciesId(3);

        caretaker = new User("soigneur.dauphin", "$2a$10$hashedPass", "ROLE_SOIGNEUR");
        caretaker.setUserId(4);

        poolModule = new IoTModule("Lagoon-Sensor-01", "10.0.0.25", "ONLINE", 100, "AUTOMATIC", dolphins);
        poolModule.setModuleId(5);

        measurement = new ObservationMeasurement(
                "EXTERNAL_API",
                "WATER_TEMP",
                21.5,
                "Normal lagoon temperature",
                "{\"sensor\":\"probe-A\",\"unit\":\"C\"}",
                dolphins
        );
        measurement.setObservationId(50);
    }

    @Test
    @DisplayName("Default constructor should initialize all fields and relationships to null")
    void testDefaultConstructor() {
        ObservationMeasurement emptyMeasurement = new ObservationMeasurement();

        assertNull(emptyMeasurement.getObservationId());
        assertNull(emptyMeasurement.getRecordedAt());
        assertNull(emptyMeasurement.getDataSource());
        assertNull(emptyMeasurement.getMeasurementType());
        assertNull(emptyMeasurement.getNumericValue());
        assertNull(emptyMeasurement.getTextValue());
        assertNull(emptyMeasurement.getJsonData());
        assertNull(emptyMeasurement.getSpecies());
        assertNull(emptyMeasurement.getUser());
        assertNull(emptyMeasurement.getModule());
    }

    @Test
    @DisplayName("Parameterized constructor should assign payload fields and species association")
    void testParameterizedConstructor() {
        ObservationMeasurement manualEntry = new ObservationMeasurement(
                "MANUAL",
                "BEHAVIOR_RATING",
                4.0,
                "High exploratory activity",
                null,
                dolphins
        );

        assertNull(manualEntry.getObservationId());
        assertNull(manualEntry.getRecordedAt());
        assertEquals("MANUAL", manualEntry.getDataSource());
        assertEquals("BEHAVIOR_RATING", manualEntry.getMeasurementType());
        assertEquals(4.0, manualEntry.getNumericValue());
        assertEquals("High exploratory activity", manualEntry.getTextValue());
        assertNull(manualEntry.getJsonData());
        assertSame(dolphins, manualEntry.getSpecies());
        assertNull(manualEntry.getUser());
        assertNull(manualEntry.getModule());
    }

    @Test
    @DisplayName("onCreate() should populate recordedAt only when it is null")
    void testOnCreateLifecycleCallback() {
        // Branch 1: recordedAt is null -> should be initialized automatically
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        measurement.onCreate();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(measurement.getRecordedAt());
        assertTrue(measurement.getRecordedAt().isAfter(before));
        assertTrue(measurement.getRecordedAt().isBefore(after));

        // Branch 2: recordedAt is already set -> should preserve the explicit timestamp
        LocalDateTime explicitTimestamp = LocalDateTime.of(2026, 11, 12, 16, 40, 0);
        measurement.setRecordedAt(explicitTimestamp);
        measurement.onCreate();

        assertEquals(explicitTimestamp, measurement.getRecordedAt());
    }

    @Test
    @DisplayName("Getters and setters should read and update all attributes and relationships")
    void testGettersAndSetters() {
        LocalDateTime recorded = LocalDateTime.of(2026, 11, 12, 14, 0, 0);
        measurement.setRecordedAt(recorded);
        measurement.setModule(poolModule);

        assertEquals(50, measurement.getObservationId());
        assertEquals(recorded, measurement.getRecordedAt());
        assertEquals("EXTERNAL_API", measurement.getDataSource());
        assertEquals("WATER_TEMP", measurement.getMeasurementType());
        assertEquals(21.5, measurement.getNumericValue());
        assertEquals("Normal lagoon temperature", measurement.getTextValue());
        assertEquals("{\"sensor\":\"probe-A\",\"unit\":\"C\"}", measurement.getJsonData());
        assertSame(dolphins, measurement.getSpecies());
        assertNull(measurement.getUser());
        assertSame(poolModule, measurement.getModule());

        Species saimiris = new Species("Saimiris", "Primate enclosure");
        saimiris.setSpeciesId(2);
        LocalDateTime updatedRecorded = LocalDateTime.of(2026, 11, 12, 17, 15, 0);

        measurement.setObservationId(51);
        measurement.setRecordedAt(updatedRecorded);
        measurement.setDataSource("MANUAL");
        measurement.setMeasurementType("FEEDING_NOTES");
        measurement.setNumericValue(null);
        measurement.setTextValue("All individuals ate normally");
        measurement.setJsonData(null);
        measurement.setSpecies(saimiris);
        measurement.setUser(caretaker);
        measurement.setModule(null);

        assertEquals(51, measurement.getObservationId());
        assertEquals(updatedRecorded, measurement.getRecordedAt());
        assertEquals("MANUAL", measurement.getDataSource());
        assertEquals("FEEDING_NOTES", measurement.getMeasurementType());
        assertNull(measurement.getNumericValue());
        assertEquals("All individuals ate normally", measurement.getTextValue());
        assertNull(measurement.getJsonData());
        assertSame(saimiris, measurement.getSpecies());
        assertSame(caretaker, measurement.getUser());
        assertNull(measurement.getModule());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(measurement.equals(measurement));

        // Null and different class comparisons
        assertFalse(measurement.equals(null));
        assertFalse(measurement.equals("WATER_TEMP"));

        // Different instance with the same ID
        ObservationMeasurement sameIdMeasurement = new ObservationMeasurement();
        sameIdMeasurement.setObservationId(50);
        assertTrue(measurement.equals(sameIdMeasurement));

        // Different instance with a different ID
        ObservationMeasurement differentIdMeasurement = new ObservationMeasurement();
        differentIdMeasurement.setObservationId(99);
        assertFalse(measurement.equals(differentIdMeasurement));

        // Both instances transient (null IDs)
        ObservationMeasurement transientOne = new ObservationMeasurement();
        ObservationMeasurement transientTwo = new ObservationMeasurement();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with observationId")
    void testHashCode() {
        assertEquals(Objects.hash(50), measurement.hashCode());

        ObservationMeasurement sameIdMeasurement = new ObservationMeasurement();
        sameIdMeasurement.setObservationId(50);
        assertEquals(measurement.hashCode(), sameIdMeasurement.hashCode());

        ObservationMeasurement differentIdMeasurement = new ObservationMeasurement();
        differentIdMeasurement.setObservationId(123);
        assertNotEquals(measurement.hashCode(), differentIdMeasurement.hashCode());
    }

    @Test
    @DisplayName("toString() should format metadata safely without dumping large payloads or lazy associations")
    void testToString() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 11, 12, 18, 30, 0);
        measurement.setRecordedAt(fixedTime);

        String output = measurement.toString();
        String expected = "ObservationMeasurement{observationId=50, dataSource='EXTERNAL_API', " +
                "measurementType='WATER_TEMP', recordedAt=2026-11-12T18:30}";

        assertEquals(expected, output);
        assertFalse(output.contains("probe-A"));

        ObservationMeasurement transientMeasurement = new ObservationMeasurement();
        assertEquals(
                "ObservationMeasurement{observationId=null, dataSource='null', measurementType='null', recordedAt=null}",
                transientMeasurement.toString()
        );
    }
}