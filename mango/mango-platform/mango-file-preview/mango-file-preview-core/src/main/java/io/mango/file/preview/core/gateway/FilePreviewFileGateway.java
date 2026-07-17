package io.mango.file.preview.core.gateway;

import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 文件预览对文件中心传输契约的适配器。
 */
@Component
@RequiredArgsConstructor
public class FilePreviewFileGateway {

    private final FileApi fileApi;
    private final IFileContentProvider fileContentProvider;

    /**
     * 查询当前上下文可见的文件。
     *
     * @param fileId 文件 ID。
     * @return 文件记录；文件中心拒绝或未找到时返回 {@code null}。
     */
    public FileRecordVO find(Long fileId) {
        R<FileRecordVO> result = fileApi.get(fileId);
        if (result == null || !result.isSuccess()) {
            return null;
        }
        return result.getData();
    }

    /**
     * 以服务身份读取源文件。
     *
     * @param fileId 文件 ID。
     * @return 文件下载信息。
     */
    public FileDownloadVO download(Long fileId) {
        return fileContentProvider.downloadForService(fileId);
    }
}
