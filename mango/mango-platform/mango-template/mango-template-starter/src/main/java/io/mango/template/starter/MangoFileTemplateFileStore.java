package io.mango.template.starter;

import io.mango.common.result.R;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.FileDownload;
import io.mango.file.core.service.IFileService;
import io.mango.template.api.TemplateCode;
import io.mango.template.core.service.ITemplateFileStore;
import io.mango.template.core.service.TemplateStoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 基于 mango-file 本地能力的模板文件适配器。
 */
@Component
@RequiredArgsConstructor
public class MangoFileTemplateFileStore implements ITemplateFileStore {

    private final IFileService fileService;

    @Override
    public Long save(byte[] content, String fileName, String contentType, String purpose, String bizType, String bizId) {
        R<FileRecordVO> result = fileService.saveGenerated(content, fileName, contentType, purpose, bizType, bizId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new io.mango.common.exception.BizException(
                    TemplateCode.TEMPLATE_RENDER_FAILED.getCode(),
                    result.getMsg());
        }
        return result.getData().getId();
    }

    @Override
    public TemplateStoredFile read(Long fileId) {
        FileDownload download = fileService.download(fileId);
        return new TemplateStoredFile(download.inputStream(), download.fileName(), download.contentType(), download.contentLength());
    }
}
