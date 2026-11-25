package peps.peps_back.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.ObservationMeasurement;
import peps.peps_back.domain.Species;
import peps.peps_back.repositories.ObservationMeasurementRepository;
import peps.peps_back.repositories.SpeciesRepository;

/**
 * Service layer responsible for validating, ingesting, and querying behavioral
 * observations and environmental/telemetry measurements associated with animal
 * {@link Species} within the PEP'S platform.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Service
public class ObservationMeasurementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationMeasurementService.class);

    public static final int MAX_DATA_SOURCE_LENGTH = 50;
    public static final int MAX_MEASUREMENT_TYPE_LENGTH = 50;

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_EXTERNAL_API = "EXTERNAL_API";
    public static final String SOURCE_IOT_MODULE = "IOT_MODULE";

    public static final Set<String> ALLOWED_DATA_SOURCES = Set.of(
            SOURCE_MANUAL,
            SOURCE_EXTERNAL_API,
            SOURCE_IOT_MODULE
    );

    private final ObservationMeasurementRepository observationRepository;
    private final SpeciesRepository speciesRepository;

    /**
     * Constructs an {@code ObservationMeasurementService} with its required persistence repositories.
     *
     * @param observationRepository repository managing {@link ObservationMeasurement} entities
     * @param speciesRepository     repository managing {@link Species} entities
     */
    @Autowired
    public ObservationMeasurementService(
            ObservationMeasurementRepository observationRepository,
            SpeciesRepository speciesRepository) {
        this.observationRepository = observationRepository;
        this.speciesRepository = speciesRepository;
    }

    /**
     * Validates, resolves the target {@link Species}, and persists a new
     * {@link ObservationMeasurement} record from either manual caretaker input
     * or automated external/IoT telemetry ingestion.
     *
     * If {@code recordedAt} is not explicitly set, the current timestamp is assigned.
     *
     * @param data the {@link ObservationMeasurement} payload to validate and persist
     * @return the newly persisted {@link ObservationMeasurement} entity
     * @throws IllegalArgumentException if the payload, classification metadata, value fields,
     *                                  or referenced species are invalid or not found
     */
    @Transactional
    public ObservationMeasurement ingestMeasurement(ObservationMeasurement data) {
        if (data == null) {
            throw new IllegalArgumentException("Observation measurement must not be null");
        }

        String normalizedSource = validateAndNormalizeDataSource(data.getDataSource());
        data.setDataSource(normalizedSource);

        String normalizedType = validateAndNormalizeMeasurementType(data.getMeasurementType());
        data.setMeasurementType(normalizedType);

        validateAndNormalizePayloadValues(data);
        validateOptionalAssociations(data);

        Species managedSpecies = resolveSpecies(data.getSpecies());
        data.setSpecies(managedSpecies);

        if (data.getRecordedAt() == null) {
            data.setRecordedAt(LocalDateTime.now());
        }

        ObservationMeasurement saved = observationRepository.save(data);
        LOGGER.info("Ingested measurement '{}' from '{}' (id={}, species='{}')",
                saved.getMeasurementType(),
                saved.getDataSource(),
                saved.getObservationId(),
                managedSpecies.getName());
        return saved;
    }

    /**
     * Retrieves all observation and measurement records classified under a given species.
     *
     * @param speciesId the primary key of the target {@link Species}
     * @return a list of matching {@link ObservationMeasurement} records
     * @throws IllegalArgumentException if {@code speciesId} is null or non-positive
     */
    @Transactional(readOnly = true)
    public List<ObservationMeasurement> getMeasurementsBySpecies(Integer speciesId) {
        validatePositiveId(speciesId, "Species ID");

        return observationRepository.findAll().stream()
                .filter(measurement -> measurement.getSpecies() != null
                        && speciesId.equals(measurement.getSpecies().getSpeciesId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all observation and measurement records for a given species filtered by
     * measurement category.
     *
     * @param speciesId       the primary key of the target {@link Species}
     * @param measurementType the category of measurement to filter by
     * @return a list of matching {@link ObservationMeasurement} records
     * @throws IllegalArgumentException if {@code speciesId} or {@code measurementType} is invalid
     */
    @Transactional(readOnly = true)
    public List<ObservationMeasurement> getMeasurementsBySpeciesAndType(
            Integer speciesId,
            String measurementType) {
        validatePositiveId(speciesId, "Species ID");
        String normalizedType = validateAndNormalizeMeasurementType(measurementType);

        return observationRepository.findBySpecies_SpeciesIdAndMeasurementType(speciesId, normalizedType);
    }

    /**
     * Retrieves all observation and measurement records logged within an inclusive time window.
     *
     * @param start inclusive start timestamp of the query window
     * @param end   inclusive end timestamp of the query window
     * @return a list of matching {@link ObservationMeasurement} records
     * @throws IllegalArgumentException if timestamps are null or {@code start} is after {@code end}
     */
    @Transactional(readOnly = true)
    public List<ObservationMeasurement> getMeasurementsByPeriod(
            LocalDateTime start,
            LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end timestamps must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start timestamp must not be after end timestamp");
        }

        return observationRepository.findByRecordedAtBetween(start, end);
    }

    /**
     * Validates and normalizes the data origin channel against allowed values and column bounds.
     */
    private String validateAndNormalizeDataSource(String dataSource) {
        if (dataSource == null || dataSource.isBlank()) {
            throw new IllegalArgumentException("Data source must not be null or blank");
        }
        String normalized = dataSource.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_DATA_SOURCE_LENGTH) {
            throw new IllegalArgumentException(
                    "Data source must not exceed " + MAX_DATA_SOURCE_LENGTH + " characters");
        }
        if (!ALLOWED_DATA_SOURCES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported data source: " + dataSource);
        }
        return normalized;
    }

    /**
     * Validates and trims the measurement type label against null/blank and database length bounds.
     */
    private String validateAndNormalizeMeasurementType(String measurementType) {
        if (measurementType == null || measurementType.isBlank()) {
            throw new IllegalArgumentException("Measurement type must not be null or blank");
        }
        String trimmed = measurementType.trim();
        if (trimmed.length() > MAX_MEASUREMENT_TYPE_LENGTH) {
            throw new IllegalArgumentException(
                    "Measurement type must not exceed " + MAX_MEASUREMENT_TYPE_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * Ensures at least one value representation (numeric, text, or JSON) is present and well-formed.
     */
    private void validateAndNormalizePayloadValues(ObservationMeasurement data) {
        Double numericValue = data.getNumericValue();
        if (numericValue != null && (numericValue.isNaN() || numericValue.isInfinite())) {
            throw new IllegalArgumentException("Numeric value must be a finite number");
        }

        String textValue = data.getTextValue();
        if (textValue != null) {
            String trimmedText = textValue.trim();
            data.setTextValue(trimmedText.isEmpty() ? null : trimmedText);
        }

        String jsonData = data.getJsonData();
        if (jsonData != null) {
            String trimmedJson = jsonData.trim();
            if (trimmedJson.isEmpty()) {
                data.setJsonData(null);
            } else {
                boolean isObject = trimmedJson.startsWith("{") && trimmedJson.endsWith("}");
                boolean isArray = trimmedJson.startsWith("[") && trimmedJson.endsWith("]");
                if (!isObject && !isArray) {
                    throw new IllegalArgumentException("JSON data must be a valid JSON object or array");
                }
                data.setJsonData(trimmedJson);
            }
        }

        boolean hasNumeric = data.getNumericValue() != null;
        boolean hasText = data.getTextValue() != null;
        boolean hasJson = data.getJsonData() != null;

        if (!hasNumeric && !hasText && !hasJson) {
            throw new IllegalArgumentException(
                    "Observation measurement must contain at least one value (numeric, text, or JSON)");
        }
    }

    /**
     * Validates that optional {@code User} and {@code IoTModule} references, if provided,
     * carry valid positive primary keys rather than transient null IDs.
     */
    private void validateOptionalAssociations(ObservationMeasurement data) {
        if (data.getUser() != null) {
            validatePositiveId(data.getUser().getUserId(), "User ID");
        }
        if (data.getModule() != null) {
            validatePositiveId(data.getModule().getModuleId(), "Module ID");
        }
    }

    /**
     * Resolves a managed {@link Species} entity by primary key or unique species name.
     */
    private Species resolveSpecies(Species speciesRef) {
        if (speciesRef == null) {
            throw new IllegalArgumentException("Observation measurement must reference a target species");
        }

        Integer speciesId = speciesRef.getSpeciesId();
        if (speciesId != null) {
            validatePositiveId(speciesId, "Species ID");
            return speciesRepository.findById(speciesId)
                    .orElseThrow(() -> new IllegalArgumentException("Species not found with ID: " + speciesId));
        }

        String speciesName = speciesRef.getName();
        if (speciesName != null && !speciesName.isBlank()) {
            String trimmedName = speciesName.trim();
            return speciesRepository.findByName(trimmedName)
                    .orElseThrow(() -> new IllegalArgumentException("Species not found with name: " + trimmedName));
        }

        throw new IllegalArgumentException("Species reference must provide a valid species ID or name");
    }

    /**
     * Validates that an entity identifier is non-null and strictly positive.
     */
    private void validatePositiveId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer");
        }
    }
}