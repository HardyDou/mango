package io.mango.template.starter;

import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.template.api.TemplateCode;
import io.mango.template.core.service.ITemplateFileStore;
import io.mango.template.core.service.TemplateStoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * 基于 mango-file 本地能力的模板文件适配器。
 */
@Component
@RequiredArgsConstructor
public class MangoFileTemplateFileStore implements ITemplateFileStore {

    private final FileApi fileApi;

    @Override
    public Long save(byte[] content, String fileName, String contentType, String purpose, String bizType, String bizId) {
        SaveFileCommand command = new SaveFileCommand();
        command.setInputStream(new ByteArrayInputStream(content));
        command.setFileName(fileName);
        command.setFileSize((long) content.length);
        command.setContentType(contentType);
        command.setPurpose(purpose);
        command.setBizType(bizType);
        command.setBizId(bizId);
        R<FileRecordVO> result = fileApi.save(command);
        if (!result.isSuccess() || result.getData() == null) {
            throw new io.mango.common.exception.BizException(
                    TemplateCode.TEMPLATE_RENDER_FAILED.getCode(),
                    result.getMsg());
        }
        return result.getData().getId();
    }

    @Override
    public TemplateStoredFile read(Long fileId) {
        FileDownloadVO download = fileApi.download(fileId);
        return new TemplateStoredFile(download.inputStream(), download.fileName(), download.contentType(), download.contentLength());
    }
}
