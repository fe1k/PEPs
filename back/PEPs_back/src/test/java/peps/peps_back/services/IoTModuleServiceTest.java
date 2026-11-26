package peps.peps_back.services;

import java.time.LocalDateTime;
import java.util.Collections;
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
import peps.peps_back.domain.IoTModule;
import peps.peps_back.domain.Species;
import peps.peps_back.repositories.IoTModuleRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IoTModuleService} covering module retrieval,
 * configuration validation/updates, and heartbeat registration.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class IoTModuleServiceTest {

    @Mock
    private IoTModuleRepository moduleRepository;

    @InjectMocks
    private IoTModuleService ioTModuleService;

    private Species macaws;
    private IoTModule araModule;

    @BeforeEach
    void setUp() {
        macaws = new Species("Macaws", "Interactive audio enrichment enclosure");
        macaws.setSpeciesId(1);

        araModule = new IoTModule("ESP32-Ara-01", "192.168.1.50", "OFFLINE", 50, "MANUAL", macaws);
        araModule.setModuleId(10);
        araModule.setLastSeenAt(LocalDateTime.of(2026, 11, 20, 10, 0, 0));
    }

    @Test
    @DisplayName("getAllModules() should return all persisted IoT modules")
    void testGetAllModulesSuccess() {
        IoTModule dolphinModule = new IoTModule(
                "Hydro-Dolphin-01", "192.168.1.60", "ONLINE", 80, "RANDOM", macaws
        );
        dolphinModule.setModuleId(11);

        when(moduleRepository.findAll()).thenReturn(List.of(araModule, dolphinModule));

        List<IoTModule> result = ioTModuleService.getAllModules();

        assertEquals(2, result.size());
        assertEquals("ESP32-Ara-01", result.get(0).getName());
        assertEquals("Hydro-Dolphin-01", result.get(1).getName());
        verify(moduleRepository).findAll();
    }

    @Test
    @DisplayName("getAllModules() should return empty list when no modules are registered")
    void testGetAllModulesEmpty() {
        when(moduleRepository.findAll()).thenReturn(Collections.emptyList());

        List<IoTModule> result = ioTModuleService.getAllModules();

        assertTrue(result.isEmpty());
        verify(moduleRepository).findAll();
    }

    @Test
    @DisplayName("getModuleById() should return matching module when ID exists")
    void testGetModuleByIdSuccess() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));

        IoTModule result = ioTModuleService.getModuleById(10);

        assertSame(araModule, result);
        verify(moduleRepository).findById(10);
    }

    @Test
    @DisplayName("getModuleById() should throw IllegalArgumentException when ID is null or non-positive")
    void testGetModuleByIdInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.getModuleById(null));
        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.getModuleById(0));
        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.getModuleById(-5));

        verify(moduleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getModuleById() should throw IllegalArgumentException when module does not exist")
    void testGetModuleByIdNotFound() {
        when(moduleRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ioTModuleService.getModuleById(99)
        );

        assertTrue(exception.getMessage().contains("99"));
        verify(moduleRepository).findById(99);
    }

    @Test
    @DisplayName("updateModuleConfig() should normalize mode, update volume, and persist module")
    void testUpdateModuleConfigSuccess() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(moduleRepository.save(any(IoTModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IoTModule updated = ioTModuleService.updateModuleConfig(10, 85, "  random  ");

        assertEquals(85, updated.getVolume());
        assertEquals("RANDOM", updated.getCurrentMode());
        verify(moduleRepository).findById(10);
        verify(moduleRepository).save(araModule);
    }

    @Test
    @DisplayName("updateModuleConfig() should accept boundary volumes (0 and 100) and all allowed modes")
    void testUpdateModuleConfigBoundariesAndAllModes() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(moduleRepository.save(any(IoTModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IoTModule minVolManual = ioTModuleService.updateModuleConfig(10, 0, "MANUAL");
        assertEquals(0, minVolManual.getVolume());
        assertEquals("MANUAL", minVolManual.getCurrentMode());

        IoTModule maxVolProgrammed = ioTModuleService.updateModuleConfig(10, 100, "PROGRAMMED");
        assertEquals(100, maxVolProgrammed.getVolume());
        assertEquals("PROGRAMMED", maxVolProgrammed.getCurrentMode());

        IoTModule sequential = ioTModuleService.updateModuleConfig(10, 40, "SEQUENTIAL");
        assertEquals(40, sequential.getVolume());
        assertEquals("SEQUENTIAL", sequential.getCurrentMode());
    }

    @Test
    @DisplayName("updateModuleConfig() should reject null or non-positive module IDs")
    void testUpdateModuleConfigInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(null, 50, "MANUAL"));
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(0, 50, "MANUAL"));
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(-1, 50, "MANUAL"));

        verify(moduleRepository, never()).findById(any());
        verify(moduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateModuleConfig() should reject null or out-of-range volume values")
    void testUpdateModuleConfigInvalidVolume() {
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(10, null, "MANUAL"));
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(10, -1, "MANUAL"));
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(10, 101, "MANUAL"));

        verify(moduleRepository, never()).findById(any());
        verify(moduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateModuleConfig() should reject null, blank, or unsupported playback modes")
    void testUpdateModuleConfigInvalidMode() {
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(10, 50, null));
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(10, 50, "   "));
        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(10, 50, "TURBO"));

        verify(moduleRepository, never()).findById(any());
        verify(moduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateModuleConfig() should throw IllegalArgumentException when module does not exist")
    void testUpdateModuleConfigNotFound() {
        when(moduleRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> ioTModuleService.updateModuleConfig(404, 60, "RANDOM"));

        verify(moduleRepository).findById(404);
        verify(moduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerHeartbeat() should mark module ONLINE and update lastSeenAt timestamp")
    void testRegisterHeartbeatSuccess() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(moduleRepository.save(any(IoTModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeHeartbeat = LocalDateTime.now().minusSeconds(1);
        ioTModuleService.registerHeartbeat(10);
        LocalDateTime afterHeartbeat = LocalDateTime.now().plusSeconds(1);

        ArgumentCaptor<IoTModule> captor = ArgumentCaptor.forClass(IoTModule.class);
        verify(moduleRepository).save(captor.capture());

        IoTModule savedModule = captor.getValue();
        assertEquals("ONLINE", savedModule.getStatus());
        assertNotNull(savedModule.getLastSeenAt());
        assertTrue(savedModule.getLastSeenAt().isAfter(beforeHeartbeat));
        assertTrue(savedModule.getLastSeenAt().isBefore(afterHeartbeat));
    }

    @Test
    @DisplayName("registerHeartbeat() should reject null or non-positive module IDs")
    void testRegisterHeartbeatInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.registerHeartbeat(null));
        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.registerHeartbeat(0));
        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.registerHeartbeat(-2));

        verify(moduleRepository, never()).findById(any());
        verify(moduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerHeartbeat() should throw IllegalArgumentException when module does not exist")
    void testRegisterHeartbeatNotFound() {
        when(moduleRepository.findById(77)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> ioTModuleService.registerHeartbeat(77));

        verify(moduleRepository).findById(77);
        verify(moduleRepository, never()).save(any());
    }
}