# 标准交付记录

## 1. 元数据

- 任务 ID：issue-1169-dialog-align-center
- 交付模式：STANDARD
- 需求影响：L2 - `@mango/common` 的 `MangoDialog` 在所有业务项目默认不居中，平台页面与业务页面弹窗全部错位，业务项目需要各自写主题 CSS 绕行。
- 方案风险：L2 - 修改跨业务复用的公共组件根样式，需要同时保持拖拽、缩放、遮罩和内部滚动行为。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-1169`，分支 `fix/issue-1169-1171-dialog-center-poi-conflict`（`M01=CREATE`，`mango workspace init` slot 17）。两个 Issue 按用户要求在同一个 worktree 修复并合并在一个 PR 中。

## 2. 目标与范围

- 目标：`MangoDialog` 默认水平、垂直居中，高度随内容自适应，内容超出时在 `--mango-dialog-max-height` 内滚动，且拖拽/缩放态行为不变。
- 成功条件：同一组件在 1500x1000 与 2560x1415 视口中，短内容弹框四边留白相等且高度等于内容高度；长内容弹框高度等于 90vh 且内容区可滚动；拖拽后位置按指针位移变化。
- 处理范围：`MangoDialog` 根样式、组件 README 布局行为说明、`@mango/common` changeset、浏览器几何证据。
- 不处理范围：`MangoDialog` props/emits/expose 变更、Element Plus 版本、页面级弹框样式、`mango-pmo` 规则调整。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 业务页面弹框 | 打开任意 `MangoDialog`，内容高度小于视口 90% | 弹框水平和垂直居中，高度等于内容高度 | 弹框贴左上角或被拉伸到 90vh | 视口 1500x1000 时左右留白相等且上下留白相等 |
| SR-002 | 业务页面弹框 | 内容高度超过视口 90% | 高度封顶 90vh，内容区内部滚动 | 弹框超出视口或页面本身滚动 | 弹框高度等于 0.9×视口高度，内容区 `scrollHeight > clientHeight` |
| SR-003 | 可拖拽/可缩放弹框 | `draggable`/`resizable` 下开始拖拽 | 拖拽按指针位移移动，缩放改变宽高 | 拖拽无效或位置回弹 | 拖拽 (-120,-80) 后位置偏移量与位移一致 |
| SR-004 | 全部视口 | 任意内容长度 | 不因居中产生底部空白 | 短内容弹框底部出现大片空白 | 短内容弹框高度等于 header+body+footer 内容高度 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001、SR-002、SR-004 | 根样式 `margin: 0` 改为 `margin: auto`；Element Plus 在 `align-center` 下把 `.el-overlay-dialog` 设为 flex 容器，auto 外边距在交叉轴阻止 stretch，弹框高度回到内容高度 | `mango-ui/packages/common/components/MangoDialog/index.vue` | 恢复 `margin: 0` |
| TD-002 | SR-003 | 拖拽/缩放态继续由 `useDialogWindow` 写行内 `position: fixed; margin: 0`，类样式不覆盖行内值 | 同上（无代码改动，仅回归验证） | 随 TD-001 回滚 |
| TD-003 | SR-001～SR-004 | 公共 API 不变；按 `M08` 更新组件 README 的「布局行为」小节与 `@mango/common` patch changeset | `MangoDialog/README.md`、`mango-ui/.changeset/issue-1169-dialog-align-center.md` | 随代码回滚文档与 changeset |

### 4.1 决策复核

- 事实与用户视角：Issue #1169 记录业务仓 `mango-baohan-risk` 的弹窗全部错位，业务侧用 `frontend/src/styles/mango-dialog-layout.css` 覆盖 `.mango-dialog.el-dialog.is-align-center { margin: auto }` 绕行，验证 18 组弹窗×视口。
- 技术与风险视角：`element-plus@2.14.1` 的 `use-dialog.mjs` 在 `align-center` 下给 `.el-overlay-dialog` 写行内 `display: flex`；`theme-chalk/src/dialog.scss` 的 `.el-dialog.is-align-center { margin: auto }` 与组件根选择器 `.mango-dialog.el-dialog` 特异性同为 0,2,0，组件样式后加载使 `margin: 0` 生效，弹框既失去 auto 居中又被交叉轴拉伸到 `max-height`。
- 验证与交付视角：同一 harness 分别在 `main`（修复前）和任务分支（修复后）渲染真实组件，输出几何数据与截图；组件测试 406 条全部通过。
- 评审选择：`AGENT_ONLY`（用户未要求外部同行评审；结论保留再次发起外部评审的入口）。

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-001 | 1 | `mango-ui/packages/common/components/MangoDialog/index.vue` | 根样式改为 `margin: auto` 并写清原因 |
| TASK-002 | TD-003 | 2 | `MangoDialog/README.md`、`mango-ui/.changeset/issue-1169-dialog-align-center.md` | 说明布局行为并声明 `@mango/common` patch |
| TASK-003 | TD-001～TD-003 | 3 | 组件测试、构建、浏览器几何验证 | 406 个组件测试通过；两种视口几何数据与截图留存 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001、SR-004 | M13 UI 验证 | `measure.mjs` 打开短内容弹框，读取 `.mango-dialog.el-dialog` 与 `.el-overlay-dialog` 的 `getBoundingClientRect` 与 computed `margin` | PASS | 1500x1000：修复前 `600x900 @ (0,0) margin 0px`，修复后 `600x306 @ (450,347) margin 347px 450px`；2560x1415：修复前 `600x1273.5 @ (0,0)`，修复后 `600x306 @ (980,554.5)` |
| SR-002 | M13 UI 验证 | 打开 2400px 高内容弹框，比较弹框高度与内容区 `scrollHeight/clientHeight` | PASS | 1500x1000 高度 900 = 90vh，内容区 `scrollHeight 2440 > clientHeight 754`；2560x1415 高度 1273.5 = 90vh |
| SR-003 | M13 UI 验证 | 打开 `draggable` + `resizable` 弹框，鼠标拖拽标题区 (-120,-80) | PASS | 2560x1415：位置由居中 `(980,559.5)` 变为 `(860,479.5)`；1500x1000：由 `(450,352)` 变为 `(330,272)`；拖拽后 computed `margin: 0px` 且 `max-height: none` |
| 全部 | M09 静态验证 | `pnpm -F @mango/common build`、`prettier --check`、`pnpm admin:styles:check`、`pnpm admin:module-styles:check`、`pnpm release:change-check --include-working-tree` | PASS | 构建成功；样式聚合检查通过；release change check 识别 `@mango/common` 并给出依赖闭包 |
| 全部 | M10 组件测试 | `pnpm -F @mango/common test` | PASS | 39 个测试文件、406 条测试全部通过 |
| SR-001～SR-004 | 浏览器控制台 | 两种视口下采集 console | PASS | 1500x1000 无 error/warning；2560x1415 首屏出现一条 404，来源为 Vite dev server 依赖预构建产物请求，修复前后一致，与本次改动无关 |

### 6.1 对照数据

| 场景 | 修复前 | 修复后 |
|---|---|---|
| 短内容 1500x1000 | `600x900 @ (0,0)`，margin `0px` | `600x306 @ (450,347)`，margin `347px 450px` |
| 短内容 2560x1415 | `600x1273.5 @ (0,0)`，margin `0px` | `600x306 @ (980,554.5)`，margin `554.5px 980px` |
| 长内容 1500x1000 | `600x900 @ (0,0)`，内容区不滚动 | `600x900 @ (450,50)`，内容区滚动 |
| 拖拽 1500x1000 | `(0,0)` 起拖 | 居中后按指针位移移动，位置 `(330,272)` |

### 6.2 真实场景验证

`mango dev start` 启动本 worktree 的前后端（后端 `18017`、前端 `30017`、数据库 `mango_dev_mango_issue_1169_017`），用 Chrome 以 `admin` 登录真实管理端，进入菜单「角色管理」(`#/system/role`) → 「新增角色」弹框，采集真实页面几何：

