package peps.peps_back.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * Represents an audio file stored in MinIO object storage and used for animal
 * acoustic enrichment, mapping to the {@code sound} table.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
@Entity
@Table(name = "sound")
public class Sound implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the sound.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sound")
    private Integer soundId;

    /**
     * Display name of the sound (maximum 255 characters).
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Category or functional classification of the sound (maximum 50 characters).
     */
    @Column(name = "sound_type", nullable = false, length = 50)
    private String soundType;

    /**
     * Audio file format extension (e.g., mp3, wav, ogg, m4a).
     */
    @Column(name = "extension", nullable = false, length = 10)
    private String extension;

    /**
     * Unique object key referencing the audio file in the MinIO S3 bucket.
     */
    @Column(name = "minio_key", nullable = false, length = 255)
    private String minioKey;

    /**
     * Total playback duration of the audio file in seconds.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Timestamp indicating when the sound record was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public Sound() {
    }

    /**
     * Constructs a new {@code Sound} instance with its core metadata and MinIO reference.
     *
     * @param name            the display name of the sound; must not be {@code null}
     * @param soundType       the functional category of the sound
     * @param extension       the audio file extension (e.g., {@code "mp3"}, {@code "wav"})
     * @param minioKey        the object key in MinIO S3 storage
     * @param durationSeconds the duration of the audio in seconds, or {@code null} if unknown
     */
    public Sound(String name, String soundType, String extension, String minioKey, Integer durationSeconds) {
        this.name = name;
        this.soundType = soundType;
        this.extension = extension;
        this.minioKey = minioKey;
        this.durationSeconds = durationSeconds;
    }

    /**
     * Automatically populates {@code createdAt} with the current timestamp before persisting
     * if it has not been explicitly set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Returns the unique identifier of this sound.
     *
     * @return the primary key {@code soundId}, or {@code null} if not yet persisted
     */
    public Integer getSoundId() {
        return soundId;
    }

    /**
     * Sets the unique identifier of this sound.
     *
     * @param soundId the primary key to assign
     */
    public void setSoundId(Integer soundId) {
        this.soundId = soundId;
    }

    /**
     * Returns the display name of this sound.
     *
     * @return the sound name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of this sound.
     *
     * @param name the sound name to set (maximum 255 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the category or type of this sound.
     *
     * @return the sound type
     */
    public String getSoundType() {
        return soundType;
    }

    /**
     * Sets the category or type of this sound.
     *
     * @param soundType the sound type to set (maximum 50 characters)
     */
    public void setSoundType(String soundType) {
        this.soundType = soundType;
    }

    /**
     * Returns the audio file extension.
     *
     * @return the file extension (e.g., {@code "mp3"}, {@code "wav"})
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Sets the audio file extension.
     *
     * @param extension the file extension to set (maximum 10 characters)
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the MinIO object key used to retrieve the binary audio file.
     *
     * @return the MinIO S3 object key
     */
    public String getMinioKey() {
        return minioKey;
    }

    /**
     * Sets the MinIO object key for this audio file.
     *
     * @param minioKey the MinIO S3 object key to assign (maximum 255 characters)
     */
    public void setMinioKey(String minioKey) {
        this.minioKey = minioKey;
    }

    /**
     * Returns the duration of the audio file in seconds.
     *
     * @return the duration in seconds, or {@code null} if not available
     */
    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Sets the duration of the audio file in seconds.
     *
     * @param durationSeconds the duration in seconds to assign
     */
    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /**
     * Returns the creation timestamp of this sound record.
     *
     * @return the creation {@code LocalDateTime}
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of this sound record.
     *
     * @param createdAt the creation {@code LocalDateTime} to assign
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Compares this sound to another object for equality based on {@code soundId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted sound; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sound sound = (Sound) o;
        return Objects.equals(soundId, sound.soundId);
    }

    /**
     * Returns a hash code value for this sound based on {@code soundId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(soundId);
    }

    /**
     * Returns a string representation of this {@code Sound} entity.
     *
     * @return a formatted string containing the sound ID, name, type, extension, and MinIO key
     */
    @Override
    public String toString() {
        return "Sound{" +
                "soundId=" + soundId +
                ", name='" + name + '\'' +
                ", soundType='" + soundType + '\'' +
                ", extension='" + extension + '\'' +
                ", minioKey='" + minioKey + '\'' +
                '}';
    }
}