# 标准交付记录

## 1. 元数据

- 任务 ID：mango-dialog-padding-drag
- 交付模式：STANDARD
- 需求影响：L1 - 仅增强开启 `draggable` 后的局部拖拽入口，不改变默认弹框、公共 API、数据或权限。
- 方案风险：L2 - 修改跨业务复用的 `@mango/common` 公共组件交互，需要防止内容操作误触拖拽。
- 最终风险：L2
- 工作区决策：REUSE - `D:\Project\mango-dialog-padding-drag`，分支 `codex/mango-dialog-padding-cursor`。

## 2. 目标与范围

- 目标：让开启 `draggable` 的 `MangoDialog` 可从内容区四周实际生效的空白内边距启动拖拽。
- 成功条件：标题区和四周 padding 可拖拽并显示四向移动光标；内容盒、slot 子元素、滚动条和未开启 draggable 的弹框不可由正文区域拖拽或显示可拖拽反馈；现有布局不变化。
- 处理范围：`@mango/common` 的拖拽命中与光标反馈逻辑、组件测试、公共能力说明和版本变更声明。
- 不处理范围：默认内边距数值、弹框 DOM 布局、拖拽视口约束、resize 行为、业务页面私有弹框和后端能力。

## 3. 可观察系统要求

| ID     | 参与者或入口                  | 输入或前置条件                                     | 预期行为                                     | 失败语义                             | 验收标准                                              |
| ------ | ----------------------------- | -------------------------------------------------- | -------------------------------------------- | ------------------------------------ | ----------------------------------------------------- |
| SR-001 | 使用 `MangoDialog` 的后台用户 | 弹框设置 `draggable=true` 且 body 存在非零 padding | 按住四周空白 padding 可移动整个弹框          | padding 按下后弹框不移动             | 四个 padding 方向使用同一命中规则，拖拽位置随指针变化 |
| SR-002 | 弹框内容交互                  | 指针位于内容盒、slot 子元素或滚动条                | 保持点击、输入、选择、预览和滚动，不启动拖拽 | 内容操作导致弹框移动或默认交互被阻止 | 内容盒和子元素 pointerdown 后不产生 fixed 拖拽布局    |
| SR-003 | 普通弹框用户                  | `draggable=false`                                  | 内容区所有位置保持原行为                     | 普通弹框被正文区域拖动               | padding pointerdown 后弹框位置不变化                  |
| SR-004 | 使用 `MangoDialog` 的后台用户 | 指针悬停在 draggable 弹框的 body 区域              | 仅四周可拖拽 padding 显示四向移动光标        | 可拖拽区域无反馈或内容区错误显示反馈 | 四边显示 `move`；内容、slot、滚动条和离开后不显示     |

## 4. 技术决定

| ID     | 对应要求       | 接口/数据/权限/兼容性决定                                                                                                  | 影响路径                                                  | 回滚方式                                                |
| ------ | -------------- | -------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------- |
| TD-001 | SR-001、SR-002 | 在 body 捕获阶段同时检查事件目标和基于 computed style 的 padding 几何边界；使用 client box 排除滚动条，不新增 prop 或 emit | `MangoDialog/useDialogWindow.ts`、`MangoDialog/index.vue` | 恢复 body 原 `bringToFront` 监听并删除 padding 命中函数 |
| TD-002 | SR-001～SR-003 | 复用既有 Pointer Events 拖拽状态机，只有精确命中时调用 `startInteraction`；其它事件继续仅置顶                              | `MangoDialog` 组件测试                                    | 删除新增用例并恢复原事件入口                            |
| TD-003 | SR-001～SR-003 | M08 启用：更新 Common README、能力地图和 `@mango/common` patch changeset；公共 API 签名不变                                | README、能力地图、changeset                               | 随代码行为一起回滚对应说明                              |
| TD-004 | SR-004         | 复用 padding 几何命中结果维护 hover 状态，不给整个 body 静态设置 cursor；拖拽期间固定使用 `move` 光标                      | `MangoDialog/useDialogWindow.ts`、`MangoDialog/index.vue` | 删除 hover 状态、事件绑定和状态样式                     |

