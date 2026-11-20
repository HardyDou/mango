package io.mango.file.preview.api;

import io.mango.common.result.R;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import jakarta.validation.constraints.NotBlank;
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

    /**
     * 校验预览源文件票据是否可用。
     *
     * <p>供独立部署的预览引擎按票据远程直取源文件前做有效性校验：只判断票据是否存在且未过期，
     * 不读取文件流。票据本身就是凭据，因此不要求登录。</p>
     *
     * @param token 源文件访问令牌
     * @return 票据有效时为 {@code true}，不存在或已过期时为 {@code false}
     */
    R<Boolean> sourceStatus(@NotBlank(message = "源文件访问令牌不能为空") String token);
}
