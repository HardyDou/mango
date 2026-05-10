package io.mango.file.core.storage;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 七牛云 Kodo 文件存储实现。
 */
public class QiniuKodoFileStorage extends AbstractCloudFileStorage {

    @Override
    public boolean supports(String storageType) {
        return "QINIU_KODO".equalsIgnoreCase(storageType);
    }

    @Override
    public void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) {
        requireConfig(config);
        try {
            UploadManager uploadManager = new UploadManager(configuration(config));
            String token = auth(config).uploadToken(config.getBucketName());
            uploadManager.put(inputStream, objectName, token, null, contentType);
        } catch (Exception e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        }
    }

    @Override
    public FileObject getObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        Require.notBlank(config.getPublicEndpoint(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "七牛云下载需要配置公开访问地址");
        try {
            FileInfo info = bucketManager(config).stat(config.getBucketName(), objectName);
            String url = normalizePublicEndpoint(config.getPublicEndpoint()) + "/" + objectName;
            String signedUrl = auth(config).privateDownloadUrl(url);
            URLConnection connection = new URL(signedUrl).openConnection();
            return new FileObject(connection.getInputStream(), info.fsize, info.mimeType);
        } catch (Exception e) {
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void removeObject(FileStorageConfig config, String objectName) {
        requireConfig(config);
        try {
            bucketManager(config).delete(config.getBucketName(), objectName);
        } catch (QiniuException e) {
            Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void test(FileStorageConfig config) throws Exception {
        requireConfig(config);
        String objectName = ".mango-storage-test";
        UploadManager uploadManager = new UploadManager(configuration(config));
        String token = auth(config).uploadToken(config.getBucketName());
        Response response = uploadManager.put(new ByteArrayInputStream(new byte[]{1}), objectName, token, null, "application/octet-stream");
        Require.isTrue(response.isOK(), FileCode.STORAGE_CONFIG_TEST_FAILED);
        bucketManager(config).delete(config.getBucketName(), objectName);
    }

    private void requireConfig(FileStorageConfig config) {
        requireBucket(config);
        requireAccessSecret(config);
    }

    private Auth auth(FileStorageConfig config) {
        return Auth.create(config.getAccessKey(), config.getSecretKey());
    }

    private BucketManager bucketManager(FileStorageConfig config) {
        return new BucketManager(auth(config), configuration(config));
    }

    private Configuration configuration(FileStorageConfig config) {
        String region = regionOrDefault(config, "auto");
        if ("huadong".equalsIgnoreCase(region) || "z0".equalsIgnoreCase(region)) {
            return new Configuration(Region.huadong());
        }
        if ("huabei".equalsIgnoreCase(region) || "z1".equalsIgnoreCase(region)) {
            return new Configuration(Region.huabei());
        }
        if ("huanan".equalsIgnoreCase(region) || "z2".equalsIgnoreCase(region)) {
            return new Configuration(Region.huanan());
        }
        if ("beimei".equalsIgnoreCase(region) || "na0".equalsIgnoreCase(region)) {
            return new Configuration(Region.beimei());
        }
        if ("xinjiapo".equalsIgnoreCase(region) || "as0".equalsIgnoreCase(region)) {
            return new Configuration(Region.xinjiapo());
        }
        return new Configuration(Region.autoRegion());
    }

    private String normalizePublicEndpoint(String endpoint) {
        String value = StringUtils.trimTrailingCharacter(endpoint.trim(), '/');
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }
}
