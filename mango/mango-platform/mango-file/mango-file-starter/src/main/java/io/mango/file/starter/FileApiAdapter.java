package io.mango.file.starter;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.SaveGeneratedFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 文件 API 本地适配器。
 */
@Component
@RequiredArgsConstructor
public class FileApiAdapter implements FileApi {

    private final IFileService fileService;

    @Override
    public R<PageResult<FileRecordVO>> page(FileRecordPageQuery query) {
        return fileService.page(query);
    }

    @Override
    public R<FileRecordVO> get(Long id) {
        return fileService.get(id);
    }

    @Override
    public R<FilePreviewVO> preview(Long id) {
        return fileService.preview(id);
    }

    @Override
    public R<FileRecordVO> saveGenerated(SaveGeneratedFileCommand command) {
        return fileService.saveGenerated(command);
    }

    @Override
    public FileDownloadVO download(Long id) {
        return fileService.download(id);
    }

    @Override
    public R<Boolean> archive(FileArchiveCommand command) {
        return fileService.archive(command);
    }
}
