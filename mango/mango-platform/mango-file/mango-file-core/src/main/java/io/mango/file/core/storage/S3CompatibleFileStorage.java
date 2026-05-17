package io.mango.file.core.storage;

import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * S3 兼容对象存储实现，覆盖 S3、MinIO、AWS S3。
 */
public class S3CompatibleFileStorage extends AbstractCloudFileStorage {

    private static final Set<String> SUPPORTED = Set.of("S3", "MINIO", "AWS_S3");

    @Override
    public boolean supports(String storageType) {
        return storageType != null && SUPPORTED.contains(storageType.toUpperCase());
    }

    @Override
    public void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) {
        requireConfig(config);
        try (S3Client client = client(config)) {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(objectName);
            if (StringUtils.hasText(contentType)) {
                builder.contentType(contentType);
            }
            client.putObject(builder.build(), RequestBody.fromInputStream(inputStream, contentLength));
        } catch (Exception e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        }
    }

    @Override
    public FileObject getObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        S3Client client = null;
        try {
            client = client(config);
            ResponseInputStream<GetObjectResponse> input = client.getObject(GetObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(objectName)
                    .build());
            GetObjectResponse response = input.response();
            return FileObject.of(input, response.contentLength(), response.contentType(), client::close);
        } catch (Exception e) {
            if (client != null) {
                client.close();
            }
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void removeObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        try (S3Client client = client(config)) {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(objectName)
                    .build());
        } catch (Exception e) {
            Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void test(FileStorageConfig config) {
        requireConfig(config);
        try (S3Client client = client(config)) {
            client.headBucket(HeadBucketRequest.builder().bucket(config.getBucketName()).build());
        }
    }

    @Override
    public Optional<String> presignedGetUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return presignedUrl(config, objectName, fileName, expires, false);
    }

    @Override
    public Optional<String> presignedDownloadUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return presignedUrl(config, objectName, fileName, expires, true);
    }

    @Override
    public Optional<String> publicGetUrl(FileStorageConfig config, String objectName, String fileName) {
        return publicObjectUrl(config, objectName);
    }

    private Optional<String> presignedUrl(FileStorageConfig config,
                                          String objectName,
                                          String fileName,
                                          Duration expires,
                                          boolean attachment) {
        requireConfig(config);
        Require.notBlank(objectName, FileCode.FILE_NOT_FOUND);
        Require.notNull(expires, FileCode.STORAGE_CONFIG_INVALID);
        Require.isTrue(!expires.isNegative() && !expires.isZero(), FileCode.STORAGE_CONFIG_INVALID);
        try (S3Presigner presigner = presigner(config)) {
            GetObjectRequest.Builder getObjectBuilder = GetObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(objectName);
            if (attachment && StringUtils.hasText(fileName)) {
                getObjectBuilder.responseContentDisposition(contentDisposition(fileName));
            }
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expires)
                    .getObjectRequest(getObjectBuilder.build())
                    .build();
            PresignedGetObjectRequest request = presigner.presignGetObject(presignRequest);
            return Optional.of(request.url().toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void requireConfig(FileStorageConfig config) {
        requireBucket(config);
        requireAccessSecret(config);
        if (!"AWS_S3".equalsIgnoreCase(config.getStorageType())) {
            requireEndpoint(config);
        }
    }

    private S3Client client(FileStorageConfig config) {
        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(enabled(config.getPathStyleAccess()))
                .build();
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(regionOrDefault(config, "us-east-1")))
                .serviceConfiguration(serviceConfig)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())));
        if (StringUtils.hasText(config.getEndpoint())) {
            builder.endpointOverride(URI.create(config.getEndpoint()));
        }
        return builder.build();
    }

    private S3Presigner presigner(FileStorageConfig config) {
        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(enabled(config.getPathStyleAccess()))
                .build();
        Builder builder = S3Presigner.builder()
                .region(Region.of(regionOrDefault(config, "us-east-1")))
                .serviceConfiguration(serviceConfig)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())));
        String endpoint = StringUtils.hasText(config.getPublicEndpoint()) ? config.getPublicEndpoint() : config.getEndpoint();
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }
        return builder.build();
    }

    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