## 5. 实施清单

| ID       | 对应决定       | 顺序 | 改动路径                                          | 完成条件                                               |
| -------- | -------------- | ---: | ------------------------------------------------- | ------------------------------------------------------ |
| TASK-001 | TD-001、TD-002 |    1 | `mango-ui/packages/common/components/MangoDialog` | padding 精确命中并复用现有拖拽逻辑                     |
| TASK-002 | TD-002         |    2 | `MangoDialog.spec.ts`                             | 覆盖 padding、内容盒、slot 子元素和关闭 draggable 场景 |
| TASK-003 | TD-003         |    3 | Common README、能力地图、changeset                | 使用说明、能力索引和版本策略同步                       |
| TASK-004 | TD-001～TD-003 |    4 | 定向验证                                          | 组件测试、构建、静态检查和浏览器交互验证完成           |
| TASK-005 | TD-004         |    5 | `MangoDialog` composable、模板、说明和组件测试    | 四边光标反馈与实际拖拽命中范围保持一致                 |

## 6. 验收映射与结果

| 要求 ID        | 验证方式                     | 命令或步骤                                                                                                          | 结果 | 证据                                                                         |
| -------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------- | ---- | ---------------------------------------------------------------------------- |
| SR-001         | M10 组件测试                 | `pnpm --filter @mango/common test`                                                                                  | PASS | `MangoDialog.spec.ts` 四侧 padding 参数化用例，29 个文件、333 条测试全部通过 |
| SR-002         | M10 组件测试、M13 浏览器交互 | 内容盒、slot 子元素、滚动条用例；浏览器中验证内容面板、输入框和按钮                                                 | PASS | `dialog-padding-drag.png`；控制台无 error/warning                            |
| SR-003         | M10 组件测试                 | `pnpm --filter @mango/common test`                                                                                  | PASS | `draggable=false` 的 padding 不触发 fixed 拖拽布局                           |
| SR-001～SR-003 | M09 静态验证                 | common build/typecheck、定向 ESLint/Prettier、`component-contracts:check`、release change check、`git diff --check` | PASS | 所有定向命令通过                                                             |
| SR-004         | M10 组件测试、M13 浏览器交互 | 四边、内容盒、slot 子元素、滚动条、pointerleave 和 `draggable=false` 光标检查                                       | PASS | 29 个文件、333 条测试通过；Chromium computed cursor 与真实拖拽验证通过       |

### 6.1 浏览器验证结果

- 从左侧 padding 拖拽后，弹框由 `(0, 0)` 移动至 `(100, 60)`。
- 在内容面板拖拽后，弹框仍保持 `(100, 60)`。
- 输入框可正常输入“内容交互正常”，按钮计数由 0 更新为 1。
- 浏览器截图：`mango-docs/evidence/mango-dialog-padding-drag/dialog-padding-drag.png`。
- 光标返工验证：上、右、下、左 padding 的 computed cursor 均为 `move`，内容面板和真实滚动条均为 `auto`。
- 从左侧 padding 拖拽后，弹框由 `(0, 0)` 移动至 `(100, 60)`；松开后 document cursor 恢复为空，控制台无 error/warning。

## 7. 例外与剩余风险

- 全仓 `pnpm typecheck` 未通过：当前 worktree 中 30 个其他 workspace 共报告 993 条诊断，主要为 `@mango/*` 构建产物/模块声明无法解析及既有类型诊断，超过 ratchet 基线；诊断未指向本次改动文件。`pnpm exec vue-tsc --noEmit --incremental false -p packages/common/tsconfig.json` 已通过。
- 本次仅验证 Chromium 桌面端 Pointer Events；未单独执行 Firefox、WebKit 和移动触控设备实测。实现复用现有 Pointer Events 状态机，剩余兼容风险较低。
