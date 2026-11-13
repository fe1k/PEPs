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
 * Represents a physical IoT enrichment device deployed in an animal enclosure,
 * tracking its network status, battery level, audio playback settings, and assigned
 * species and playlist, mapping to the {@code iot_module} table.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
@Entity
@Table(name = "iot_module")
public class IoTModule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique primary identifier of the IoT module.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_module")
    private Integer moduleId;

    /**
     * Human-readable name identifying the physical device (maximum 255 characters).
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Local network IPv4/IPv6 address assigned to the IoT module (maximum 50 characters).
     */
    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    /**
     * Current operational connectivity state of the device (e.g., ONLINE, OFFLINE, ERROR).
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status = "OFFLINE";

    /**
     * Remaining battery percentage of the device (0 to 100), or {@code null} if mains-powered.
     */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    /**
     * Configured audio output volume level (0 to 100).
     */
    @Column(name = "volume", nullable = false)
    private Integer volume = 50;

    /**
     * Active playback operating mode of the module (e.g., MANUAL, RANDOM, PROGRAMMED).
     */
    @Column(name = "current_mode", nullable = false, length = 50)
    private String currentMode = "MANUAL";

    /**
     * Flag indicating whether the module is administratively enabled in the system.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Timestamp of the most recent heartbeat or network communication received from the device.
     */
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    /**
     * Animal species enclosure to which this IoT module belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_species", nullable = false)
    private Species species;

    /**
     * Optional audio playlist currently assigned to this IoT module.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_playlist")
    private Playlist playlist;

    /**
     * Default no-argument constructor required by the JPA specification.
     */
    public IoTModule() {
    }

    /**
     * Constructs a new {@code IoTModule} instance with its required network, configuration,
     * and species association fields.
     *
     * @param name        the display name of the device; must not be {@code null}
     * @param ipAddress   the network IP address of the device; must not be {@code null}
     * @param status      the initial operational status (e.g., {@code "OFFLINE"})
     * @param volume      the audio volume level (0-100)
     * @param currentMode the playback operating mode (e.g., {@code "MANUAL"})
     * @param species     the target {@link Species} enclosure; must not be {@code null}
     */
    public IoTModule(String name, String ipAddress, String status, Integer volume,
                     String currentMode, Species species) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.status = status;
        this.volume = volume;
        this.currentMode = currentMode;
        this.species = species;
        this.isActive = true;
    }

    /**
     * Automatically initializes default operational values and {@code lastSeenAt}
     * before persisting a new module entity if they have not been set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.lastSeenAt == null) {
            this.lastSeenAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "OFFLINE";
        }
        if (this.volume == null) {
            this.volume = 50;
        }
        if (this.currentMode == null) {
            this.currentMode = "MANUAL";
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    /**
     * Returns the unique identifier of this IoT module.
     *
     * @return the primary key {@code moduleId}, or {@code null} if not yet persisted
     */
    public Integer getModuleId() {
        return moduleId;
    }

    /**
     * Sets the unique identifier of this IoT module.
     *
     * @param moduleId the primary key to assign
     */
    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Returns the display name of this IoT module.
     *
     * @return the device name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of this IoT module.
     *
     * @param name the device name to assign (maximum 255 characters)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the network IP address of this IoT module.
     *
     * @return the IP address string
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the network IP address of this IoT module.
     *
     * @param ipAddress the IP address string to assign (maximum 50 characters)
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the current operational status of this IoT module.
     *
     * @return the status string (e.g., {@code "ONLINE"}, {@code "OFFLINE"}, {@code "ERROR"})
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the operational status of this IoT module.
     *
     * @param status the status string to assign
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the current battery percentage of this IoT module.
     *
     * @return the battery level (0 to 100), or {@code null} if not applicable
     */
    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    /**
     * Sets the battery percentage of this IoT module.
     *
     * @param batteryLevel the battery level (0 to 100) to assign
     */
    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    /**
     * Returns the configured audio volume of this IoT module.
     *
     * @return the volume level
     */
    public Integer getVolume() {
        return volume;
    }

    /**
     * Sets the audio volume of this IoT module.
     *
     * @param volume the volume level to assign
     */
    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    /**
     * Returns the active operating mode of this IoT module.
     *
     * @return the current mode string
     */
    public String getCurrentMode() {
        return currentMode;
    }

    /**
     * Sets the active operating mode of this IoT module.
     *
     * @param currentMode the operating mode string to assign
     */
    public void setCurrentMode(String currentMode) {
        this.currentMode = currentMode;
    }

    /**
     * Returns whether this IoT module is administratively active.
     *
     * @return {@code true} if active; {@code false} if disabled
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets whether this IoT module is administratively active.
     *
     * @param isActive {@code true} to enable the module; {@code false} to disable it
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Returns the timestamp of the last heartbeat received from this module.
     *
     * @return the last seen {@code LocalDateTime}
     */
    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    /**
     * Sets the timestamp of the last heartbeat received from this module.
     *
     * @param lastSeenAt the {@code LocalDateTime} to assign
     */
    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    /**
     * Returns the animal species associated with this IoT module.
     *
     * @return the parent {@link Species} entity
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Sets the animal species associated with this IoT module.
     *
     * @param species the {@link Species} entity to assign
     */
    public void setSpecies(Species species) {
        this.species = species;
    }

    /**
     * Returns the playlist currently assigned to this IoT module.
     *
     * @return the assigned {@link Playlist}, or {@code null} if none is assigned
     */
    public Playlist getPlaylist() {
        return playlist;
    }

    /**
     * Sets the playlist assigned to this IoT module.
     *
     * @param playlist the {@link Playlist} to assign, or {@code null} to unassign
     */
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    /**
     * Compares this IoT module to another object for equality based on {@code moduleId}.
     *
     * @param o the reference object with which to compare
     * @return {@code true} if both objects represent the same persisted module; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IoTModule ioTModule = (IoTModule) o;
        return Objects.equals(moduleId, ioTModule.moduleId);
    }

    /**
     * Returns a hash code value for this IoT module based on {@code moduleId}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(moduleId);
    }

    /**
     * Returns a string representation of this {@code IoTModule} entity without
     * triggering lazy-loaded associations.
     *
     * @return a formatted string containing the module ID, name, IP address, status, and active state
     */
    @Override
    public String toString() {
        return "IoTModule{" +
                "moduleId=" + moduleId +
                ", name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", status='" + status + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}