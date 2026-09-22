package io.mango.file.preview.core.gateway;

import io.mango.file.api.FileApi;
import io.mango.file.api.FileSettingsApi;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.common.result.R;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件预览对文件中心传输契约的适配器。
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed collaborators are injected once")
public class FilePreviewFileGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewFileGateway.class);
    private static final long DEFAULT_PREVIEW_MAX_SIZE_BYTES = 200L * 1024L * 1024L;

    private final FileApi fileApi;
    private final IFileContentProvider fileContentProvider;
    private final FileSettingsApi fileSettingsApi;

    public FilePreviewFileGateway(FileApi fileApi, IFileContentProvider fileContentProvider) {
        this(fileApi, fileContentProvider, null);
    }

    @Autowired
    public FilePreviewFileGateway(FileApi fileApi, IFileContentProvider fileContentProvider,
                                  FileSettingsApi fileSettingsApi) {
        this.fileApi = fileApi;
        this.fileContentProvider = fileContentProvider;
        this.fileSettingsApi = fileSettingsApi;
    }

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

    /**
     * 读取当前租户的 Office 预览大小限制。
     * 文件服务不可用时使用安全的 200 MiB 默认值，避免转换服务放行超大文件。
     *
     * @return 预览大小限制，单位字节
     */
    public long previewMaxSize() {
        if (fileSettingsApi == null) {
            return DEFAULT_PREVIEW_MAX_SIZE_BYTES;
        }
        try {
            R<FileSettingsVO> result = fileSettingsApi.get();
            Long value = result == null || !result.isSuccess() || result.getData() == null
                    ? null : result.getData().getPreviewMaxSize();
            return value == null || value <= 0 ? DEFAULT_PREVIEW_MAX_SIZE_BYTES : value;
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to read file preview size settings; using the safe default", exception);
            return DEFAULT_PREVIEW_MAX_SIZE_BYTES;
        }
    }
}
