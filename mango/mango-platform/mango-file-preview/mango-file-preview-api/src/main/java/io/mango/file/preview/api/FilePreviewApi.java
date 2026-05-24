package io.mango.file.preview.api;

import io.mango.common.result.R;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;

/**
 * 文件预览本地接口契约。
 */
public interface FilePreviewApi {

    /**
     * 按文件 ID 创建预览入口。
     *
     * @param fileId 文件 ID。
     * @return 预览入口信息。
     */
    R<FilePreviewLinkVO> preview(Long fileId);
}
