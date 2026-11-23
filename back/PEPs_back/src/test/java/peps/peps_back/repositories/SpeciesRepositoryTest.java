package peps.peps_back.repositories;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link SpeciesRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class SpeciesRepositoryTest {

    @Autowired
    private SpeciesRepository speciesRepository;

    @BeforeEach
    void setUp() {
        speciesRepository.save(new Species("Ara", "Blue-and-yellow macaw"));
        speciesRepository.save(new Species("Lion", "African lion"));
    }

    @Test
    @DisplayName("findBySpeciesName() should return the species with the given name")
    void testFindBySpeciesNameFound() {
        Optional<Species> result = speciesRepository.findBySpeciesName("Ara");

        assertTrue(result.isPresent());
        assertEquals("Blue-and-yellow macaw", result.get().getDescription());
    }

    @Test
    @DisplayName("findBySpeciesName() should return an empty Optional for an unknown name")
    void testFindBySpeciesNameNotFound() {
        assertFalse(speciesRepository.findBySpeciesName("Tiger").isPresent());
    }
}
