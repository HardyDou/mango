---
'@mango/file': minor
'@mango/admin': patch
'@mango/admin-pages': patch
'@mango/admin-shell': patch
'@mango/calendar': patch
'@mango/cms': patch
'@mango/detail': patch
'@mango/job': patch
'@mango/link': patch
'@mango/notice': patch
'@mango/numgen': patch
'@mango/payment': patch
'@mango/system': patch
'@mango/template': patch
'@mango/workflow': patch
'@mango/workflow-business-example': patch
---

预览组件契约收敛到 `fileId`，渲染类型由后端返回（#1363）。

`@mango/file`：

- `FilePreviewPanel` / `MangoFilePreviewDialog` 只需传 `fileId`，组件按 `/file-preview/files/preview-link` 新增的 `renderMode`
  （`PDF` / `IMAGE` / `VIDEO` / `AUDIO` / `CONVERTING` / `NOT_SUPPORTED` / `DOWNLOAD_ONLY`）单处分派渲染器，不再解析预览地址，
  也不再按扩展名自行判断格式；`utils/previewUrl.ts` 的三个地址反推函数已删除。
- 同一 `fileId` 重复打开复用 preview-link 结果（按 `expireSeconds` 提前 30 秒失效），不再重复请求、不重复提交转换任务。
- `preview`、`previewUrl`、`documentPreviewUrl`、`directPreviewUrl`、`previewProviderUrl`、`previewExternalExtensions`
  进入弃用路径：保留可用（仅在未传 `fileId` 时作为兜底展示），每次挂载输出一次 `console.warn`，**下一个 major 移除**。
- 新增可选 props `textContent`（未入库纯文本）、`title`；`file` 仅用于展示文件名和大小。

dependents（`@mango/admin`、`@mango/admin-pages`、`@mango/admin-shell`、`@mango/calendar`、`@mango/cms`、`@mango/detail`、
`@mango/job`、`@mango/link`、`@mango/notice`、`@mango/numgen`、`@mango/payment`、`@mango/system`、`@mango/template`、
`@mango/workflow`、`@mango/workflow-business-example`）：仅为依赖闭包随 `@mango/file` 版本同步而发 patch，
没有 API 变更；仓库内调用方已改为只传 `file-id`（`packages/file/src/views/files/index.vue` 不再传预览服务地址相关 props）。
