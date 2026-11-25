package peps.peps_back.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import peps.peps_back.domain.Interaction;
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.Sound;
import peps.peps_back.domain.Species;
import peps.peps_back.repositories.InteractionRepository;
import peps.peps_back.repositories.IoTModuleRepository;
import peps.peps_back.repositories.SoundRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InteractionService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private IoTModuleRepository moduleRepository;

    @Mock
    private SoundRepository soundRepository;

    @InjectMocks
    private InteractionService interactionService;

    private IoTModule araModule;
    private Sound macawSound;
    private Interaction sampleInteraction;

    @BeforeEach
    void setUp() {
        Species macaws = new Species("Macaws", "Interactive audio enrichment enclosure");
        macaws.setSpeciesId(1);

        araModule = new IoTModule("ESP32-Ara-01", "192.168.1.50", "ONLINE", 75, "SEQUENTIAL", macaws);
        araModule.setModuleId(10);

        macawSound = new Sound("Macaw Call", "Vocalization", "mp3", "Vocalization/Macaw_100.mp3", 12);
        macawSound.setSoundId(7);

        sampleInteraction = new Interaction("ROPE_PULL", araModule, macawSound);
        sampleInteraction.setInteractionId(100);
        sampleInteraction.setTriggeredAt(LocalDateTime.of(2026, 11, 25, 10, 30));
    }

    @Test
    @DisplayName("getInteractions() should request descending triggeredAt page and return results")
    void testGetInteractionsPagedSuccess() {
        Page<Interaction> pageResponse = new PageImpl<>(List.of(sampleInteraction));
        when(interactionRepository.findAll(any(Pageable.class))).thenReturn(pageResponse);

        Page<Interaction> result = interactionService.getInteractions(0, 15);

        assertEquals(1, result.getTotalElements());
        assertSame(sampleInteraction, result.getContent().get(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(interactionRepository).findAll(pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        assertEquals(0, usedPageable.getPageNumber());
        assertEquals(15, usedPageable.getPageSize());
        assertEquals(Sort.Direction.DESC, usedPageable.getSort().getOrderFor("triggeredAt").getDirection());
    }

    @Test
    @DisplayName("getInteractions() should reject negative page index or non-positive page size")
    void testGetInteractionsInvalidPagination() {
        assertThrows(IllegalArgumentException.class, () -> interactionService.getInteractions(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> interactionService.getInteractions(0, 0));
        assertThrows(IllegalArgumentException.class, () -> interactionService.getInteractions(0, -5));

        verify(interactionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getInteractionsByModuleAndPeriod() should query repository for valid window")
    void testGetInteractionsByModuleAndPeriodSuccess() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 18, 0);

        when(interactionRepository.findByModule_ModuleIdAndTriggeredAtBetween(10, start, end))
                .thenReturn(List.of(sampleInteraction));

        List<Interaction> results = interactionService.getInteractionsByModuleAndPeriod(10, start, end);

        assertEquals(1, results.size());
        assertSame(sampleInteraction, results.get(0));
        verify(interactionRepository).findByModule_ModuleIdAndTriggeredAtBetween(10, start, end);
    }

    @Test
    @DisplayName("getInteractionsByModuleAndPeriod() should reject invalid module ID or invalid date bounds")
    void testGetInteractionsByModuleAndPeriodValidationFailures() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 14, 0);

        assertThrows(IllegalArgumentException.class,
                () -> interactionService.getInteractionsByModuleAndPeriod(null, start, end));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.getInteractionsByModuleAndPeriod(0, start, end));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.getInteractionsByModuleAndPeriod(10, null, end));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.getInteractionsByModuleAndPeriod(10, start, null));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.getInteractionsByModuleAndPeriod(10, end, start));

        verify(interactionRepository, never())
                .findByModule_ModuleIdAndTriggeredAtBetween(any(), any(), any());
    }

    @Test
    @DisplayName("getInteractionsBySpecies() should query interactions by species ID")
    void testGetInteractionsBySpeciesSuccess() {
        when(interactionRepository.findByModule_Species_SpeciesId(1))
                .thenReturn(List.of(sampleInteraction));

        List<Interaction> results = interactionService.getInteractionsBySpecies(1);

        assertEquals(1, results.size());
        assertEquals("ROPE_PULL", results.get(0).getInteractionType());
        verify(interactionRepository).findByModule_Species_SpeciesId(1);
    }

    @Test
    @DisplayName("getInteractionsBySpecies() should reject null or non-positive species ID")
    void testGetInteractionsBySpeciesInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> interactionService.getInteractionsBySpecies(null));
        assertThrows(IllegalArgumentException.class, () -> interactionService.getInteractionsBySpecies(0));
        assertThrows(IllegalArgumentException.class, () -> interactionService.getInteractionsBySpecies(-2));

        verify(interactionRepository, never()).findByModule_Species_SpeciesId(any());
    }

    @Test
    @DisplayName("recordInteraction() should resolve managed entities, assign default timestamp, and persist")
    void testRecordInteractionWithSoundSuccess() {
        IoTModule moduleRef = new IoTModule();
        moduleRef.setModuleId(10);

        Sound soundRef = new Sound();
        soundRef.setSoundId(7);

        Interaction incoming = new Interaction("  SPRING_TRIGGER  ", moduleRef, soundRef);

        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> {
            Interaction saved = invocation.getArgument(0);
            saved.setInteractionId(101);
            return saved;
        });

        Interaction result = interactionService.recordInteraction(incoming);

        assertEquals(101, result.getInteractionId());
        assertEquals("SPRING_TRIGGER", result.getInteractionType());
        assertSame(araModule, result.getModule());
        assertSame(macawSound, result.getSound());
        assertNotNull(result.getTriggeredAt());
        verify(interactionRepository).save(incoming);
    }

    @Test
    @DisplayName("recordInteraction() should support silent interactions and preserve explicit triggeredAt")
    void testRecordInteractionSilentPreservesExplicitTimestamp() {
        IoTModule moduleRef = new IoTModule();
        moduleRef.setModuleId(10);

        LocalDateTime explicitTime = LocalDateTime.of(2026, 11, 20, 9, 15);
        Interaction incoming = new Interaction("PROXIMITY", moduleRef, null);
        incoming.setTriggeredAt(explicitTime);

        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interaction result = interactionService.recordInteraction(incoming);

        assertNull(result.getSound());
        assertEquals(explicitTime, result.getTriggeredAt());
        verify(soundRepository, never()).findById(any());
        verify(interactionRepository).save(incoming);
    }

    @Test
    @DisplayName("recordInteraction() should reject invalid payloads or missing referenced entities")
    void testRecordInteractionValidationAndMissingEntities() {
        assertThrows(IllegalArgumentException.class, () -> interactionService.recordInteraction(null));

        Interaction blankType = new Interaction("   ", araModule, null);
        assertThrows(IllegalArgumentException.class, () -> interactionService.recordInteraction(blankType));

        String overlyLongType = "X".repeat(InteractionService.MAX_INTERACTION_TYPE_LENGTH + 1);
        Interaction tooLongType = new Interaction(overlyLongType, araModule, null);
        assertThrows(IllegalArgumentException.class, () -> interactionService.recordInteraction(tooLongType));

        Interaction nullModule = new Interaction("BUTTON", null, null);
        assertThrows(IllegalArgumentException.class, () -> interactionService.recordInteraction(nullModule));

        IoTModule transientModule = new IoTModule();
        Interaction nullModuleId = new Interaction("BUTTON", transientModule, null);
        assertThrows(IllegalArgumentException.class, () -> interactionService.recordInteraction(nullModuleId));

        IoTModule unknownModule = new IoTModule();
        unknownModule.setModuleId(99);
        when(moduleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.recordInteraction(new Interaction("BUTTON", unknownModule, null)));

        IoTModule validModuleRef = new IoTModule();
        validModuleRef.setModuleId(10);
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));

        Sound transientSound = new Sound();
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.recordInteraction(new Interaction("BUTTON", validModuleRef, transientSound)));

        Sound unknownSound = new Sound();
        unknownSound.setSoundId(88);
        when(soundRepository.findById(88)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.recordInteraction(new Interaction("BUTTON", validModuleRef, unknownSound)));

        verify(interactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("processIoTTrigger() should create and persist interaction with resolved module and sound")
    void testProcessIoTTriggerWithSoundSuccess() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> {
            Interaction saved = invocation.getArgument(0);
            saved.setInteractionId(205);
            return saved;
        });

        Interaction created = interactionService.processIoTTrigger(10, 7, "  PERCH_SENSOR  ");

        assertEquals(205, created.getInteractionId());
        assertEquals("PERCH_SENSOR", created.getInteractionType());
        assertSame(araModule, created.getModule());
        assertSame(macawSound, created.getSound());
        assertNotNull(created.getTriggeredAt());
        verify(interactionRepository).save(any(Interaction.class));
    }

    @Test
    @DisplayName("processIoTTrigger() should allow null soundId for silent interactions")
    void testProcessIoTTriggerSilentSuccess() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interaction created = interactionService.processIoTTrigger(10, null, "BEAK_TAP");

        assertEquals("BEAK_TAP", created.getInteractionType());
        assertSame(araModule, created.getModule());
        assertNull(created.getSound());
        verify(soundRepository, never()).findById(any());
        verify(interactionRepository).save(any(Interaction.class));
    }

    @Test
    @DisplayName("processIoTTrigger() should reject invalid inputs or non-existent module/sound")
    void testProcessIoTTriggerValidationAndNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(null, 7, "BUTTON"));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(0, 7, "BUTTON"));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(10, 7, null));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(10, 7, "   "));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(10, 7, "A".repeat(51)));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(10, 0, "BUTTON"));
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(10, -2, "BUTTON"));

        when(moduleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(99, 7, "BUTTON"));

        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(soundRepository.findById(88)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> interactionService.processIoTTrigger(10, 88, "BUTTON"));

        verify(interactionRepository, never()).save(any());
    }
}