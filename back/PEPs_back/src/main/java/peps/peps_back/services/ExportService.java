package peps.peps_back.services;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Interaction;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.ObservationMeasurement;
import peps.peps_back.domain.Sound;
import peps.peps_back.domain.Species;
import peps.peps_back.repositories.InteractionRepository;
import peps.peps_back.repositories.ObservationMeasurementRepository;

/**
 * Service layer responsible for exporting filtered animal-device interaction events
 * and behavioral/environmental observation measurements into standard UTF-8 comma-separated
 * values (CSV) reports for scientific and statistical analysis.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Service
public class ExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

    public static final String CSV_HEADER =
            "record_category,record_id,timestamp,species_id,species_name,data_source,"
                    + "event_or_metric_type,module_id,module_name,sound_name,"
                    + "numeric_value,text_value,json_data";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final InteractionRepository interactionRepository;
    private final ObservationMeasurementRepository observationRepository;

    @Autowired
    public ExportService(
            InteractionRepository interactionRepository,
            ObservationMeasurementRepository observationRepository) {
        this.interactionRepository = interactionRepository;
        this.observationRepository = observationRepository;
    }

    /**
     * Generates a chronological UTF-8 CSV report combining {@link Interaction} events and
     * {@link ObservationMeasurement} records within the specified inclusive time window.
     *
     * @param speciesId optional primary key of the target {@link Species}, or {@code null} for all species
     * @param start     inclusive start timestamp of the export window
     * @param end       inclusive end timestamp of the export window
     * @return UTF-8 encoded CSV content as a byte array
     * @throws IllegalArgumentException if {@code speciesId} is non-positive or timestamps are null/inverted
     */
    @Transactional(readOnly = true)
    public byte[] exportCsvReport(Integer speciesId, LocalDateTime start, LocalDateTime end) {
        if (speciesId != null && speciesId <= 0) {
            throw new IllegalArgumentException("Species ID must be a positive integer");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end timestamps must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start timestamp must not be after end timestamp");
        }

        List<Interaction> interactions = fetchFilteredInteractions(speciesId, start, end);
        List<ObservationMeasurement> observations = fetchFilteredObservations(speciesId, start, end);

        List<ExportRow> rows = new ArrayList<>(interactions.size() + observations.size());
        for (Interaction interaction : interactions) {
            rows.add(mapInteractionToRow(interaction));
        }
        for (ObservationMeasurement observation : observations) {
            rows.add(mapObservationToRow(observation));
        }

        rows.sort(Comparator.comparing(
                ExportRow::timestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(CSV_HEADER).append('\n');
        for (ExportRow row : rows) {
            csvBuilder.append(row.toCsvLine()).append('\n');
        }

        LOGGER.info("Exported CSV report (speciesId={}, start={}, end={}, interactions={}, observations={})",
                speciesId, start, end, interactions.size(), observations.size());

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<Interaction> fetchFilteredInteractions(
            Integer speciesId,
            LocalDateTime start,
            LocalDateTime end) {
        List<Interaction> candidates = (speciesId != null)
                ? interactionRepository.findByModule_Species_SpeciesId(speciesId)
                : interactionRepository.findAll();

        return candidates.stream()
                .filter(interaction -> isWithinRange(interaction.getTriggeredAt(), start, end))
                .collect(Collectors.toList());
    }

    private List<ObservationMeasurement> fetchFilteredObservations(
            Integer speciesId,
            LocalDateTime start,
            LocalDateTime end) {
        return observationRepository.findByRecordedAtBetween(start, end).stream()
                .filter(obs -> isWithinRange(obs.getRecordedAt(), start, end))
                .filter(obs -> speciesId == null
                        || (obs.getSpecies() != null
                        && Objects.equals(speciesId, obs.getSpecies().getSpeciesId())))
                .collect(Collectors.toList());
    }

    private ExportRow mapInteractionToRow(Interaction interaction) {
        IoTModule module = interaction.getModule();
        Species species = (module != null) ? module.getSpecies() : null;
        Sound sound = interaction.getSound();

        return new ExportRow(
                "INTERACTION",
                formatInteger(interaction.getInteractionId()),
                interaction.getTriggeredAt(),
                species != null ? formatInteger(species.getSpeciesId()) : "",
                species != null ? safeString(species.getName()) : "",
                "IOT_MODULE",
                safeString(interaction.getInteractionType()),
                module != null ? formatInteger(module.getModuleId()) : "",
                module != null ? safeString(module.getName()) : "",
                sound != null ? safeString(sound.getName()) : "",
                "",
                "",
                ""
        );
    }

    private ExportRow mapObservationToRow(ObservationMeasurement observation) {
        Species species = observation.getSpecies();
        IoTModule module = observation.getModule();

        return new ExportRow(
                "OBSERVATION_MEASUREMENT",
                formatInteger(observation.getObservationId()),
                observation.getRecordedAt(),
                species != null ? formatInteger(species.getSpeciesId()) : "",
                species != null ? safeString(species.getName()) : "",
                safeString(observation.getDataSource()),
                safeString(observation.getMeasurementType()),
                module != null ? formatInteger(module.getModuleId()) : "",
                module != null ? safeString(module.getName()) : "",
                "",
                observation.getNumericValue() != null ? observation.getNumericValue().toString() : "",
                safeString(observation.getTextValue()),
                safeString(observation.getJsonData())
        );
    }

    private boolean isWithinRange(LocalDateTime candidate, LocalDateTime start, LocalDateTime end) {
        return candidate != null && !candidate.isBefore(start) && !candidate.isAfter(end);
    }

    private String formatInteger(Integer value) {
        return value != null ? value.toString() : "";
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    /**
     * Escapes a single CSV field according to RFC 4180 rules.
     */
    static String escapeCsv(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return "";
        }
        boolean requiresQuoting = rawValue.indexOf(',') >= 0
                || rawValue.indexOf('"') >= 0
                || rawValue.indexOf('\n') >= 0
                || rawValue.indexOf('\r') >= 0;

        if (!requiresQuoting) {
            return rawValue;
        }
        return "\"" + rawValue.replace("\"", "\"\"") + "\"";
    }

    /**
     * Immutable internal representation of a normalized CSV output record.
     */
    private record ExportRow(
            String recordCategory,
            String recordId,
            LocalDateTime timestamp,
            String speciesId,
            String speciesName,
            String dataSource,
            String eventOrMetricType,
            String moduleId,
            String moduleName,
            String soundName,
            String numericValue,
            String textValue,
            String jsonData) {

        private String toCsvLine() {
            String formattedTimestamp = (timestamp != null) ? timestamp.format(TIMESTAMP_FORMATTER) : "";
            return String.join(",",
                    escapeCsv(recordCategory),
                    escapeCsv(recordId),
                    escapeCsv(formattedTimestamp),
                    escapeCsv(speciesId),
                    escapeCsv(speciesName),
                    escapeCsv(dataSource),
                    escapeCsv(eventOrMetricType),
                    escapeCsv(moduleId),
                    escapeCsv(moduleName),
                    escapeCsv(soundName),
                    escapeCsv(numericValue),
                    escapeCsv(textValue),
                    escapeCsv(jsonData)
            );
        }
    }
}