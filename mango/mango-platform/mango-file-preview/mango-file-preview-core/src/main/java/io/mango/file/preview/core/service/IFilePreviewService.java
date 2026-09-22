package io.mango.file.preview.core.service;

import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;

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
     * 按预览入口令牌创建预览引擎入口。
     *
     * @param token 预览入口令牌。
     * @return 预览引擎入口信息。
     */
    FilePreviewLinkVO createEnginePreviewByToken(String token);

    /**
     * Resolves a public preview-entry token to its protected file id.
     */
    Long resolvePreviewFileId(String token);

    /**
     * Reads task state with the tenant context carried by the public preview token.
     */
    default FilePreviewTaskVO previewTaskByToken(String token) {
        return previewTaskByToken(token, false);
    }

    FilePreviewTaskVO previewTaskByToken(String token, boolean allowLargeFile);

    /** 在预览令牌携带的租户上下文中读取已生成的预览产物。 */
    FileDownloadVO downloadPreviewArtifact(String token);

    /**
     * 在预览入口令牌携带的租户上下文中读取原始文件内容。
     * 用于无需转换的 PDF，避免经过旧的在线预览 URL。
     */
    FileDownloadVO downloadPreviewSource(String token);

    /**
     * 判断预览入口对应的原文件是否为 PDF。
     */
    boolean isPdfPreview(String token);

    /**
     * 打开预览源文件。
     *
     * @param token 源文件访问令牌。
     * @return 源文件。
     */
    FilePreviewSource openSource(String token);

    /**
     * 校验转换后 PDF 的临时访问权限。
     *
     * @param token 源文件访问令牌。
     * @param fileName 转换后 PDF 文件名。
     */
    void validateGeneratedAccess(String token, String fileName);
}
