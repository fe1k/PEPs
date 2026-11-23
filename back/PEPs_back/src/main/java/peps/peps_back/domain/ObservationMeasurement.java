package peps.peps_back.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * Represents a behavioral observation or environmental/telemetry measurement
 * recorded either manually by a caretaker or automatically via an external API
 * or IoT module, mapping to the {@code observation_measurement} table.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@Entity
@Table(name = "observation_measurement")
public class ObservationMeasurement implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the observation or measurement record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_observation")
    private Integer observationId;

    /**
     * Timestamp indicating when the observation or measurement was recorded.
     */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /**
     * Origin channel of the data record (e.g., MANUAL, EXTERNAL_API).
     */
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;

    /**
     * Specific metric or observation category (e.g., WATER_TEMP, BEHAVIOR_RATING).
     */
    @Column(name = "measurement_type", nullable = false, length = 50)
    private String measurementType;

    /**
     * Optional quantitative floating-point value for numeric sensor metrics.
     */
    @Column(name = "numeric_value")
    private Double numericValue;

    /**
     * Optional qualitative textual note entered during manual observations.
     */
    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    /**
     * Optional serialized JSON payload for flexible multi-parameter external data.
     */
    @Column(name = "json_data", columnDefinition = "JSONB")
    private String jsonData;

    /**
     * Animal species to which this observation or measurement applies.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_species", nullable = false)
    private Species species;

    /**
     * User who manually submitted the observation, or {@code null} if ingested automatically.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    private User user;

    /**
     * IoT module that transmitted the measurement, or {@code null} for manual observations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_module")
    private IoTModule module;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public ObservationMeasurement() {
    }

    /**
     * Constructs a new {@code ObservationMeasurement} instance with its required classification
     * metadata and optional value representations.
     *
     * @param dataSource      the origin channel (e.g., {@code "MANUAL"} or {@code "EXTERNAL_API"})
     * @param measurementType the category of the measurement; must not be {@code null}
     * @param numericValue    optional numeric value
     * @param textValue       optional textual observation
     * @param jsonData        optional JSON payload string
     * @param species         the target {@link Species}; must not be {@code null}
     */
    public ObservationMeasurement(String dataSource, String measurementType, Double numericValue,
                                  String textValue, String jsonData, Species species) {
        this.dataSource = dataSource;
        this.measurementType = measurementType;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.jsonData = jsonData;
        this.species = species;
    }

    /**
     * Automatically sets {@code recordedAt} to the current timestamp before persisting
     * if no explicit timestamp was provided.
     */
    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }

    /**
     * Returns the unique identifier of this observation measurement.
     *
     * @return the primary key {@code observationId}, or {@code null} if not yet persisted
     */
    public Integer getObservationId() {
        return observationId;
    }

    /**
     * Sets the unique identifier of this observation measurement.
     *
     * @param observationId the primary key to assign
     */
    public void setObservationId(Integer observationId) {
        this.observationId = observationId;
    }

    /**
     * Returns the timestamp when this observation or measurement was recorded.
     *
     * @return the recording {@code LocalDateTime}
     */
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    /**
     * Sets the timestamp when this observation or measurement was recorded.
     *
     * @param recordedAt the recording {@code LocalDateTime} to assign
     */
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    /**
     * Returns the data origin channel (e.g., {@code "MANUAL"}, {@code "EXTERNAL_API"}).
     *
     * @return the data source identifier
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Sets the data origin channel.
     *
     * @param dataSource the data source identifier to assign (maximum 50 characters)
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns the category or type of measurement.
     *
     * @return the measurement type string
     */
    public String getMeasurementType() {
        return measurementType;
    }

    /**
     * Sets the category or type of measurement.
     *
     * @param measurementType the measurement type to assign (maximum 50 characters)
     */
    public void setMeasurementType(String measurementType) {
        this.measurementType = measurementType;
    }

    /**
     * Returns the numeric value of the measurement, if applicable.
     *
     * @return the floating-point numeric value, or {@code null} if not numeric
     */
    public Double getNumericValue() {
        return numericValue;
    }

    /**
     * Sets the numeric value of the measurement.
     *
     * @param numericValue the floating-point value to assign
     */
    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }

    /**
     * Returns the qualitative text note of the observation, if applicable.
     *
     * @return the textual note, or {@code null} if none was recorded
     */
    public String getTextValue() {
        return textValue;
    }

    /**
     * Sets the qualitative text note of the observation.
     *
     * @param textValue the text note to assign
     */
    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    /**
     * Returns the raw JSON payload string for structured external data.
     *
     * @return the JSON string, or {@code null} if not applicable
     */
    public String getJsonData() {
        return jsonData;
    }

    /**
     * Sets the raw JSON payload string.
     *
     * @param jsonData the JSON string to assign
     */
    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    /**
     * Returns the animal species associated with this observation or measurement.
     *
     * @return the parent {@link Species} entity
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Sets the animal species associated with this observation or measurement.
     *
     * @param species the {@link Species} entity to assign
     */
    public void setSpecies(Species species) {
        this.species = species;
    }

    /**
     * Returns the user who manually submitted this observation, if applicable.
     *
     * @return the submitting {@link User}, or {@code null} if ingested via an external system
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who manually submitted this observation.
     *
     * @param user the {@link User} to assign, or {@code null} for automated measurements
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns the IoT module that transmitted this measurement, if applicable.
     *
     * @return the source {@link IoTModule}, or {@code null} for manual observations
     */
    public IoTModule getModule() {
        return module;
    }

    /**
     * Sets the IoT module that transmitted this measurement.
     *
     * @param module the {@link IoTModule} to assign, or {@code null} for manual entries
     */
    public void setModule(IoTModule module) {
        this.module = module;
    }

    /**
     * Compares this observation measurement to another object for equality based on {@code observationId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted record; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObservationMeasurement that = (ObservationMeasurement) o;
        return Objects.equals(observationId, that.observationId);
    }

    /**
     * Returns a hash code value for this observation measurement based on {@code observationId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(observationId);
    }

    /**
     * Returns a string representation of this {@code ObservationMeasurement} entity
     * without triggering lazy-loaded associations or dumping large payloads.
     *
     * @return a formatted string containing the observation ID, data source, type, and timestamp
     */
    @Override
    public String toString() {
        return "ObservationMeasurement{" +
                "observationId=" + observationId +
                ", dataSource='" + dataSource + '\'' +
                ", measurementType='" + measurementType + '\'' +
                ", recordedAt=" + recordedAt +
                '}';
    }
}