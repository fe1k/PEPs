package peps.peps_back.repositories;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import peps.peps_back.domain.Interaction;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.Species;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link InteractionRepository} against an H2 in-memory database.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@SpringJUnitConfig(RepositoryTestConfig.class)
@Transactional
class InteractionRepositoryTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 11, 20, 10, 0);

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private IoTModuleRepository moduleRepository;

    @Autowired
    private SpeciesRepository speciesRepository;

    private Species ara;
    private Species lion;
    private IoTModule araModule;
    private IoTModule lionModule;

    @BeforeEach
    void setUp() {
        // saveAndFlush(): EclipseLink only runs the INSERT (and assigns the IDENTITY id) on flush,
        // so a plain save() would leave the id null when it is used below.
        ara = speciesRepository.saveAndFlush(new Species("Ara", "Blue-and-yellow macaw"));
        lion = speciesRepository.saveAndFlush(new Species("Lion", "African lion"));

        araModule = moduleRepository.saveAndFlush(new IoTModule("Ara Rope", "192.168.1.10", "ONLINE", 50, "MANUAL", ara));
        lionModule = moduleRepository.saveAndFlush(new IoTModule("Lion Ball", "192.168.1.20", "ONLINE", 50, "MANUAL", lion));

        saveInteraction("ROPE_PULL", araModule, BASE_TIME);
        saveInteraction("ROPE_PULL", araModule, BASE_TIME.plusHours(2));
        saveInteraction("ROPE_PULL", araModule, BASE_TIME.plusDays(1));
        saveInteraction("CONTACT", lionModule, BASE_TIME.plusHours(1));
    }

    private void saveInteraction(String type, IoTModule module, LocalDateTime triggeredAt) {
        Interaction interaction = new Interaction(type, module, null);
        interaction.setTriggeredAt(triggeredAt);
        interactionRepository.save(interaction);
    }

    @Test
    @DisplayName("findByModule_ModuleIdAndTriggeredAtBetween() should return the module's interactions within the range")
    void testFindByModuleAndTimeRange() {
        List<Interaction> result = interactionRepository.findByModule_ModuleIdAndTriggeredAtBetween(
                araModule.getModuleId(), BASE_TIME, BASE_TIME.plusHours(3));

        assertEquals(2, result.size());
        assertTrue(result.stream()
                .allMatch(i -> i.getModule().getModuleId().equals(araModule.getModuleId())));
    }

    @Test
    @DisplayName("findByModule_ModuleIdAndTriggeredAtBetween() should include both range boundaries")
    void testFindByModuleAndTimeRangeInclusiveBounds() {
        List<Interaction> result = interactionRepository.findByModule_ModuleIdAndTriggeredAtBetween(
                araModule.getModuleId(), BASE_TIME, BASE_TIME.plusHours(2));

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findByModule_ModuleIdAndTriggeredAtBetween() should return an empty list outside the range")
    void testFindByModuleAndTimeRangeNoMatch() {
        assertTrue(interactionRepository.findByModule_ModuleIdAndTriggeredAtBetween(
                araModule.getModuleId(), BASE_TIME.minusDays(5), BASE_TIME.minusDays(4)).isEmpty());
    }

    @Test
    @DisplayName("findByModule_Species_SpeciesId() should return the interactions of the species' modules")
    void testFindByModuleSpecies() {
        List<Interaction> araInteractions = interactionRepository.findByModule_Species_SpeciesId(ara.getSpeciesId());
        List<Interaction> lionInteractions = interactionRepository.findByModule_Species_SpeciesId(lion.getSpeciesId());

        assertEquals(3, araInteractions.size());
        assertEquals(1, lionInteractions.size());
        assertEquals("CONTACT", lionInteractions.get(0).getInteractionType());
    }
}
