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
 * Unit tests for the {@link IoTModule} domain entity.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
class IoTModuleTest {

    private Species macaws;
    private Playlist morningPlaylist;
    private IoTModule module;

    @BeforeEach
    void setUp() {
        macaws = new Species("Macaws", "Interactive audio enrichment enclosure");
        macaws.setSpeciesId(1);

        morningPlaylist = new Playlist("Morning Forest", "RANDOM");
        morningPlaylist.setPlaylistId(2);

        module = new IoTModule("ESP32-Ara-01", "192.168.1.50", "ONLINE", 75, "RANDOM", macaws);
        module.setModuleId(1);
    }

    @Test
    @DisplayName("Default constructor should initialize operational defaults and null associations")
    void testDefaultConstructor() {
        IoTModule emptyModule = new IoTModule();

        assertNull(emptyModule.getModuleId());
        assertNull(emptyModule.getName());
        assertNull(emptyModule.getIpAddress());
        assertEquals("OFFLINE", emptyModule.getStatus());
        assertNull(emptyModule.getBatteryLevel());
        assertEquals(50, emptyModule.getVolume());
        assertEquals("MANUAL", emptyModule.getCurrentMode());
        assertTrue(emptyModule.getIsActive());
        assertNull(emptyModule.getLastSeenAt());
        assertNull(emptyModule.getSpecies());
        assertNull(emptyModule.getPlaylist());
    }

    @Test
    @DisplayName("Parameterized constructor should assign configuration fields and species")
    void testParameterizedConstructor() {
        IoTModule createdModule = new IoTModule(
                "ESP32-Ara-02", "192.168.1.51", "OFFLINE", 60, "PROGRAMMED", macaws
        );

        assertNull(createdModule.getModuleId());
        assertEquals("ESP32-Ara-02", createdModule.getName());
        assertEquals("192.168.1.51", createdModule.getIpAddress());
        assertEquals("OFFLINE", createdModule.getStatus());
        assertNull(createdModule.getBatteryLevel());
        assertEquals(60, createdModule.getVolume());
        assertEquals("PROGRAMMED", createdModule.getCurrentMode());
        assertTrue(createdModule.getIsActive());
        assertNull(createdModule.getLastSeenAt());
        assertSame(macaws, createdModule.getSpecies());
        assertNull(createdModule.getPlaylist());
    }

    @Test
    @DisplayName("onCreate() should populate null defaults and lastSeenAt without overwriting explicit values")
    void testOnCreateLifecycleCallback() {
        // Branch 1: all defaultable fields are null -> should be initialized automatically
        IoTModule uninitializedModule = new IoTModule();
        uninitializedModule.setStatus(null);
        uninitializedModule.setVolume(null);
        uninitializedModule.setCurrentMode(null);
        uninitializedModule.setIsActive(null);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        uninitializedModule.onCreate();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(uninitializedModule.getLastSeenAt());
        assertTrue(uninitializedModule.getLastSeenAt().isAfter(before));
        assertTrue(uninitializedModule.getLastSeenAt().isBefore(after));
        assertEquals("OFFLINE", uninitializedModule.getStatus());
        assertEquals(50, uninitializedModule.getVolume());
        assertEquals("MANUAL", uninitializedModule.getCurrentMode());
        assertTrue(uninitializedModule.getIsActive());

        // Branch 2: all fields already have explicit values -> should preserve them
        LocalDateTime explicitLastSeen = LocalDateTime.of(2026, 11, 10, 16, 45, 0);
        uninitializedModule.setLastSeenAt(explicitLastSeen);
        uninitializedModule.setStatus("ERROR");
        uninitializedModule.setVolume(90);
        uninitializedModule.setCurrentMode("SEQUENTIAL");
        uninitializedModule.setIsActive(false);

        uninitializedModule.onCreate();

        assertEquals(explicitLastSeen, uninitializedModule.getLastSeenAt());
        assertEquals("ERROR", uninitializedModule.getStatus());
        assertEquals(90, uninitializedModule.getVolume());
        assertEquals("SEQUENTIAL", uninitializedModule.getCurrentMode());
        assertFalse(uninitializedModule.getIsActive());
    }

