package io.mango.file.preview.core.service;

import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.service.model.FilePreviewSource;

/**
 * 文件预览服务。
 */
public interface IFilePreviewService {

    /**
     * 按文件 ID 创建预览入口。
     *
     * @param fileId 文件 ID。
     * @return 预览入口信息。
     */
    FilePreviewLinkVO createPreview(Long fileId);

    /**
     * 按文件 ID 创建预览引擎入口。
     *
     * @param fileId 文件 ID。
     * @return 预览引擎入口信息。
     */
    FilePreviewLinkVO createEnginePreview(Long fileId);

    /**
     * 打开预览源文件。
     *
     * @param token 源文件访问令牌。
     * @return 源文件。
     */
    FilePreviewSource openSource(String token);
}
