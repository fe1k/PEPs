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
 * Represents a physical interaction event triggered by an animal on an
 * {@link IoTModule} (e.g., rope pull, spring trigger, contact sensor) and
 * optionally linked to a played {@link Sound}, mapping to the {@code interaction} table.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
@Entity
@Table(name = "interaction")
public class Interaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the interaction event.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_interaction")
    private Integer interactionId;

    /**
     * Category or mechanical type of the physical interaction (maximum 50 characters).
     */
    @Column(name = "interaction_type", nullable = false, length = 50)
    private String interactionType;

    /**
     * Timestamp indicating when the interaction occurred on the IoT module.
     */
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    /**
     * IoT module that detected and recorded this interaction event.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_module", nullable = false)
    private IoTModule module;

    /**
     * Optional audio file triggered in response to this interaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sound")
    private Sound sound;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public Interaction() {
    }

    /**
     * Constructs a new {@code Interaction} instance with the specified type, source module,
     * and optional triggered sound.
     *
     * @param interactionType the physical interaction type; must not be {@code null}
     * @param module          the {@link IoTModule} that recorded the event; must not be {@code null}
     * @param sound           the {@link Sound} triggered by the event, or {@code null} if silent
     */
    public Interaction(String interactionType, IoTModule module, Sound sound) {
        this.interactionType = interactionType;
        this.module = module;
        this.sound = sound;
    }

    /**
     * Automatically sets {@code triggeredAt} to the current timestamp before persisting
     * if no explicit timestamp was provided by the IoT device.
     */
    @PrePersist
    protected void onCreate() {
        if (this.triggeredAt == null) {
            this.triggeredAt = LocalDateTime.now();
        }
    }

    /**
     * Returns the unique identifier of this interaction.
     *
     * @return the primary key {@code interactionId}, or {@code null} if not yet persisted
     */
    public Integer getInteractionId() {
        return interactionId;
    }

    /**
     * Sets the unique identifier of this interaction.
     *
     * @param interactionId the primary key to assign
     */
    public void setInteractionId(Integer interactionId) {
        this.interactionId = interactionId;
    }

    /**
     * Returns the type of physical interaction recorded.
     *
     * @return the interaction type string
     */
    public String getInteractionType() {
        return interactionType;
    }

    /**
     * Sets the type of physical interaction recorded.
     *
     * @param interactionType the interaction type to assign (maximum 50 characters)
     */
    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    /**
     * Returns the timestamp when the interaction was triggered.
     *
     * @return the event {@code LocalDateTime}
     */
    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    /**
     * Sets the timestamp when the interaction was triggered.
     *
     * @param triggeredAt the event {@code LocalDateTime} to assign
     */
    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    /**
     * Returns the IoT module that generated this interaction.
     *
     * @return the parent {@link IoTModule}
     */
    public IoTModule getModule() {
        return module;
    }

    /**
     * Sets the IoT module that generated this interaction.
     *
     * @param module the parent {@link IoTModule} to assign
     */
    public void setModule(IoTModule module) {
        this.module = module;
    }

    /**
     * Returns the sound triggered by this interaction, if any.
     *
     * @return the triggered {@link Sound}, or {@code null} if no sound was played
     */
    public Sound getSound() {
        return sound;
    }

    /**
     * Sets the sound triggered by this interaction.
     *
     * @param sound the {@link Sound} to assign, or {@code null} if none
     */
    public void setSound(Sound sound) {
        this.sound = sound;
    }

    /**
     * Compares this interaction to another object for equality based on {@code interactionId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted interaction; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Interaction that = (Interaction) o;
        return Objects.equals(interactionId, that.interactionId);
    }

    /**
     * Returns a hash code value for this interaction based on {@code interactionId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(interactionId);
    }

    /**
     * Returns a string representation of this {@code Interaction} entity without
     * triggering lazy-loaded associations.
     *
     * @return a formatted string containing the interaction ID, type, and timestamp
     */
    @Override
    public String toString() {
        return "Interaction{" +
                "interactionId=" + interactionId +
                ", interactionType='" + interactionType + '\'' +
                ", triggeredAt=" + triggeredAt +
                '}';
    }
}