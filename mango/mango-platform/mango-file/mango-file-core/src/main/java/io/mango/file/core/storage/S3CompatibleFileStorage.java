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

import java.io.InputStream;
import java.net.URI;
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
        try {
            S3Client client = client(config);
            ResponseInputStream<GetObjectResponse> input = client.getObject(GetObjectRequest.builder()
                    .bucket(config.getBucketName())
                    .key(objectName)
                    .build());
            GetObjectResponse response = input.response();
            return new FileObject(input, response.contentLength(), response.contentType());
        } catch (Exception e) {
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
}