| 视口 | 弹框尺寸 | 位置 | 水平居中 | 垂直居中 | 内容区 | console |
|---|---|---|---|---|---|---|
| 1500x1000 | `560x603` | `(470, 198.5)` | 是 | 是 | `clientHeight 481 = scrollHeight 481` | 无 error/warning |
| 2560x1415 | `560x603` | `(1000, 406)` | 是 | 是 | `clientHeight 481 = scrollHeight 481` | 无 error/warning |

证据：`real-role-create-dialog-1500x1000.png`、`real-role-create-dialog-2560x1415.png`、`result-real.json`、`real-dialog-check.mjs`。

## 7. 例外与剩余风险

- 未执行 `M16` 人工验收：修复效果由浏览器几何数据与截图证明，未由业务责任人在真实业务页面确认；业务项目删除本地主题覆盖后需自行走查一次。
- `pnpm component-contracts:check` 与 `pnpm package-exports:check` 在 `main` 主工作区同样失败（`@mango/common:.:GraphicCaptcha` 未分类、`@mango/workflow-business-example` 缺 `dist` 产物），属存量问题，本次未处理。
- `MangoDialog/README.md` 在 `main` 已存在 prettier 格式告警，本次仅新增段落，未扩大格式改动范围。
- 未执行发布：`@mango/common` 版本号与 `CHANGELOG.md` 由后续发布流程负责，本次只提供 patch changeset。
