package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.WebUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves files extracted from compressed archives under the preview cache directory.
 */
@Component
public class CompressFileEntryService {

    public Path resolveEntryPath(String compressFileKey, String compressFilePath)
            throws IOException {
        if (!StringUtils.hasText(compressFileKey) || !StringUtils.hasText(compressFilePath)) {
            throw new IllegalArgumentException("压缩包文件参数为空");
        }
        String normalizedKey = normalizeRelativePath(compressFileKey);
        String normalizedPath = normalizeRelativePath(compressFilePath);
        if (KkFileUtils.isIllegalFileName(normalizedKey) || KkFileUtils.isIllegalFileName(normalizedPath)) {
            throw new SecurityException("压缩包文件路径不合法");
        }
        if (!normalizedPath.startsWith(normalizedKey + "/")) {
            throw new SecurityException("压缩包文件路径不属于当前压缩包");
        }

        Path fileDir = Paths.get(ConfigConstants.getFileDir()).toAbsolutePath().normalize();
        Path entryRoot = fileDir.resolve(normalizedKey).normalize();
        Path entryPath = fileDir.resolve(normalizedPath).normalize();
        if (!entryRoot.startsWith(fileDir) || !entryPath.startsWith(entryRoot)) {
            throw new SecurityException("压缩包文件路径越界");
        }
        if (!Files.isRegularFile(entryPath)) {
            throw new FileNotFoundException("压缩包内文件不存在");
        }
        return entryPath;
    }

    public String contentType(String fullFileName, String compressFilePath) {
        String filename = StringUtils.hasText(fullFileName) ? fullFileName : compressFilePath;
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }
        return WebUtils.getContentTypeByFilename(filename);
    }

    private String normalizeRelativePath(String path) {
        return path.replace('\\', '/').replaceFirst("^/+", "");
    }
}
