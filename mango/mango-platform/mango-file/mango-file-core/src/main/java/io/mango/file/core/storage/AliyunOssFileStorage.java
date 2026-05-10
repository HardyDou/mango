package io.mango.file.core.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import io.mango.common.result.Require;
import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import org.springframework.util.StringUtils;

import java.io.InputStream;

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
            return new FileObject(object.getObjectContent(), metadata.getContentLength(), metadata.getContentType());
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
        try {
            client.doesBucketExist(config.getBucketName());
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
}
