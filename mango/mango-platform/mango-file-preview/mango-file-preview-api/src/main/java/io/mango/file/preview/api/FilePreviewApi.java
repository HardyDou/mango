package io.mango.file.preview.api;

import io.mango.common.result.R;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import jakarta.validation.constraints.NotNull;

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
    R<FilePreviewLinkVO> preview(
            @NotNull(message = "文件ID不能为空") Long fileId,
            @NotNull(message = "大文件预览确认标识不能为空") Boolean allowLargeFile);

    /**
     * 查询预览产物生成状态。
     *
     * @param fileId 文件 ID
     * @return 异步任务状态
     */
    R<FilePreviewTaskVO> status(
            @NotNull(message = "文件ID不能为空") Long fileId,
            @NotNull(message = "大文件预览确认标识不能为空") Boolean allowLargeFile);
}
