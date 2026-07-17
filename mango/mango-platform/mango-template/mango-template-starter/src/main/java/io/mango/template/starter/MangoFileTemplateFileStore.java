package io.mango.template.starter;

import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.template.api.enums.TemplateCode;
import io.mango.template.core.service.ITemplateFileStore;
import io.mango.template.core.service.TemplateStoredFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * 基于 mango-file 本地能力的模板文件适配器。
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MangoFileTemplateFileStore implements ITemplateFileStore {

    private final IFileContentProvider fileContentProvider;

    @Override
    public Long save(byte[] content, String fileName, String contentType, String purpose, String bizType, String bizId) {
        SaveFileCommand command = new SaveFileCommand();
        if (content != null) {
            command.setInputStream(new ByteArrayInputStream(content));
            command.setFileSize((long) content.length);
        }
        command.setFileName(fileName);
        command.setContentType(contentType);
        command.setPurpose(purpose);
        command.setBizType(bizType);
        command.setBizId(bizId);
        command.setDirectoryId(0L);
        FileRecordVO result = fileContentProvider.save(command);
        if (result == null) {
            throw new io.mango.common.exception.BizException(
                    TemplateCode.TEMPLATE_RENDER_FAILED.getCode(),
                    TemplateCode.TEMPLATE_RENDER_FAILED.getMessage());
        }
        return result.getId();
    }

    @Override
    public TemplateStoredFile read(Long fileId) {
        FileDownloadVO download = fileContentProvider.download(fileId);
        return new TemplateStoredFile(download.inputStream(), download.fileName(), download.contentType(), download.contentLength());
    }
}
