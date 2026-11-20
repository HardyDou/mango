import type { ApiId } from '@mango/api-schema';
import type { FilePreview, FileReference } from '../api/file';

export type { FilePreviewRenderMode } from '../api/file';

/**
 * 预览面板契约：给 `fileId` 即可，渲染类型由后端 preview-link 的 `renderMode` 决定。
 */
export interface FilePreviewPanelProps {
  /** 文件 ID，主入口。 */
  fileId?: ApiId | `mango-file:${string}` | null;
  /** 未入库的纯文本内容，与 fileId 互斥。 */
  textContent?: string;
  /** 可选元信息，仅用于展示文件名、大小，不参与渲染分派。 */
  file?: FileReference;
  /** 面板标题，缺省取文件名。 */
  title?: string;
  showActions?: boolean;
  fitContainer?: boolean;
  /**
   * 兼容旧调用方的元信息入参，仅用于展示文件名、大小，不参与渲染分派。
   *
   * @deprecated 下一个 major 移除，改用 `file`。
   */
  preview?: FilePreview | null;
  /**
   * 旧预览地址入参，仅在未传 `fileId` 时作为兜底展示。
   *
   * @deprecated 下一个 major 移除，改为只传 `fileId`，渲染类型由后端 `renderMode` 决定。
   */
  previewUrl?: string;
  /**
   * 旧文档预览地址入参，仅在未传 `fileId` 时作为兜底展示。
   *
   * @deprecated 下一个 major 移除，改为只传 `fileId`。
   */
  documentPreviewUrl?: string;
  /**
   * 旧直连预览地址入参，仅在未传 `fileId` 时作为兜底展示。
   *
   * @deprecated 下一个 major 移除，改为只传 `fileId`。
   */
  directPreviewUrl?: string;
  /**
   * 旧外部文档预览服务地址，已不再使用。
   *
   * @deprecated 下一个 major 移除，预览链路由后端 preview-link 提供。
   */
  previewProviderUrl?: string;
  /**
   * 旧外部文档预览扩展名白名单，已不再使用。
   *
   * @deprecated 下一个 major 移除，格式判定在后端完成。
   */
  previewExternalExtensions?: string[];
}
