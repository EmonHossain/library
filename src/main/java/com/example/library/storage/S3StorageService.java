package com.example.library.storage;

import com.example.library.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String prefix;

    public S3StorageService(StorageProperties properties) {
        if (properties.getS3Bucket() == null || properties.getS3Bucket().isBlank()) {
            throw new IllegalStateException("app.storage.s3-bucket is required when app.storage.type=s3.");
        }
        this.bucket = properties.getS3Bucket();
        this.prefix = normalizePrefix(properties.getS3Prefix());
        this.s3Client = S3Client.builder()
            .region(Region.of(properties.getS3Region()))
            .build();
    }

    @Override
    public String storageType() {
        return "s3";
    }

    @Override
    public void store(String key, InputStream inputStream, long sizeBytes, String contentType) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(fullKey(key))
            .contentType(contentType)
            .contentLength(sizeBytes)
            .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, sizeBytes));
    }

    @Override
    public StoredObject load(String key, String contentType, String filename) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(fullKey(key))
            .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
        return new StoredObject(objectBytes.asByteArray(), contentType, filename);
    }

    private String fullKey(String key) {
        return prefix.isBlank() ? key : prefix + "/" + key;
    }

    private String normalizePrefix(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