    @Test
    @DisplayName("Getters and setters should read and update all attributes and relationships")
    void testGettersAndSetters() {
        LocalDateTime seenTime = LocalDateTime.of(2026, 11, 12, 18, 0, 0);
        module.setBatteryLevel(88);
        module.setLastSeenAt(seenTime);
        module.setPlaylist(morningPlaylist);

        assertEquals(1, module.getModuleId());
        assertEquals("ESP32-Ara-01", module.getName());
        assertEquals("192.168.1.50", module.getIpAddress());
        assertEquals("ONLINE", module.getStatus());
        assertEquals(88, module.getBatteryLevel());
        assertEquals(75, module.getVolume());
        assertEquals("RANDOM", module.getCurrentMode());
        assertTrue(module.getIsActive());
        assertEquals(seenTime, module.getLastSeenAt());
        assertSame(macaws, module.getSpecies());
        assertSame(morningPlaylist, module.getPlaylist());

        Species dolphins = new Species("Dolphins", "Pool telemetry");
        dolphins.setSpeciesId(3);
        LocalDateTime updatedSeenTime = LocalDateTime.of(2026, 11, 12, 19, 30, 0);

        module.setModuleId(4);
        module.setName("Hydro-Sensor-01");
        module.setIpAddress("10.0.0.12");
        module.setStatus("OFFLINE");
        module.setBatteryLevel(42);
        module.setVolume(0);
        module.setCurrentMode("MANUAL");
        module.setIsActive(false);
        module.setLastSeenAt(updatedSeenTime);
        module.setSpecies(dolphins);
        module.setPlaylist(null);

        assertEquals(4, module.getModuleId());
        assertEquals("Hydro-Sensor-01", module.getName());
        assertEquals("10.0.0.12", module.getIpAddress());
        assertEquals("OFFLINE", module.getStatus());
        assertEquals(42, module.getBatteryLevel());
        assertEquals(0, module.getVolume());
        assertEquals("MANUAL", module.getCurrentMode());
        assertFalse(module.getIsActive());
        assertEquals(updatedSeenTime, module.getLastSeenAt());
        assertSame(dolphins, module.getSpecies());
        assertNull(module.getPlaylist());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(module.equals(module));

        // Null and different class comparisons
        assertFalse(module.equals(null));
        assertFalse(module.equals("ESP32-Ara-01"));

        // Different instance with the same ID
        IoTModule sameIdModule = new IoTModule();
        sameIdModule.setModuleId(1);
        assertTrue(module.equals(sameIdModule));

        // Different instance with a different ID
        IoTModule differentIdModule = new IoTModule();
        differentIdModule.setModuleId(99);
        assertFalse(module.equals(differentIdModule));

        // Both instances transient (null IDs)
        IoTModule transientOne = new IoTModule();
        IoTModule transientTwo = new IoTModule();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with moduleId")
    void testHashCode() {
        assertEquals(Objects.hash(1), module.hashCode());

        IoTModule sameIdModule = new IoTModule();
        sameIdModule.setModuleId(1);
        assertEquals(module.hashCode(), sameIdModule.hashCode());

        IoTModule differentIdModule = new IoTModule();
        differentIdModule.setModuleId(77);
        assertNotEquals(module.hashCode(), differentIdModule.hashCode());
    }

    @Test
    @DisplayName("toString() should format core module attributes without triggering lazy associations")
    void testToString() {
        String expected = "IoTModule{moduleId=1, name='ESP32-Ara-01', ipAddress='192.168.1.50', " +
                "status='ONLINE', isActive=true}";
        assertEquals(expected, module.toString());

        IoTModule transientModule = new IoTModule();
        assertEquals(
                "IoTModule{moduleId=null, name='null', ipAddress='null', status='OFFLINE', isActive=true}",
                transientModule.toString()
        );
    }
}