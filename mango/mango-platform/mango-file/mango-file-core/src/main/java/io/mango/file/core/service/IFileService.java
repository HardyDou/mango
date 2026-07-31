package io.mango.file.core.service;

import io.mango.common.vo.PageResult;
import io.mango.file.api.command.CompleteFileUploadPartCommand;
import io.mango.file.api.command.CreateFileUploadPartSignCommand;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.core.service.model.FileDownloadOptions;
import io.mango.file.core.service.model.ServerFilePart;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePackageResultVO;
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

    FileRecordVO upload(MultipartFile file, SaveFileCommand command);

    List<FileRecordVO> uploadBatch(MultipartFile[] files, SaveFileCommand command);

    FileRecordVO save(SaveFileCommand command);

    FileRecordVO packageFiles(FilePackageCommand command);

    FilePackageResultVO packageFilesWithSizeControl(FilePackageSizeControlCommand command);

    FileRecordVO mergeToPdf(FileMergePdfCommand command);

    FileRecordVO saveGenerated(byte[] content, SaveFileCommand command);

    PageResult<FileRecordVO> page(FileRecordPageQuery query);

    FileRecordVO get(Long id);

    FilePreviewVO preview(Long id);

    FileDownloadVO download(Long id);

    FileDownloadVO download(FileDownloadOptions options);

    FileDownloadVO downloadForService(Long id);

    Boolean archive(FileArchiveCommand command);

    Boolean delete(FileDeleteCommand command);

    FileUploadInitVO createUploadSession(CreateFileUploadSessionCommand command);

    FileUploadPartSignVO createUploadPartSign(Long sessionId, CreateFileUploadPartSignCommand command);

    Boolean uploadServerPart(ServerFilePart part);

    Boolean completeUploadPart(Long sessionId, CompleteFileUploadPartCommand command);

    FileRecordVO completeUploadSession(Long sessionId);

    Boolean abortUploadSession(Long sessionId);
}
