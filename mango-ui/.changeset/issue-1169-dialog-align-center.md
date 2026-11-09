---
'@mango/common': patch
---

修复 `MangoDialog` 默认不居中：Element Plus `align-center` 下 `.el-overlay-dialog` 是 flex 容器，组件根样式 `margin: 0` 覆盖了 `.el-dialog.is-align-center` 的 `margin: auto`，导致弹框贴在视口左上角并被拉伸到 `--mango-dialog-max-height`（90vh），短内容弹框底部出现大片空白。改为 `margin: auto` 后，弹框默认水平、垂直居中且高度随内容自适应，内容超出最大高度时仍在内容区滚动；拖拽和缩放态继续使用行内固定定位，行为不变。
