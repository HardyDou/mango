package io.mango.file.core.storage;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import io.mango.common.result.Require;
import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * 阿里云 OSS 文件存储实现。
 */
public class AliyunOssFileStorage extends AbstractCloudFileStorage {

    @Override
    public boolean supports(String storageType) {
        return "ALIYUN_OSS".equalsIgnoreCase(storageType);
    }

    @Override
    public void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) {
        requireConfig(config);
        OSS client = client(config);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            if (StringUtils.hasText(contentType)) {
                metadata.setContentType(contentType);
            }
            client.putObject(config.getBucketName(), objectName, inputStream, metadata);
        } catch (Exception e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public FileObject getObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        OSS client = client(config);
        try {
            OSSObject object = client.getObject(config.getBucketName(), objectName);
            ObjectMetadata metadata = object.getObjectMetadata();
            return FileObject.of(object.getObjectContent(), metadata.getContentLength(), metadata.getContentType(), client::shutdown);
        } catch (Exception e) {
            client.shutdown();
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void removeObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        OSS client = client(config);
        try {
            client.deleteObject(config.getBucketName(), objectName);
        } catch (Exception e) {
            Require.fail(FileCode.FILE_READ_FAILED);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void test(FileStorageConfig config) {
        requireConfig(config);
        OSS client = client(config);
        String objectName = ".mango-storage-test";
        try {
            Require.isTrue(client.doesBucketExist(config.getBucketName()), FileCode.STORAGE_CONFIG_TEST_FAILED);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(1L);
            metadata.setContentType("application/octet-stream");
            client.putObject(config.getBucketName(), objectName, new ByteArrayInputStream(new byte[]{1}), metadata);
            client.deleteObject(config.getBucketName(), objectName);
        } finally {
            client.shutdown();
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
        OSS client = client(config);
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    config.getBucketName(),
                    objectName,
                    HttpMethod.GET);
            request.setExpiration(Date.from(Instant.now().plus(expires)));
            if (attachment && StringUtils.hasText(fileName)) {
                ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
                headers.setContentDisposition(contentDisposition(fileName));
                request.setResponseHeaders(headers);
            }
            return Optional.of(client.generatePresignedUrl(request).toString());
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            client.shutdown();
        }
    }

    private void requireConfig(FileStorageConfig config) {
        requireEndpoint(config);
        requireBucket(config);
        requireAccessSecret(config);
    }

    private OSS client(FileStorageConfig config) {
        return new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKey(), config.getSecretKey());
    }

    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
