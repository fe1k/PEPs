package peps.peps_back.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AudioJobPublisher}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AudioJobPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private AudioJobPublisher audioJobPublisher;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        audioJobPublisher = new AudioJobPublisher(
                redisTemplate,
                AudioJobPublisher.DEFAULT_UPLOAD_STREAM,
                AudioJobPublisher.DEFAULT_RETRIEVAL_STREAM
        );
    }

    @Test
    void constructor_WithNullOrBlankStreamKeys_ShouldFallbackToDefaults() {
        AudioJobPublisher publisherWithNulls = new AudioJobPublisher(redisTemplate, null, null);
        AudioJobPublisher publisherWithBlanks = new AudioJobPublisher(redisTemplate, "   ", "  ");

        when(streamOperations.add(eq(AudioJobPublisher.DEFAULT_UPLOAD_STREAM), anyMap()))
                .thenReturn(RecordId.of("1700000000000-0"));
        when(streamOperations.add(eq(AudioJobPublisher.DEFAULT_RETRIEVAL_STREAM), anyMap()))
                .thenReturn(RecordId.of("1700000000001-0"));

        publisherWithNulls.publishUploadJob("job-1", 1, "Ara Call", "UklGRg==", "audio/wav");
        publisherWithBlanks.publishRetrievalJob("job-2", 2);

        verify(streamOperations).add(eq(AudioJobPublisher.DEFAULT_UPLOAD_STREAM), anyMap());
        verify(streamOperations).add(eq(AudioJobPublisher.DEFAULT_RETRIEVAL_STREAM), anyMap());
    }

    @Test
    void publishUploadJob_WithInvalidArguments_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob(null, 1, "Ara", "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("   ", 1, "Ara", "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", null, "Ara", "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", 0, "Ara", "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", -3, "Ara", "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", 1, null, "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", 1, "  ", "UklGRg==", "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", 1, "Ara", null, "audio/wav"));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishUploadJob("job-1", 1, "Ara", "   ", "audio/wav"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishUploadJob_WithValidPayload_ShouldPublishExactWorkerContractToUploadStream() {
        when(streamOperations.add(eq("upload-audio-stream"), anyMap()))
                .thenReturn(RecordId.of("1700000000123-0"));

        audioJobPublisher.publishUploadJob(
                "uuid-upload-123",
                42,
                "Saimiri Alert",
                "SUQzBAAAAAA=",
                "audio/mpeg"
        );

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("upload-audio-stream"), payloadCaptor.capture());

        Map<String, String> payload = payloadCaptor.getValue();
        assertEquals(5, payload.size());
        assertEquals("uuid-upload-123", payload.get("jobId"));
        assertEquals("42", payload.get("soundId"));
        assertEquals("Saimiri Alert", payload.get("soundName"));
        assertEquals("SUQzBAAAAAA=", payload.get("audioBase64"));
        assertEquals("audio/mpeg", payload.get("contentType"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishUploadJob_WithNullOrBlankContentType_ShouldDefaultToOctetStream() {
        when(streamOperations.add(eq("upload-audio-stream"), anyMap()))
                .thenReturn(RecordId.of("1700000000124-0"));

        audioJobPublisher.publishUploadJob("job-null-ct", 10, "Dolphin Click", "T2dnUw==", null);
        audioJobPublisher.publishUploadJob("job-blank-ct", 11, "Dolphin Whistle", "T2dnUw==", "   ");

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations, org.mockito.Mockito.times(2))
                .add(eq("upload-audio-stream"), payloadCaptor.capture());

        assertEquals("application/octet-stream", payloadCaptor.getAllValues().get(0).get("contentType"));
        assertEquals("application/octet-stream", payloadCaptor.getAllValues().get(1).get("contentType"));
    }

    @Test
    void publishUploadJob_WhenRedisFails_ShouldThrowIllegalStateException() {
        when(streamOperations.add(eq("upload-audio-stream"), anyMap()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> audioJobPublisher.publishUploadJob("job-err", 7, "Macaw", "UklGRg==", "audio/wav"));

        assertTrue(ex.getMessage().contains("upload-audio-stream"));
        assertTrue(ex.getMessage().contains("soundId=7"));
    }

    @Test
    void publishRetrievalJob_WithInvalidArguments_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishRetrievalJob(null, 5));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishRetrievalJob("   ", 5));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishRetrievalJob("job-ret-1", null));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishRetrievalJob("job-ret-1", 0));
        assertThrows(IllegalArgumentException.class,
                () -> audioJobPublisher.publishRetrievalJob("job-ret-1", -1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishRetrievalJob_WithValidPayload_ShouldPublishExactWorkerContractToRetrievalStream() {
        AudioJobPublisher customPublisher = new AudioJobPublisher(
                redisTemplate, "  custom-upload  ", "  retrieval-request-stream  ");
        when(streamOperations.add(eq("retrieval-request-stream"), anyMap()))
                .thenReturn(RecordId.of("1700000000200-0"));

        customPublisher.publishRetrievalJob("uuid-ret-999", 15);

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("retrieval-request-stream"), payloadCaptor.capture());

        Map<String, String> payload = payloadCaptor.getValue();
        assertEquals(2, payload.size());
        assertEquals("uuid-ret-999", payload.get("jobId"));
        assertEquals("15", payload.get("soundId"));
    }

    @Test
    void publishRetrievalJob_WhenRedisFails_ShouldThrowIllegalStateException() {
        when(streamOperations.add(eq("retrieval-request-stream"), anyMap()))
                .thenThrow(new RuntimeException("Redis timeout"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> audioJobPublisher.publishRetrievalJob("job-ret-err", 9));

        assertTrue(ex.getMessage().contains("retrieval-request-stream"));
        assertTrue(ex.getMessage().contains("soundId=9"));
    }
}