package io.mango.file.api;

import io.mango.common.exception.BizException;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local or remote provider for generated file content. */
public interface IFileContentProvider {

    FileRecordVO save(SaveFileCommand command);

    FileDownloadVO download(Long id);

    FileDownloadVO downloadForService(Long id);

    default Path downloadTo(Long id, Path directory) {
        return writeToDirectory(downloadForService(id), directory);
    }

    default Map<Long, Path> downloadTo(List<Long> ids, Path directory) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(FileCode.FILE_NOT_FOUND.getCode(), "文件ID不能为空");
        }
        Map<Long, Path> result = new LinkedHashMap<>();
        for (Long id : ids) {
            result.put(id, downloadTo(id, directory));
        }
        return result;
    }

    private Path writeToDirectory(FileDownloadVO download, Path directory) {
        if (download == null || download.inputStream() == null) {
            throw new BizException(FileCode.FILE_READ_FAILED.getCode(), FileCode.FILE_READ_FAILED.getMessage());
        }
        if (directory == null) {
            throw new BizException(FileCode.STORAGE_PATH_INVALID.getCode(), "下载目录不能为空");
        }
        String fileName = safeFileName(download.fileName());
        try {
            Path targetDirectory = directory.toAbsolutePath().normalize();
            Files.createDirectories(targetDirectory);
            Path target = uniqueTarget(targetDirectory.resolve(fileName).normalize());
            if (!target.startsWith(targetDirectory)) {
                throw new BizException(FileCode.STORAGE_PATH_INVALID.getCode(), FileCode.STORAGE_PATH_INVALID.getMessage());
            }
            try (InputStream inputStream = download.inputStream()) {
                Files.copy(inputStream, target);
            }
            return target;
        } catch (IOException ex) {
            throw new BizException(FileCode.FILE_READ_FAILED.getCode(), FileCode.FILE_READ_FAILED.getMessage(), ex);
        }
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "download";
        }
        String normalized = fileName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return name.isEmpty() ? "download" : name;
    }

    private Path uniqueTarget(Path target) throws IOException {
        Path directory = target.getParent();
        String fileName = target.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }
        Path candidate = target;
        int index = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(baseName + "(" + index + ")" + extension);
            index++;
        }
        return candidate;
    }
}
