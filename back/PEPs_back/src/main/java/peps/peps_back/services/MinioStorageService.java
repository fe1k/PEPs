package peps.peps_back.services;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Service layer responsible for audio file persistence in MinIO S3-compatible object storage.
 *
 * Encapsulates bucket policies, input validation, and deterministic key sanitization.
 *
 * @author Haytam BEN SRIBIT and Franco Valdebenito
 * @version 2.0
 */
@Service
public class MinioStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioStorageService.class);
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

    private final MinioClient minioClient;
    private final String bucketName;

    /**
     * Builds a MinIO storage service with connection settings read from environment-backed
     * Spring configuration properties.
     *
     * @param endpoint   MinIO server endpoint URL
     * @param accessKey  MinIO access key
     * @param secretKey  MinIO secret key
     * @param bucketName target bucket used for sound objects
     */
    @Autowired
    public MinioStorageService(
            @Value("${MINIO_ENDPOINT:http://localhost:9000}") String endpoint,
            @Value("${MINIO_ACCESS_KEY:minioadmin}") String accessKey,
            @Value("${MINIO_SECRET_KEY:minioadmin}") String secretKey,
            @Value("${MINIO_BUCKET:peps-sounds}") String bucketName) {
        this(
                MinioClient.builder()
                        .endpoint(endpoint.trim())
                        .credentials(accessKey.trim(), secretKey.trim())
                        .build(),
                bucketName
        );
    }

    /**
     * Package-private constructor used for dependency injection in unit tests.
     *
     * @param minioClient configured MinIO SDK client
     * @param bucketName  target bucket name
     */
    MinioStorageService(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName.trim();
    }

    /**
     * Ensures the target bucket exists and enforces a private bucket policy at startup.
     */
    @PostConstruct
    public void initializeBucket() {
        try {
            ensureBucketExists();
            String privatePolicy = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(privatePolicy)
                    .build());
        } catch (Exception e) {
            LOGGER.warn("MinIO initialization skipped: {}", e.getMessage());
        }
    }

    /**
     * Uploads a sound stream to MinIO and returns the generated object key.
     *
     * @param name        logical sound name used in object key generation
     * @param type        sound category/type used as object prefix
     * @param extension   file extension (with or without leading dot)
     * @param contentType MIME content type
     * @param inputStream stream carrying binary audio data
     * @param size        stream length in bytes (must be positive)
     * @return generated MinIO object key
     * @throws Exception when bucket provisioning or object upload fails
     */
    public String uploadSound(
            String name,
            String type,
            String extension,
            String contentType,
            InputStream inputStream,
            long size) throws Exception {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Sound name must not be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Sound type must not be null or blank");
        }
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("File extension must not be null or blank");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("Audio input stream must not be null");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Audio stream size must be positive");
        }

        ensureBucketExists();
        String objectKey = buildObjectKey(name, type, extension);
        String resolvedContentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType.trim();

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(inputStream, size, -1)
                .contentType(resolvedContentType)
                .build());

        return objectKey;
    }

    /**
     * Downloads a sound object from MinIO as a byte array.
     *
     * @param objectKey object key in MinIO
     * @return raw binary content of the audio object
     * @throws Exception when object retrieval fails
     */
    public byte[] downloadSound(String objectKey) throws Exception {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("MinIO object key must not be null or blank");
        }

        ensureBucketExists();
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectKey).build());
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }

    /**
     * Deletes a sound object from MinIO.
     *
     * @param objectKey object key in MinIO
     * @throws Exception when object deletion fails
     */
    public void deleteSound(String objectKey) throws Exception {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("MinIO object key must not be null or blank");
        }

        ensureBucketExists();
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build());
    }

    /**
     * Ensures the configured MinIO bucket exists, creating it if absent.
     */
    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * Builds a sanitized, collision-resistant object key organized by sound type.
     */
    private String buildObjectKey(String name, String type, String extension) {
        String sanitizedType = sanitizeSegment(type, "general");
        String sanitizedName = sanitizeSegment(name, "sound");
        String normalizedExt = extension.trim().replaceFirst("^\\.+", "").toLowerCase(Locale.ROOT);
        return sanitizedType + "/" + sanitizedName + "_" + System.currentTimeMillis() + "." + normalizedExt;
    }

    /**
     * Normalizes an identifier segment for safe S3 object paths.
     */
    private String sanitizeSegment(String value, String fallback) {
        String sanitized = UNSAFE_CHARS.matcher(value.trim()).replaceAll("_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return sanitized.isEmpty() ? fallback : sanitized;
    }
}