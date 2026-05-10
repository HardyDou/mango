package io.mango.file.core.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import org.springframework.util.StringUtils;

import java.io.InputStream;

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
            return new FileObject(object.getObjectContent(), metadata.getContentLength(), metadata.getContentType());
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
        try {
            client.doesBucketExist(config.getBucketName());
        } finally {
            client.shutdown();
        }
    }

    private void requireConfig(FileStorageConfig config) {
        requireBucket(config);
        requireAccessSecret(config);
        Require.notBlank(config.getRegion(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "腾讯云 COS 区域不能为空");
    }

    private COSClient client(FileStorageConfig config) {
        COSCredentials credentials = new BasicCOSCredentials(config.getAccessKey(), config.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));
        clientConfig.setHttpProtocol(enabled(config.getSslEnabled()) ? HttpProtocol.https : HttpProtocol.http);
        return new COSClient(credentials, clientConfig);
    }
}
