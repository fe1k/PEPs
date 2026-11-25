package peps.peps_back.services;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import peps.peps_back.domain.Sound;
import peps.peps_back.repositories.SoundRepository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SoundService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class SoundServiceTest {

    @Mock
    private SoundRepository soundRepository;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private AudioJobPublisher audioJobPublisher;

    @InjectMocks
    private SoundService soundService;

    private Sound macawSound;
    private byte[] sampleAudioBytes;

    @BeforeEach
    void setUp() {
        macawSound = new Sound("Macaw Call", "Vocalization", "mp3", "Vocalization/Macaw_Call_1000.mp3", 12);
        macawSound.setSoundId(7);

        sampleAudioBytes = "FAKE_MP3_BINARY_DATA".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("getAllSounds() should return all persisted sounds")
    void testGetAllSoundsSuccess() {
        Sound dolphinSound = new Sound("Dolphin Click", "Echolocation", "wav", "Echolocation/Dolphin_2000.wav", 5);
        dolphinSound.setSoundId(8);

        when(soundRepository.findAll()).thenReturn(List.of(macawSound, dolphinSound));

        List<Sound> result = soundService.getAllSounds();

        assertEquals(2, result.size());
        assertEquals("Macaw Call", result.get(0).getName());
        assertEquals("Dolphin Click", result.get(1).getName());
        verify(soundRepository).findAll();
    }

    @Test
    @DisplayName("getAllSounds() should return empty list when no sounds exist")
    void testGetAllSoundsEmpty() {
        when(soundRepository.findAll()).thenReturn(Collections.emptyList());

        List<Sound> result = soundService.getAllSounds();

        assertTrue(result.isEmpty());
        verify(soundRepository).findAll();
    }

    @Test
    @DisplayName("getSoundById() should return matching sound when ID exists")
    void testGetSoundByIdSuccess() {
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));

        Sound result = soundService.getSoundById(7);

        assertSame(macawSound, result);
        verify(soundRepository).findById(7);
    }

    @Test
    @DisplayName("getSoundById() should reject null or non-positive IDs")
    void testGetSoundByIdInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> soundService.getSoundById(null));
        assertThrows(IllegalArgumentException.class, () -> soundService.getSoundById(0));
        assertThrows(IllegalArgumentException.class, () -> soundService.getSoundById(-4));

        verify(soundRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getSoundById() should throw IllegalArgumentException when sound does not exist")
    void testGetSoundByIdNotFound() {
        when(soundRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> soundService.getSoundById(99)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(soundRepository).findById(99);
    }

    @Test
    @DisplayName("getSoundsByType() should trim category and query repository")
    void testGetSoundsByTypeSuccess() {
        when(soundRepository.findBySoundType("Vocalization")).thenReturn(List.of(macawSound));

        List<Sound> result = soundService.getSoundsByType("  Vocalization  ");

        assertEquals(1, result.size());
        assertEquals("Macaw Call", result.get(0).getName());
        verify(soundRepository).findBySoundType("Vocalization");
    }

    @Test
    @DisplayName("getSoundsByType() should reject null or blank type")
    void testGetSoundsByTypeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> soundService.getSoundsByType(null));
        assertThrows(IllegalArgumentException.class, () -> soundService.getSoundsByType("   "));

        verify(soundRepository, never()).findBySoundType(any());
    }

    @Test
    @DisplayName("downloadSoundFile() should resolve MinIO key and return binary payload")
    void testDownloadSoundFileSuccess() throws Exception {
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));
        when(minioStorageService.downloadSound("Vocalization/Macaw_Call_1000.mp3"))
                .thenReturn(sampleAudioBytes);

        byte[] downloaded = soundService.downloadSoundFile(7);

        assertArrayEquals(sampleAudioBytes, downloaded);
        verify(minioStorageService).downloadSound("Vocalization/Macaw_Call_1000.mp3");
    }

    @Test
    @DisplayName("downloadSoundFile() should wrap MinIO checked exceptions in IllegalStateException")
    void testDownloadSoundFileMinioFailure() throws Exception {
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));
        when(minioStorageService.downloadSound("Vocalization/Macaw_Call_1000.mp3"))
                .thenThrow(new Exception("Connection refused"));

        assertThrows(IllegalStateException.class, () -> soundService.downloadSoundFile(7));
    }

    @Test
    @DisplayName("registerNewSound(name, bytes) should upload to MinIO, save metadata, and publish Redis job")
    void testRegisterNewSoundDefaultOverloadSuccess() throws Exception {
        when(minioStorageService.uploadSound(
                eq("Jungle_Dawn"),
                eq("GENERAL"),
                eq("mp3"),
                eq("audio/mpeg"),
                any(InputStream.class),
                eq((long) sampleAudioBytes.length)))
                .thenReturn("GENERAL/Jungle_Dawn_123.mp3");

        when(soundRepository.save(any(Sound.class))).thenAnswer(invocation -> {
            Sound s = invocation.getArgument(0);
            s.setSoundId(15);
            return s;
        });

        Sound created = soundService.registerNewSound("  Jungle_Dawn  ", sampleAudioBytes);

        assertEquals(15, created.getSoundId());
        assertEquals("Jungle_Dawn", created.getName());
        assertEquals("GENERAL", created.getSoundType());
        assertEquals("mp3", created.getExtension());
        assertEquals("GENERAL/Jungle_Dawn_123.mp3", created.getMinioKey());

        String expectedBase64 = Base64.getEncoder().encodeToString(sampleAudioBytes);
        verify(audioJobPublisher).publishUploadJob(
                anyString(),
                eq(15),
                eq("Jungle_Dawn"),
                eq(expectedBase64),
                eq("audio/mpeg")
        );
    }

    @Test
    @DisplayName("registerNewSound() should support wav, ogg, m4a formats and fallback defaults")
    void testRegisterNewSoundAllFormatsAndFallbacks() throws Exception {
        when(minioStorageService.uploadSound(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(InputStream.class),
                anyLong()))
                .thenReturn("category/test_key");

        when(soundRepository.save(any(Sound.class))).thenAnswer(invocation -> {
            Sound s = invocation.getArgument(0);
            s.setSoundId(20);
            return s;
        });

        // WAV with leading dot and uppercase
        Sound wavSound = soundService.registerNewSound("Whistle", "Echolocation", ".WAV", 8, sampleAudioBytes);
        assertEquals("wav", wavSound.getExtension());
        assertEquals(8, wavSound.getDurationSeconds());
        verify(audioJobPublisher).publishUploadJob(anyString(), eq(20), eq("Whistle"), anyString(), eq("audio/wav"));

        // OGG
        Sound oggSound = soundService.registerNewSound("Rainforest", "Ambient", "ogg", 30, sampleAudioBytes);
        assertEquals("ogg", oggSound.getExtension());
        verify(audioJobPublisher).publishUploadJob(anyString(), eq(20), eq("Rainforest"), anyString(), eq("audio/ogg"));

        // M4A
        Sound m4aSound = soundService.registerNewSound("Saimiri Call", "Alarm", "m4a", 4, sampleAudioBytes);
        assertEquals("m4a", m4aSound.getExtension());
        verify(audioJobPublisher).publishUploadJob(anyString(), eq(20), eq("Saimiri Call"), anyString(), eq("audio/mp4"));

        // Blank type & blank extension fallback
        Sound fallbackSound = soundService.registerNewSound("Fallback", "   ", "   ", null, sampleAudioBytes);
        assertEquals("GENERAL", fallbackSound.getSoundType());
        assertEquals("mp3", fallbackSound.getExtension());
    }

    @Test
    @DisplayName("registerNewSound() should reject invalid inputs before touching external services")
    void testRegisterNewSoundValidationFailures() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> soundService.registerNewSound(null, sampleAudioBytes));
        assertThrows(IllegalArgumentException.class,
                () -> soundService.registerNewSound("   ", sampleAudioBytes));
        assertThrows(IllegalArgumentException.class,
                () -> soundService.registerNewSound("Valid", null));
        assertThrows(IllegalArgumentException.class,
                () -> soundService.registerNewSound("Valid", new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> soundService.registerNewSound("Valid", "Ambient", "mp3", -1, sampleAudioBytes));
        assertThrows(IllegalArgumentException.class,
                () -> soundService.registerNewSound("Valid", "Ambient", "flac", 10, sampleAudioBytes));

        verify(minioStorageService, never()).uploadSound(any(), any(), any(), any(), any(), anyLong());
        verify(soundRepository, never()).save(any());
        verify(audioJobPublisher, never()).publishUploadJob(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("registerNewSound() should wrap MinIO upload exception in IllegalStateException")
    void testRegisterNewSoundMinioUploadFailure() throws Exception {
        when(minioStorageService.uploadSound(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(InputStream.class),
                anyLong()))
                .thenThrow(new Exception("MinIO bucket unreachable"));

        assertThrows(IllegalStateException.class,
                () -> soundService.registerNewSound("Broken_Upload", sampleAudioBytes));

        verify(soundRepository, never()).save(any());
        verify(audioJobPublisher, never()).publishUploadJob(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("registerNewSound() should compensate by deleting MinIO object if DB or Redis fails")
    void testRegisterNewSoundRollbackOnPersistenceFailure() throws Exception {
        when(minioStorageService.uploadSound(
                eq("Broken_Audio"),
                eq("GENERAL"),
                eq("mp3"),
                eq("audio/mpeg"),
                any(InputStream.class),
                anyLong()))
                .thenReturn("GENERAL/Broken_Audio_999.mp3");
        when(soundRepository.save(any(Sound.class)))
                .thenThrow(new IllegalStateException("PostgreSQL constraint failure"));

        assertThrows(IllegalStateException.class,
                () -> soundService.registerNewSound("Broken_Audio", sampleAudioBytes));

        verify(minioStorageService).deleteSound("GENERAL/Broken_Audio_999.mp3");
        verify(audioJobPublisher, never()).publishUploadJob(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("registerNewSound() should still rethrow original exception if compensating MinIO delete fails")
    void testRegisterNewSoundRollbackCleanupFailure() throws Exception {
        when(minioStorageService.uploadSound(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(InputStream.class),
                anyLong()))
                .thenReturn("GENERAL/Orphan_Audio_999.mp3");
        when(soundRepository.save(any(Sound.class)))
                .thenThrow(new IllegalStateException("DB down"));
        doThrow(new Exception("Cleanup failed"))
                .when(minioStorageService).deleteSound("GENERAL/Orphan_Audio_999.mp3");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> soundService.registerNewSound("Orphan_Audio", sampleAudioBytes));

        assertEquals("DB down", thrown.getMessage());
        verify(minioStorageService).deleteSound("GENERAL/Orphan_Audio_999.mp3");
    }

    @Test
    @DisplayName("requestDeviceAudioPreparation() should publish retrieval job to Redis and return jobId")
    void testRequestDeviceAudioPreparationSuccess() {
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));

        String jobId = soundService.requestDeviceAudioPreparation(7);

        assertNotNull(jobId);
        verify(audioJobPublisher).publishRetrievalJob(jobId, 7);
    }

    @Test
    @DisplayName("deleteSound() should remove binary object from MinIO and delete DB record")
    void testDeleteSoundSuccess() throws Exception {
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));

        soundService.deleteSound(7);

        ArgumentCaptor<Sound> captor = ArgumentCaptor.forClass(Sound.class);
        verify(minioStorageService).deleteSound("Vocalization/Macaw_Call_1000.mp3");
        verify(soundRepository).delete(captor.capture());
        assertSame(macawSound, captor.getValue());
    }

    @Test
    @DisplayName("deleteSound() should reject invalid ID or non-existent sound")
    void testDeleteSoundInvalidOrNotFound() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> soundService.deleteSound(null));
        assertThrows(IllegalArgumentException.class, () -> soundService.deleteSound(0));

        when(soundRepository.findById(404)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> soundService.deleteSound(404));

        verify(minioStorageService, never()).deleteSound(any());
        verify(soundRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteSound() should wrap MinIO deletion exception and abort DB deletion")
    void testDeleteSoundMinioFailureAbortsDbDelete() throws Exception {
        when(soundRepository.findById(7)).thenReturn(Optional.of(macawSound));
        doThrow(new Exception("MinIO unreachable"))
                .when(minioStorageService).deleteSound("Vocalization/Macaw_Call_1000.mp3");

        assertThrows(IllegalStateException.class, () -> soundService.deleteSound(7));

        verify(soundRepository, never()).delete(any());
    }
}