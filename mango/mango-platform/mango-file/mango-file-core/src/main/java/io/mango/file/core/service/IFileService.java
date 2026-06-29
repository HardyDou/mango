package io.mango.file.core.service;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.CompleteFileUploadPartCommand;
import io.mango.file.api.command.CreateFileUploadPartSignCommand;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileUploadInitVO;
import io.mango.file.api.vo.FileUploadPartSignVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务。
 */
public interface IFileService {

    R<FileRecordVO> upload(MultipartFile file, String purpose, String accessLevel, String bizType, String bizId, String bizMeta, Long directoryId);

    R<List<FileRecordVO>> uploadBatch(MultipartFile[] files, String purpose, String accessLevel, String bizType, String bizId, String bizMeta, Long directoryId);

    R<FileRecordVO> save(SaveFileCommand command);

    R<FileRecordVO> packageFiles(FilePackageCommand command);

    R<FileRecordVO> saveGenerated(byte[] content,
                                  String fileName,
                                  String contentType,
                                  String purpose,
                                  String bizType,
                                  String bizId);

    R<PageResult<FileRecordVO>> page(FileRecordPageQuery query);

    R<FileRecordVO> get(Long id);

    R<FilePreviewVO> preview(Long id);

    FileDownloadVO download(Long id);

    FileDownloadVO downloadForService(Long id);

    R<Boolean> archive(FileArchiveCommand command);

    R<Boolean> delete(FileDeleteCommand command);

    R<FileUploadInitVO> createUploadSession(CreateFileUploadSessionCommand command);

    R<FileUploadPartSignVO> createUploadPartSign(Long sessionId, CreateFileUploadPartSignCommand command);

    R<Boolean> uploadServerPart(Long sessionId, Integer partNumber, MultipartFile file);

    R<Boolean> completeUploadPart(Long sessionId, CompleteFileUploadPartCommand command);

    R<FileRecordVO> completeUploadSession(Long sessionId);

    R<Boolean> abortUploadSession(Long sessionId);
}
