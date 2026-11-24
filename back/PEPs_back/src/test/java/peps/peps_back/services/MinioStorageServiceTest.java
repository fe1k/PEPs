package peps.peps_back.services;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MinioStorageService}.
 *
 * @author Franco Valdebenito
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    private static final String BUCKET_NAME = "peps-sounds";

    @Mock
    private MinioClient minioClient;

    private MinioStorageService minioStorageService;

    @BeforeEach
    void setUp() {
        minioStorageService = new MinioStorageService(minioClient, BUCKET_NAME);
    }

    @Test
    void publicConstructor_ShouldInitializeClientWithoutThrowing() {
        MinioStorageService service = new MinioStorageService(
                "http://localhost:9000", "minioadmin", "minioadmin", "peps-sounds");
        assertNotNull(service);
    }

    @Test
    void initializeBucket_WhenBucketDoesNotExist_ShouldCreateBucketAndApplyPrivatePolicy() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioStorageService.initializeBucket();

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    void initializeBucket_WhenBucketAlreadyExists_ShouldOnlyApplyPrivatePolicy() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioStorageService.initializeBucket();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    void initializeBucket_WhenMinioFails_ShouldSwallowExceptionGracefully() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("MinIO unreachable"));

        minioStorageService.initializeBucket();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void uploadSound_WithInvalidArguments_ShouldThrowIllegalArgumentException() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound(null, "recompense", "mp3", "audio/mpeg", stream, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("   ", "recompense", "mp3", "audio/mpeg", stream, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", null, "mp3", "audio/mpeg", stream, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", " ", "mp3", "audio/mpeg", stream, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", "recompense", null, "audio/mpeg", stream, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", "recompense", " ", "audio/mpeg", stream, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", "recompense", "mp3", "audio/mpeg", null, 3));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", "recompense", "mp3", "audio/mpeg", stream, 0));
        assertThrows(IllegalArgumentException.class,
                () -> minioStorageService.uploadSound("Ara Call", "recompense", "mp3", "audio/mpeg", stream, -5));
    }

    @Test
    void uploadSound_WithValidInputAndExistingBucket_ShouldUploadAndReturnSanitizedKey() throws Exception {
        byte[] payload = "ID3-audio-content".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String key = minioStorageService.uploadSound(
                "Ara Rouge #1", "Recompense!", ".MP3", "audio/mpeg", inputStream, payload.length);

        assertNotNull(key);
        assertTrue(key.matches("^Recompense/Ara_Rouge_1_\\d+\\.mp3$"), "Unexpected key format: " + key);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue().bucket());
        assertEquals(key, captor.getValue().object());
        assertEquals("audio/mpeg", captor.getValue().contentType());
    }

    @Test
    void uploadSound_WhenBucketMissingAndSpecialCharsOnly_ShouldCreateBucketAndUseFallbacks() throws Exception {
        byte[] payload = new byte[]{0x52, 0x49, 0x46, 0x46};
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        String key = minioStorageService.uploadSound(
                "!!!", "@@@", "wav", null, inputStream, payload.length);

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        assertTrue(key.matches("^general/sound_\\d+\\.wav$"), "Unexpected fallback key: " + key);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertEquals("application/octet-stream", captor.getValue().contentType());
    }

    @Test
    void uploadSound_WithBlankContentType_ShouldDefaultToOctetStream() throws Exception {
        byte[] payload = new byte[]{1, 2, 3, 4};
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioStorageService.uploadSound("Saimiri", "alerte", "ogg", "   ", inputStream, payload.length);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertEquals("application/octet-stream", captor.getValue().contentType());
    }

    @Test
    void downloadSound_WithInvalidKey_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> minioStorageService.downloadSound(null));
        assertThrows(IllegalArgumentException.class, () -> minioStorageService.downloadSound("   "));
    }

    @Test
    void downloadSound_WithValidKey_ShouldReturnBytes() throws Exception {
        byte[] expectedBytes = "binary-wav-data".getBytes(StandardCharsets.UTF_8);
        String objectKey = "recompense/ara_123.wav";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        GetObjectResponse response = new GetObjectResponse(
                Headers.of(), BUCKET_NAME, "", objectKey, new ByteArrayInputStream(expectedBytes));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        byte[] result = minioStorageService.downloadSound(objectKey);

        assertArrayEquals(expectedBytes, result);
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void downloadSound_WhenMinioThrowsException_ShouldPropagateException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("S3 read error"));

        assertThrows(RuntimeException.class, () -> minioStorageService.downloadSound("recompense/missing.mp3"));
    }

    @Test
    void deleteSound_WithInvalidKey_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> minioStorageService.deleteSound(null));
        assertThrows(IllegalArgumentException.class, () -> minioStorageService.deleteSound(""));
    }

    @Test
    void deleteSound_WithValidKey_ShouldRemoveObjectFromBucket() throws Exception {
        String objectKey = "ambiance/rain_456.ogg";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioStorageService.deleteSound(objectKey);

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue().bucket());
        assertEquals(objectKey, captor.getValue().object());
    }

    @Test
    void deleteSound_WhenMinioThrowsException_ShouldPropagateException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doThrow(new RuntimeException("S3 delete error"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(RuntimeException.class, () -> minioStorageService.deleteSound("ambiance/rain_456.ogg"));
    }
}