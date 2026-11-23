package peps.peps_back.domain;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Species} domain entity.
 *
 * @author Franco Valdebenito
 * @version 1.1
 */
class SpeciesTest {

    private Species species;

    @BeforeEach
    void setUp() {
        species = new Species("Macaws", "Interactive sound modules and playlist management");
        species.setSpeciesId(1);
    }

    @Test
    @DisplayName("Default constructor should initialize all fields to null")
    void testDefaultConstructor() {
        Species emptySpecies = new Species();

        assertNull(emptySpecies.getSpeciesId());
        assertNull(emptySpecies.getName());
        assertNull(emptySpecies.getDescription());
    }

    @Test
    @DisplayName("Parameterized constructor should assign name and description")
    void testParameterizedConstructor() {
        Species saimiris = new Species("Saimiris", "Manual behavioral observation entry forms");

        assertNull(saimiris.getSpeciesId());
        assertEquals("Saimiris", saimiris.getName());
        assertEquals("Manual behavioral observation entry forms", saimiris.getDescription());
    }

    @Test
    @DisplayName("Getters and setters should read and update all entity attributes")
    void testGettersAndSetters() {
        assertEquals(1, species.getSpeciesId());
        assertEquals("Macaws", species.getName());
        assertEquals("Interactive sound modules and playlist management", species.getDescription());

        species.setSpeciesId(3);
        species.setName("Dolphins");
        species.setDescription("Automated external network ingestion and pool metrics");

        assertEquals(3, species.getSpeciesId());
        assertEquals("Dolphins", species.getName());
        assertEquals("Automated external network ingestion and pool metrics", species.getDescription());
    }

    @Test
    @DisplayName("equals() should handle same reference, null, different type, and ID comparisons")
    void testEquals() {
        // Same object reference
        assertTrue(species.equals(species));

        // Null and different class comparisons
        assertFalse(species.equals(null));
        assertFalse(species.equals("Macaws"));

        // Different instance with the same ID
        Species sameIdSpecies = new Species("Different Name", "Different Description");
        sameIdSpecies.setSpeciesId(1);
        assertTrue(species.equals(sameIdSpecies));

        // Different instance with a different ID
        Species differentIdSpecies = new Species("Macaws", "Same Description");
        differentIdSpecies.setSpeciesId(2);
        assertFalse(species.equals(differentIdSpecies));

        // Both instances transient (null IDs)
        Species transientOne = new Species();
        Species transientTwo = new Species();
        assertTrue(transientOne.equals(transientTwo));
    }

    @Test
    @DisplayName("hashCode() should be consistent with speciesId")
    void testHashCode() {
        assertEquals(Objects.hash(1), species.hashCode());

        Species sameIdSpecies = new Species();
        sameIdSpecies.setSpeciesId(1);
        assertEquals(species.hashCode(), sameIdSpecies.hashCode());

        Species differentIdSpecies = new Species();
        differentIdSpecies.setSpeciesId(99);
        assertNotEquals(species.hashCode(), differentIdSpecies.hashCode());
    }

    @Test
    @DisplayName("toString() should format speciesId and name without errors")
    void testToString() {
        String expected = "Species{speciesId=1, name='Macaws'}";
        assertEquals(expected, species.toString());

        Species transientSpecies = new Species();
        assertEquals("Species{speciesId=null, name='null'}", transientSpecies.toString());
    }
}