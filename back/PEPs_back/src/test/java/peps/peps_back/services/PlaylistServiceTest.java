package peps.peps_back.services;

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
import peps.peps_back.domain.Playlist;
import peps.peps_back.domain.PlaylistSound;
import peps.peps_back.domain.Sound;
import peps.peps_back.domain.Species;
import peps.peps_back.repositories.IoTModuleRepository;
import peps.peps_back.repositories.PlaylistRepository;
import peps.peps_back.repositories.PlaylistSoundRepository;
import peps.peps_back.repositories.SoundRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlaylistService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistSoundRepository playlistSoundRepository;

    @Mock
    private SoundRepository soundRepository;

    @Mock
    private IoTModuleRepository moduleRepository;

    @InjectMocks
    private PlaylistService playlistService;

    private Playlist morningPlaylist;
    private Sound macawSound;
    private IoTModule araModule;

    @BeforeEach
    void setUp() {
        morningPlaylist = new Playlist("Macaws Morning Enrichment", "SEQUENTIAL");
        morningPlaylist.setPlaylistId(1);

        macawSound = new Sound("Macaw Call", "Vocalization", "mp3", "sounds/macaw-call.mp3", 12);
        macawSound.setSoundId(7);

        Species macaws = new Species("Macaws", "Interactive audio enrichment enclosure");
        macaws.setSpeciesId(1);

        araModule = new IoTModule("ESP32-Ara-01", "192.168.1.50", "ONLINE", 75, "SEQUENTIAL", macaws);
        araModule.setModuleId(10);
    }

    @Test
    @DisplayName("getAllPlaylists() should return all persisted playlists")
    void testGetAllPlaylistsSuccess() {
        Playlist afternoonPlaylist = new Playlist("Afternoon Calm", "RANDOM");
        afternoonPlaylist.setPlaylistId(2);

        when(playlistRepository.findAll()).thenReturn(List.of(morningPlaylist, afternoonPlaylist));

        List<Playlist> result = playlistService.getAllPlaylists();

        assertEquals(2, result.size());
        assertEquals("Macaws Morning Enrichment", result.get(0).getName());
        assertEquals("Afternoon Calm", result.get(1).getName());
        verify(playlistRepository).findAll();
    }

    @Test
    @DisplayName("getAllPlaylists() should return empty list when no playlists exist")
    void testGetAllPlaylistsEmpty() {
        when(playlistRepository.findAll()).thenReturn(Collections.emptyList());

        List<Playlist> result = playlistService.getAllPlaylists();

        assertTrue(result.isEmpty());
        verify(playlistRepository).findAll();
    }

    @Test
    @DisplayName("getPlaylistById() should return matching playlist when ID exists")
    void testGetPlaylistByIdSuccess() {
        when(playlistRepository.findById(1)).thenReturn(Optional.of(morningPlaylist));

        Playlist result = playlistService.getPlaylistById(1);

        assertSame(morningPlaylist, result);
        verify(playlistRepository).findById(1);
    }

    @Test
    @DisplayName("getPlaylistById() should reject null or non-positive IDs")
    void testGetPlaylistByIdInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> playlistService.getPlaylistById(null));
        assertThrows(IllegalArgumentException.class, () -> playlistService.getPlaylistById(0));
        assertThrows(IllegalArgumentException.class, () -> playlistService.getPlaylistById(-3));

        verify(playlistRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getPlaylistById() should throw IllegalArgumentException when playlist does not exist")
    void testGetPlaylistByIdNotFound() {
        when(playlistRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> playlistService.getPlaylistById(99)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(playlistRepository).findById(99);
    }

    @Test
    @DisplayName("getPlaylistSounds() should return ordered entries when playlist exists")
    void testGetPlaylistSoundsSuccess() {
        PlaylistSound entry1 = new PlaylistSound(morningPlaylist, macawSound, 1);
        when(playlistRepository.existsById(1)).thenReturn(true);
        when(playlistSoundRepository.findByPlaylist_PlaylistIdOrderByPlayOrderAsc(1))
                .thenReturn(List.of(entry1));

        List<PlaylistSound> entries = playlistService.getPlaylistSounds(1);

        assertEquals(1, entries.size());
        assertEquals(1, entries.get(0).getPlayOrder());
        verify(playlistSoundRepository).findByPlaylist_PlaylistIdOrderByPlayOrderAsc(1);
    }

    @Test
    @DisplayName("getPlaylistSounds() should reject invalid ID or non-existent playlist")
    void testGetPlaylistSoundsInvalidOrNotFound() {
        assertThrows(IllegalArgumentException.class, () -> playlistService.getPlaylistSounds(null));
        assertThrows(IllegalArgumentException.class, () -> playlistService.getPlaylistSounds(0));

        when(playlistRepository.existsById(404)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> playlistService.getPlaylistSounds(404));

        verify(playlistSoundRepository, never()).findByPlaylist_PlaylistIdOrderByPlayOrderAsc(any());
    }

    @Test
    @DisplayName("createPlaylist(name) should trim name, default to SEQUENTIAL, and persist")
    void testCreatePlaylistDefaultModeSuccess() {
        when(playlistRepository.findByName("Tropical Dawn")).thenReturn(Optional.empty());
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(invocation -> {
            Playlist p = invocation.getArgument(0);
            p.setPlaylistId(5);
            return p;
        });

        Playlist created = playlistService.createPlaylist("  Tropical Dawn  ");

        assertEquals(5, created.getPlaylistId());
        assertEquals("Tropical Dawn", created.getName());
        assertEquals("SEQUENTIAL", created.getPlaybackMode());
        verify(playlistRepository).save(any(Playlist.class));
    }

    @Test
    @DisplayName("createPlaylist(name, mode) should support RANDOM mode and fallback to SEQUENTIAL when mode is null or blank")
    void testCreatePlaylistExplicitAndBlankModes() {
        when(playlistRepository.findByName("Random Mix")).thenReturn(Optional.empty());
        when(playlistRepository.findByName("Null Mode Mix")).thenReturn(Optional.empty());
        when(playlistRepository.findByName("Blank Mode Mix")).thenReturn(Optional.empty());
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Playlist randomPlaylist = playlistService.createPlaylist("Random Mix", "  random  ");
        assertEquals("RANDOM", randomPlaylist.getPlaybackMode());

        Playlist nullModePlaylist = playlistService.createPlaylist("Null Mode Mix", null);
        assertEquals("SEQUENTIAL", nullModePlaylist.getPlaybackMode());

        Playlist blankModePlaylist = playlistService.createPlaylist("Blank Mode Mix", "   ");
        assertEquals("SEQUENTIAL", blankModePlaylist.getPlaybackMode());
    }

    @Test
    @DisplayName("createPlaylist() should reject null, blank, duplicate names, or unsupported modes")
    void testCreatePlaylistValidationFailures() {
        assertThrows(IllegalArgumentException.class, () -> playlistService.createPlaylist(null));
        assertThrows(IllegalArgumentException.class, () -> playlistService.createPlaylist("   "));

        when(playlistRepository.findByName("Macaws Morning Enrichment"))
                .thenReturn(Optional.of(morningPlaylist));
        assertThrows(IllegalArgumentException.class,
                () -> playlistService.createPlaylist("Macaws Morning Enrichment"));

        when(playlistRepository.findByName("Valid Name")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> playlistService.createPlaylist("Valid Name", "SHUFFLE_PRO"));

        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("addSoundToPlaylist() should compute next 1-based playOrder and persist association")
    void testAddSoundToPlaylistSuccess() {
        when(playlistRepository.findById(1)).thenReturn(Optional.of(morningPlaylist));
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));
        when(playlistSoundRepository.countByPlaylist_PlaylistId(1)).thenReturn(2L);
        when(playlistSoundRepository.save(any(PlaylistSound.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playlistService.addSoundToPlaylist(1, 7);

        ArgumentCaptor<PlaylistSound> captor = ArgumentCaptor.forClass(PlaylistSound.class);
        verify(playlistSoundRepository).save(captor.capture());

        PlaylistSound savedEntry = captor.getValue();
        assertSame(morningPlaylist, savedEntry.getPlaylist());
        assertSame(macawSound, savedEntry.getSound());
        assertEquals(3, savedEntry.getPlayOrder());
    }

    @Test
    @DisplayName("addSoundToPlaylist() should reject invalid IDs or missing entities")
    void testAddSoundToPlaylistValidationAndNotFound() {
        assertThrows(IllegalArgumentException.class, () -> playlistService.addSoundToPlaylist(null, 7));
        assertThrows(IllegalArgumentException.class, () -> playlistService.addSoundToPlaylist(0, 7));
        assertThrows(IllegalArgumentException.class, () -> playlistService.addSoundToPlaylist(1, null));
        assertThrows(IllegalArgumentException.class, () -> playlistService.addSoundToPlaylist(1, -1));

        when(playlistRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> playlistService.addSoundToPlaylist(99, 7));

        when(playlistRepository.findById(1)).thenReturn(Optional.of(morningPlaylist));
        when(soundRepository.findById(88)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> playlistService.addSoundToPlaylist(1, 88));

        verify(playlistSoundRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignPlaylistToModule() should link playlist to IoT module and persist")
    void testAssignPlaylistToModuleSuccess() {
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(playlistRepository.findById(1)).thenReturn(Optional.of(morningPlaylist));
        when(moduleRepository.save(any(IoTModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playlistService.assignPlaylistToModule(10, 1);

        assertSame(morningPlaylist, araModule.getPlaylist());
        verify(moduleRepository).save(araModule);
    }

    @Test
    @DisplayName("assignPlaylistToModule() should clear module playlist when playlistId is null")
    void testAssignPlaylistToModuleUnassign() {
        araModule.setPlaylist(morningPlaylist);
        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        when(moduleRepository.save(any(IoTModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        playlistService.assignPlaylistToModule(10, null);

        assertNull(araModule.getPlaylist());
        verify(playlistRepository, never()).findById(any());
        verify(moduleRepository).save(araModule);
    }

    @Test
    @DisplayName("assignPlaylistToModule() should reject invalid IDs or missing module/playlist")
    void testAssignPlaylistToModuleValidationAndNotFound() {
        assertThrows(IllegalArgumentException.class, () -> playlistService.assignPlaylistToModule(null, 1));
        assertThrows(IllegalArgumentException.class, () -> playlistService.assignPlaylistToModule(0, 1));

        when(moduleRepository.findById(404)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> playlistService.assignPlaylistToModule(404, 1));

        when(moduleRepository.findById(10)).thenReturn(Optional.of(araModule));
        assertThrows(IllegalArgumentException.class, () -> playlistService.assignPlaylistToModule(10, -5));

        when(playlistRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> playlistService.assignPlaylistToModule(10, 99));

        verify(moduleRepository, never()).save(any());
    }
}