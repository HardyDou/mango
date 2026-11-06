# MangoDialog 居中布局验证

本目录保存 Issue #1169 的浏览器几何证据和可重复执行的验证脚本。

## 结论

`MangoDialog` 根样式由 `margin: 0` 改为 `margin: auto` 后，弹框在 Element Plus `align-center` 的 flex overlay 中恢复水平、垂直居中，高度随内容自适应；拖拽和缩放态继续使用行内固定定位。

## 文件

| 文件 | 用途 |
|---|---|
| `layout-before.json` | `main` 版本组件的几何数据（修复前） |
| `layout-after.json` | 任务分支组件的几何数据（修复后） |
| `before-short-1500x1000.png` | 修复前短内容弹框贴左上角并被拉伸 |
| `after-short-1500x1000.png` | 修复后短内容弹框居中且高度随内容 |
| `after-tall-1500x1000.png` | 修复后长内容弹框高度封顶 90vh |
| `after-drag-1500x1000.png` | 修复后拖拽位置 |
| `before-short-2560x1415.png`、`after-short-2560x1415.png` | 大视口对照 |
| `main.ts`、`vite.config.mts`、`measure.mjs` | 验证 harness：真实渲染 `MangoDialog` 并读取几何数据 |

## 复现步骤

1. 在仓库根的 `.runtime/dialog-lab/` 放入 `main.ts`、`vite.config.mts`、`measure.mjs`，并把 `node_modules` 指向 `mango-ui/node_modules`。
2. 指定被测组件目录启动 dev server：

```bash
COMMON_NODE_MODULES=<repo>/mango-ui/packages/common/node_modules \
DIALOG_DIR=<repo>/mango-ui/packages/common/components/MangoDialog \
LAB_PORT=5199 <repo>/mango-ui/node_modules/.bin/vite --config ./vite.config.mts
```

3. 采集几何数据与截图：

```bash
LAB_LABEL=after LAB_PORT=5199 node measure.mjs
```

`measure.mjs` 使用本机 Chrome（`chromium.launch({ channel: 'chrome' })`），在 1500x1000 与 2560x1415 两种视口下分别测量短内容、长内容和拖拽场景。

## 说明

`measure.mjs` 中的 `round` 辅助函数定义在页面上下文内，因为 `page.evaluate` 只能使用浏览器内可用的符号。
