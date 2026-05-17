package io.mango.file.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务。
 */
public interface IFileService {

    R<FileRecordVO> upload(MultipartFile file, String purpose, String accessLevel, String bizType, String bizId, String bizMeta, Long directoryId);

    R<List<FileRecordVO>> uploadBatch(MultipartFile[] files, String purpose, String accessLevel, String bizType, String bizId, String bizMeta, Long directoryId);

    R<FileRecordVO> save(SaveFileCommand command);

    R<PageResult<FileRecordVO>> page(FileRecordPageQuery query);

    R<FileRecordVO> get(Long id);

    R<FilePreviewVO> preview(Long id);

    FileDownload download(Long id);

    R<Boolean> archive(FileArchiveCommand command);
}
