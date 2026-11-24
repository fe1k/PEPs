package peps.peps_back.repositories;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.Species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link IoTModuleRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class IoTModuleRepositoryTest {

    @Autowired
    private IoTModuleRepository moduleRepository;

    @Autowired
    private SpeciesRepository speciesRepository;

    @BeforeEach
    void setUp() {
        Species ara = speciesRepository.save(new Species("Ara", "Blue-and-yellow macaw"));

        moduleRepository.save(new IoTModule("Rope Module", "192.168.1.10", "ONLINE", 50, "MANUAL", ara));
        moduleRepository.save(new IoTModule("Spring Module", "192.168.1.11", "ONLINE", 70, "AUTO", ara));

        IoTModule disabled = new IoTModule("Old Module", "192.168.1.12", "ONLINE", 30, "MANUAL", ara);
        disabled.setIsActive(false);
        moduleRepository.save(disabled);

        moduleRepository.save(new IoTModule("Offline Module", "192.168.1.13", "OFFLINE", 50, "MANUAL", ara));
    }

    @Test
    @DisplayName("findByIpAddress() should return the module bound to the given IP address")
    void testFindByIpAddressFound() {
        Optional<IoTModule> result = moduleRepository.findByIpAddress("192.168.1.11");

        assertTrue(result.isPresent());
        assertEquals("Spring Module", result.get().getName());
    }

    @Test
    @DisplayName("findByIpAddress() should return an empty Optional for an unknown IP address")
    void testFindByIpAddressNotFound() {
        assertFalse(moduleRepository.findByIpAddress("10.0.0.1").isPresent());
    }

    @Test
    @DisplayName("findByStatusAndIsActive() should filter on both status and activation flag")
    void testFindByStatusAndIsActive() {
        List<IoTModule> activeOnline = moduleRepository.findByStatusAndIsActive("ONLINE", true);
        List<IoTModule> inactiveOnline = moduleRepository.findByStatusAndIsActive("ONLINE", false);

        assertEquals(2, activeOnline.size());
        assertTrue(activeOnline.stream()
                .allMatch(m -> "ONLINE".equals(m.getStatus()) && m.getIsActive()));

        assertEquals(1, inactiveOnline.size());
        assertEquals("Old Module", inactiveOnline.get(0).getName());
    }

    @Test
    @DisplayName("findByStatusAndIsActive() should return an empty list when no module matches")
    void testFindByStatusAndIsActiveNoMatch() {
        assertTrue(moduleRepository.findByStatusAndIsActive("MAINTENANCE", true).isEmpty());
    }
}
