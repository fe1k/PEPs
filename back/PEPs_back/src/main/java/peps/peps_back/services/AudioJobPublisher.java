package peps.peps_back.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service layer responsible for publishing asynchronous audio processing and device
 * delivery jobs onto the Redis Streams.
 *
 * @author Santiago Alexander RODRIGUEZ TRIANA and Franco Valdebenito
 * @version 2.0
 */
@Service
public class AudioJobPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioJobPublisher.class);

    static final String DEFAULT_UPLOAD_STREAM = "upload-audio-stream";
    static final String DEFAULT_RETRIEVAL_STREAM = "retrieval-request-stream";

    private final RedisTemplate<String, Object> redisTemplate;
    private final String uploadStreamKey;
    private final String retrievalStreamKey;

    /**
     * Constructs an {@code AudioJobPublisher} bound to the application's string-serialized
     * {@link RedisTemplate} and configured stream keys.
     *
     * @param redisTemplate      Spring Redis template configured in {@code RedisConfig}
     * @param uploadStreamKey    Redis Stream key for upload jobs (defaults to {@code upload-audio-stream})
     * @param retrievalStreamKey Redis Stream key for device retrieval jobs (defaults to {@code retrieval-request-stream})
     */
    @Autowired
    public AudioJobPublisher(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${REDIS_UPLOAD_STREAM:upload-audio-stream}") String uploadStreamKey,
            @Value("${REDIS_RETRIEVAL_STREAM:retrieval-request-stream}") String retrievalStreamKey) {
        this.redisTemplate = redisTemplate;
        this.uploadStreamKey = (uploadStreamKey == null || uploadStreamKey.isBlank())
                ? DEFAULT_UPLOAD_STREAM
                : uploadStreamKey.trim();
        this.retrievalStreamKey = (retrievalStreamKey == null || retrievalStreamKey.isBlank())
                ? DEFAULT_RETRIEVAL_STREAM
                : retrievalStreamKey.trim();
    }

    /**
     * Publishes an asynchronous audio upload and validation job (Pipeline 1) onto
     * {@code upload-audio-stream}.
     *
     * @param jobId       UUID correlation identifier for tracking the upload job
     * @param soundId     positive primary key of the persisted {@code Sound} metadata row
     * @param soundName   human-readable sound name used for worker log traceability
     * @param audioBase64 Base-64 encoded raw audio payload
     * @param contentType MIME type of the audio file (defaults to {@code application/octet-stream} if blank)
     * @throws IllegalArgumentException if any required argument is null, blank, or non-positive
     * @throws IllegalStateException    if publishing to Redis fails
     */
    public void publishUploadJob(
            String jobId,
            Integer soundId,
            String soundName,
            String audioBase64,
            String contentType) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Job ID must not be null or blank");
        }
        if (soundId == null || soundId <= 0) {
            throw new IllegalArgumentException("Sound ID must be a positive integer");
        }
        if (soundName == null || soundName.isBlank()) {
            throw new IllegalArgumentException("Sound name must not be null or blank");
        }
        if (audioBase64 == null || audioBase64.isBlank()) {
            throw new IllegalArgumentException("Base-64 audio payload must not be null or blank");
        }

        String resolvedContentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType.trim();

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("jobId", jobId.trim());
        payload.put("soundId", String.valueOf(soundId));
        payload.put("soundName", soundName.trim());
        payload.put("audioBase64", audioBase64);
        payload.put("contentType", resolvedContentType);

        try {
            StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
            RecordId recordId = streamOps.add(uploadStreamKey, payload);
            LOGGER.info("Published upload job to '{}' (recordId={}, jobId={}, soundId={}, soundName='{}')",
                    uploadStreamKey, recordId, jobId, soundId, soundName);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to publish upload job to Redis stream '" + uploadStreamKey
                            + "' for soundId=" + soundId + ", jobId=" + jobId, e);
        }
    }

    /**
     * Publishes an asynchronous audio retrieval and ESP32 transcoding job (Pipeline 2)
     * onto {@code retrieval-request-stream}.
     *
     * @param jobId   UUID correlation identifier used to cache the result under {@code ready-audio:{jobId}}
     * @param soundId positive primary key of the {@code Sound} row to retrieve from MinIO
     * @throws IllegalArgumentException if {@code jobId} is null/blank or {@code soundId} is null/non-positive
     * @throws IllegalStateException    if publishing to Redis fails
     */
    public void publishRetrievalJob(String jobId, Integer soundId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Job ID must not be null or blank");
        }
        if (soundId == null || soundId <= 0) {
            throw new IllegalArgumentException("Sound ID must be a positive integer");
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("jobId", jobId.trim());
        payload.put("soundId", String.valueOf(soundId));

        try {
            StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
            RecordId recordId = streamOps.add(retrievalStreamKey, payload);
            LOGGER.info("Published retrieval job to '{}' (recordId={}, jobId={}, soundId={})",
                    retrievalStreamKey, recordId, jobId, soundId);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to publish retrieval job to Redis stream '" + retrievalStreamKey
                            + "' for soundId=" + soundId + ", jobId=" + jobId, e);
        }
    }
}