package peps.peps_back.services;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import peps.peps_back.domain.Interaction;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.ObservationMeasurement;
import peps.peps_back.domain.Sound;
import peps.peps_back.domain.Species;
import peps.peps_back.repositories.InteractionRepository;
import peps.peps_back.repositories.ObservationMeasurementRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**g
 * Unit tests for {@link ExportService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private ObservationMeasurementRepository observationRepository;

    @InjectMocks
    private ExportService exportService;

    private Species macaws;
    private Species dolphins;
    private IoTModule araModule;
    private Sound macawSound;

    @BeforeEach
    void setUp() {
        macaws = new Species("Macaws", "Interactive audio enrichment zone");
        macaws.setSpeciesId(1);

        dolphins = new Species("Dolphins", "Marine lagoon zone");
        dolphins.setSpeciesId(3);

        araModule = new IoTModule("ESP32-Ara-01", "192.168.1.50", "ONLINE", 80, "RANDOM", macaws);
        araModule.setModuleId(10);

        macawSound = new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw.mp3", 14);
        macawSound.setSoundId(7);
    }

    @Test
    @DisplayName("exportCsvReport() should filter by speciesId, merge records chronologically, and escape RFC 4180 values")
    void testExportCsvReportFilteredBySpeciesSuccess() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 18, 0);

        Interaction inRangeInteraction = new Interaction("ROPE_PULL", araModule, macawSound);
        inRangeInteraction.setInteractionId(101);
        inRangeInteraction.setTriggeredAt(LocalDateTime.of(2026, 11, 25, 10, 30));

        Interaction outOfRangeInteraction = new Interaction("BUTTON", araModule, null);
        outOfRangeInteraction.setInteractionId(102);
        outOfRangeInteraction.setTriggeredAt(LocalDateTime.of(2026, 11, 25, 19, 0));

        Interaction nullTimestampInteraction = new Interaction("SENSOR", araModule, null);
        nullTimestampInteraction.setInteractionId(103);
        nullTimestampInteraction.setTriggeredAt(null);

        ObservationMeasurement macawObservation = new ObservationMeasurement(
                "MANUAL",
                "BEHAVIOR_NOTE",
                4.0,
                "Pulled rope, said \"hello\"",
                "{\"intensity\": 3, \"zone\": \"north\"}",
                macaws);
        macawObservation.setObservationId(201);
        macawObservation.setRecordedAt(LocalDateTime.of(2026, 11, 25, 9, 15));

        ObservationMeasurement dolphinObservation = new ObservationMeasurement(
                "EXTERNAL_API",
                "WATER_TEMP",
                21.5,
                null,
                null,
                dolphins);
        dolphinObservation.setObservationId(202);
        dolphinObservation.setRecordedAt(LocalDateTime.of(2026, 11, 25, 11, 0));

        ObservationMeasurement nullSpeciesObservation = new ObservationMeasurement(
                "MANUAL",
                "NOTE",
                null,
                "Orphan record",
                null,
                null);
        nullSpeciesObservation.setObservationId(203);
        nullSpeciesObservation.setRecordedAt(LocalDateTime.of(2026, 11, 25, 12, 0));

        when(interactionRepository.findByModule_Species_SpeciesId(1))
                .thenReturn(List.of(inRangeInteraction, outOfRangeInteraction, nullTimestampInteraction));
        when(observationRepository.findByRecordedAtBetween(start, end))
                .thenReturn(List.of(macawObservation, dolphinObservation, nullSpeciesObservation));

        byte[] csvBytes = exportService.exportCsvReport(1, start, end);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\n");

        assertEquals(3, lines.length);
        assertEquals(ExportService.CSV_HEADER, lines[0]);

        // Row 1 (09:15): Macaw observation (chronologically first) with RFC 4180 escaping
        assertEquals(
                "OBSERVATION_MEASUREMENT,201,2026-11-25T09:15:00,1,Macaws,MANUAL,BEHAVIOR_NOTE,,,,"
                        + "4.0,\"Pulled rope, said \"\"hello\"\"\",\"{\"\"intensity\"\": 3, \"\"zone\"\": \"\"north\"\"}\"",
                lines[1]);

        // Row 2 (10:30): Macaw interaction
        assertEquals(
                "INTERACTION,101,2026-11-25T10:30:00,1,Macaws,IOT_MODULE,ROPE_PULL,10,ESP32-Ara-01,Macaw Call,,,",
                lines[2]);

        verify(interactionRepository, never()).findAll();
    }

    @Test
    @DisplayName("exportCsvReport() with null speciesId should export all species and handle nullable relations")
    void testExportCsvReportAllSpeciesAndNullableFields() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 18, 0);

        Interaction boundaryStartInteraction = new Interaction("CONTACT", araModule, null);
        boundaryStartInteraction.setInteractionId(301);
        boundaryStartInteraction.setTriggeredAt(start);

        Interaction transientModuleInteraction = new Interaction("PING", null, null);
        transientModuleInteraction.setInteractionId(null);
        transientModuleInteraction.setTriggeredAt(end);

        ObservationMeasurement telemetryWithModule = new ObservationMeasurement(
                "IOT_MODULE",
                "ACOUSTIC_DB",
                62.4,
                null,
                null,
                dolphins);
        telemetryWithModule.setObservationId(401);
        telemetryWithModule.setModule(araModule);
        telemetryWithModule.setRecordedAt(LocalDateTime.of(2026, 11, 25, 12, 0));

        ObservationMeasurement beforeStartObservation = new ObservationMeasurement(
                "MANUAL",
                "NOTE",
                null,
                "Too early",
                null,
                macaws);
        beforeStartObservation.setObservationId(402);
        beforeStartObservation.setRecordedAt(start.minusMinutes(1));

        when(interactionRepository.findAll())
                .thenReturn(List.of(boundaryStartInteraction, transientModuleInteraction));
        when(observationRepository.findByRecordedAtBetween(start, end))
                .thenReturn(List.of(telemetryWithModule, beforeStartObservation));

        byte[] csvBytes = exportService.exportCsvReport(null, start, end);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\n");

        assertEquals(4, lines.length);
        assertEquals(ExportService.CSV_HEADER, lines[0]);
        assertTrue(lines[1].startsWith("INTERACTION,301,2026-11-25T08:00:00,1,Macaws,IOT_MODULE,CONTACT,10,ESP32-Ara-01,,,,"));
        assertTrue(lines[2].startsWith("OBSERVATION_MEASUREMENT,401,2026-11-25T12:00:00,3,Dolphins,IOT_MODULE,ACOUSTIC_DB,10,ESP32-Ara-01,,62.4,,"));
        assertTrue(lines[3].startsWith("INTERACTION,,2026-11-25T18:00:00,,,IOT_MODULE,PING,,,,,,"));

        verify(interactionRepository).findAll();
        verify(interactionRepository, never()).findByModule_Species_SpeciesId(any());
    }

    @Test
    @DisplayName("exportCsvReport() should return only the CSV header when no records match")
    void testExportCsvReportEmptyResults() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 9, 0);

        when(interactionRepository.findByModule_Species_SpeciesId(2)).thenReturn(Collections.emptyList());
        when(observationRepository.findByRecordedAtBetween(start, end)).thenReturn(Collections.emptyList());

        byte[] csvBytes = exportService.exportCsvReport(2, start, end);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertEquals(ExportService.CSV_HEADER + "\n", csvContent);
    }

    @Test
    @DisplayName("escapeCsv() should properly quote fields containing newlines or carriage returns")
    void testEscapeCsvLineBreaksAndNulls() {
        assertEquals("", ExportService.escapeCsv(null));
        assertEquals("", ExportService.escapeCsv(""));
        assertEquals("SimpleText", ExportService.escapeCsv("SimpleText"));
        assertEquals("\"Line1\nLine2\"", ExportService.escapeCsv("Line1\nLine2"));
        assertEquals("\"Line1\rLine2\"", ExportService.escapeCsv("Line1\rLine2"));
        assertFalse(ExportService.escapeCsv("Plain").startsWith("\""));
    }

    @Test
    @DisplayName("exportCsvReport() should reject non-positive speciesId or null/inverted timestamps")
    void testExportCsvReportValidationFailures() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 14, 0);

        assertThrows(IllegalArgumentException.class, () -> exportService.exportCsvReport(0, start, end));
        assertThrows(IllegalArgumentException.class, () -> exportService.exportCsvReport(-2, start, end));
        assertThrows(IllegalArgumentException.class, () -> exportService.exportCsvReport(1, null, end));
        assertThrows(IllegalArgumentException.class, () -> exportService.exportCsvReport(1, start, null));
        assertThrows(IllegalArgumentException.class, () -> exportService.exportCsvReport(1, end, start));

        verify(interactionRepository, never()).findAll();
        verify(interactionRepository, never()).findByModule_Species_SpeciesId(any());
        verify(observationRepository, never()).findByRecordedAtBetween(any(), any());
    }
}