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
 * Unit tests for the {@link Interaction} domain entity.
 *
 * @author Franco Valdebenito
 * @version 2.0
 */
class InteractionTest {

    private Species macaws;
    private IoTModule module;
    private Sound sound;
    private Interaction interaction;

    @BeforeEach
    void setUp() {
        macaws = new Species("Macaws", "Interactive audio enrichment enclosure");
        macaws.setSpeciesId(1);

        module = new IoTModule("ESP32-Ara-01", "192.168.1.50", "ONLINE", 75, "RANDOM", macaws);
        module.setModuleId(1);

        sound = new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12);
        sound.setSoundId(7);

        interaction = new Interaction("ROPE_PULL", module, sound);
        interaction.setInteractionId(100);
    }

    @Test
    @DisplayName("Default constructor should initialize all fields and relationships to null")
    void testDefaultConstructor() {
        Interaction emptyInteraction = new Interaction();

        assertNull(emptyInteraction.getInteractionId());
        assertNull(emptyInteraction.getInteractionType());
        assertNull(emptyInteraction.getTriggeredAt());
        assertNull(emptyInteraction.getModule());
        assertNull(emptyInteraction.getSound());
    }

    @Test
    @DisplayName("Parameterized constructor should assign interactionType, module, and optional sound")
    void testParameterizedConstructor() {
        Interaction silentInteraction = new Interaction("SPRING_TRIGGER", module, null);

        assertNull(silentInteraction.getInteractionId());
        assertEquals("SPRING_TRIGGER", silentInteraction.getInteractionType());
        assertNull(silentInteraction.getTriggeredAt());
        assertSame(module, silentInteraction.getModule());
        assertNull(silentInteraction.getSound());
    }

    @Test
    @DisplayName("onCreate() should populate triggeredAt only when it is null")
    void testOnCreateLifecycleCallback() {
        // Branch 1: triggeredAt is null -> should be initialized automatically
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        interaction.onCreate();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(interaction.getTriggeredAt());
        assertTrue(interaction.getTriggeredAt().isAfter(before));
        assertTrue(interaction.getTriggeredAt().isBefore(after));

        // Branch 2: triggeredAt is already set -> should preserve the explicit timestamp
        LocalDateTime explicitTimestamp = LocalDateTime.of(2026, 11, 12, 15, 20, 0);
        interaction.setTriggeredAt(explicitTimestamp);
        interaction.onCreate();

        assertEquals(explicitTimestamp, interaction.getTriggeredAt());
    }

    @Test
    @DisplayName("Getters and setters should read and update all attributes and relationships")
    void testGettersAndSetters() {
        LocalDateTime triggered = LocalDateTime.of(2026, 11, 12, 10, 30, 0);
        interaction.setTriggeredAt(triggered);

        assertEquals(100, interaction.getInteractionId());
        assertEquals("ROPE_PULL", interaction.getInteractionType());
        assertEquals(triggered, interaction.getTriggeredAt());
        assertSame(module, interaction.getModule());
        assertSame(sound, interaction.getSound());

        IoTModule secondModule = new IoTModule("ESP32-Ara-02", "192.168.1.51", "ONLINE", 60, "MANUAL", macaws);
        secondModule.setModuleId(2);
        LocalDateTime updatedTime = LocalDateTime.of(2026, 11, 12, 11, 45, 0);

        interaction.setInteractionId(101);
        interaction.setInteractionType("CONTACT_SENSOR");
        interaction.setTriggeredAt(updatedTime);
        interaction.setModule(secondModule);
        interaction.setSound(null);

        assertEquals(101, interaction.getInteractionId());
        assertEquals("CONTACT_SENSOR", interaction.getInteractionType());
        assertEquals(updatedTime, interaction.getTriggeredAt());
        assertSame(secondModule, interaction.getModule());
        assertNull(interaction.getSound());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(interaction.equals(interaction));

        // Null and different class comparisons
        assertFalse(interaction.equals(null));
        assertFalse(interaction.equals("ROPE_PULL"));

        // Different instance with the same ID
        Interaction sameIdInteraction = new Interaction();
        sameIdInteraction.setInteractionId(100);
        assertTrue(interaction.equals(sameIdInteraction));

        // Different instance with a different ID
        Interaction differentIdInteraction = new Interaction("ROPE_PULL", module, sound);
        differentIdInteraction.setInteractionId(200);
        assertFalse(interaction.equals(differentIdInteraction));

        // Both instances transient (null IDs)
        Interaction transientOne = new Interaction();
        Interaction transientTwo = new Interaction();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with interactionId")
    void testHashCode() {
        assertEquals(Objects.hash(100), interaction.hashCode());

        Interaction sameIdInteraction = new Interaction();
        sameIdInteraction.setInteractionId(100);
        assertEquals(interaction.hashCode(), sameIdInteraction.hashCode());

        Interaction differentIdInteraction = new Interaction();
        differentIdInteraction.setInteractionId(500);
        assertNotEquals(interaction.hashCode(), differentIdInteraction.hashCode());
    }

    @Test
    @DisplayName("toString() should format interactionId, interactionType, and triggeredAt safely")
    void testToString() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 11, 12, 9, 15, 0);
        interaction.setTriggeredAt(fixedTime);

        String expected = "Interaction{interactionId=100, interactionType='ROPE_PULL', " +
                "triggeredAt=2026-11-12T09:15}";
        assertEquals(expected, interaction.toString());

        Interaction transientInteraction = new Interaction();
        assertEquals(
                "Interaction{interactionId=null, interactionType='null', triggeredAt=null}",
                transientInteraction.toString()
        );
    }
}