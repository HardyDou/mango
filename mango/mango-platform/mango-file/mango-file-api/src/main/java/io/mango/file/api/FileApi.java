package io.mango.file.api;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件能力 API 契约。
 */
public interface FileApi {

    /** 分页查询文件记录。 */
    R<PageResult<FileRecordVO>> page(FileRecordPageQuery query);

    /** 查询文件详情。 */
    R<FileRecordVO> get(Long id);

    /** 查询文件预览元数据。 */
    R<FilePreviewVO> preview(Long id);

    /** 通过对外下载语义读取文件内容。 */
    FileDownloadVO download(Long id);

    /** 通过服务内语义读取文件内容。 */
    default FileDownloadVO downloadForService(Long id) {
        return download(id);
    }

    /** 保存内部生成的文件内容。 */
    default R<FileRecordVO> save(SaveFileCommand command) {
        throw new UnsupportedOperationException("当前文件 API 实现不支持保存文件");
    }

    /** 按目录结构清单打包多个文件为 ZIP，并保存为新的文件记录。 */
    default R<FileRecordVO> packageFiles(FilePackageCommand command) {
        throw new UnsupportedOperationException("当前文件 API 实现不支持文件打包");
    }

    /**
     * 下载文件到指定目录。
     *
     * @param id 文件 ID。
     * @param directory 目标目录。
     * @return 实际落盘路径。
     */
    default Path downloadTo(Long id, Path directory) {
        FileDownloadVO download = downloadForService(id);
        return writeToDirectory(download, directory);
    }

    /**
     * 批量下载文件到指定目录。
     *
     * @param ids 文件 ID 列表。
     * @param directory 目标目录。
     * @return 文件 ID 与实际落盘路径映射。
     */
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

    /** 归档文件记录。 */
    R<Boolean> archive(FileArchiveCommand command);

    /** 删除文件记录。 */
    default R<Boolean> delete(FileDeleteCommand command) {
        throw new UnsupportedOperationException("当前文件 API 实现不支持删除文件");
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
