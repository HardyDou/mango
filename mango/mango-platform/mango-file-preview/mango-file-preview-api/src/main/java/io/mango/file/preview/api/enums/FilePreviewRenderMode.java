package io.mango.file.preview.api.enums;

/**
 * 文件预览的渲染类型。
 *
 * <p>由服务端按文件扩展名与可预览性判定，前端据此直接选择渲染器，不要解析
 * {@code previewUrl} 或按扩展名自行猜测。新增取值属于契约变更。</p>
 */
public enum FilePreviewRenderMode {
    /** 服务端已有（或本身即）PDF，前端用 PDF 阅读器渲染预览地址。 */
    PDF,
    /** 图片文件，前端用图片查看器渲染原图内容。 */
    IMAGE,
    /** 视频文件，前端用视频播放器渲染。 */
    VIDEO,
    /** 音频文件，前端用音频播放器渲染。 */
    AUDIO,
    /** 需要异步转换（Office 文档等），转换完成前预览地址返回等待页，前端应展示转换等待态。 */
    CONVERTING,
    /** 格式不受支持，前端展示不支持提示并引导下载原文件。 */
    NOT_SUPPORTED,
    /** 平台可预览但当前没有专用渲染器，前端只提供下载入口。 */
    DOWNLOAD_ONLY
}
