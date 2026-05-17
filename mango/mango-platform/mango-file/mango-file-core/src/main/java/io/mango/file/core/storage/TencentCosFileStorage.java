package io.mango.file.core.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.region.Region;
import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
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
 * 腾讯云 COS 文件存储实现。
 */
public class TencentCosFileStorage extends AbstractCloudFileStorage {

    @Override
    public boolean supports(String storageType) {
        return "TENCENT_COS".equalsIgnoreCase(storageType);
    }

    @Override
    public void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) {
        requireConfig(config);
        COSClient client = client(config);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            if (StringUtils.hasText(contentType)) {
                metadata.setContentType(contentType);
            }
            client.putObject(new PutObjectRequest(config.getBucketName(), objectName, inputStream, metadata));
        } catch (Exception e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public FileObject getObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        COSClient client = client(config);
        try {
            COSObject object = client.getObject(config.getBucketName(), objectName);
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
        COSClient client = client(config);
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
        COSClient client = client(config);
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
        COSClient client = client(config);
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    config.getBucketName(),
                    objectName,
                    HttpMethodName.GET);
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
        requireBucket(config);
        requireAccessSecret(config);
        Require.notBlank(config.getRegion(), FileCode.STORAGE_CONFIG_INVALID);
    }

    private COSClient client(FileStorageConfig config) {
        COSCredentials credentials = new BasicCOSCredentials(config.getAccessKey(), config.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));
        clientConfig.setHttpProtocol(enabled(config.getSslEnabled()) ? HttpProtocol.https : HttpProtocol.http);
        return new COSClient(credentials, clientConfig);
    }

    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
