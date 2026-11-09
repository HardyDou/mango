# 标准交付记录

> Issue #1182 消息自定义按钮目标解析与兜底

## 1. 元数据

- 任务 ID：Issue #1182
- 交付模式：STANDARD
- 需求影响：L2：消息详情弹窗主按钮（工作流终态「查看申请」与其他模块自定义按钮）的跳转行为变化。
- 方案风险：L2：新增前端解析顺序与后端 `fallbackTargetKey` 契约，跨 `@mango/notice`、`@mango/admin-shell`、workflow starter。
- 最终风险：L2
- 工作区决策：CREATE：`/Users/hardy/Work/mango-issue-1182`，分支 `fix/issue-1182-notice-target`
- 决策复核：`AGENT_ONLY`；未指定外部同行评审。

## 2. 目标与范围

- 目标：消息按钮不再出现“目标未注册或当前无权访问”死路；工作流终态消息在收件人无办理人菜单权限时仍可打开可访问页面。
- 成功条件：`targetKey` 不可解析时按 `fallbackTargetKey` → 消息中心顺序兜底；后端终态通知始终带可达 `fallbackTargetKey`；文档写明直达业务详情需业务配置 `customConfig.viewPath`。
- 处理范围：`@mango/notice` 目标解析与两处交互入口、`@mango/admin-shell` 顶部铃铛、workflow starter 通知目标、notice/workflow README、业务接入指南与变更索引。
- 不处理范围：不新增菜单、不改变权限模型（没有授权的菜单页面不会被绕过）、不改任务审批接口、不改消息发送与接收数据模型、不做浏览器端到端验收。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| U1182-01 | 站内信详情「查看申请」 | 工作流终态消息，收件人无 `workflow:task:done` 菜单 | 打开消息中心并提示已兜底 | 连消息中心不可用时提示“目标未注册或当前无权访问” | 前端用例：主目标不可解析时返回 `message-center` 并导航 `/profile?tab=notice-site-message` |
| U1182-02 | 同上 | 消息参数带 `fallbackTargetKey` 且可达 | 打开 `fallbackTargetKey` 页面 | 不可达时继续退到消息中心 | 前端用例：`openNoticeTarget` 返回 `fallback` 并导航目标路径 |
| U1182-03 | 顶部铃铛单条消息 | 同 U1182-01 | 与站内信详情行为一致 | 同上 | `@mango/admin-shell` 测试与构建通过 |
| U1182-04 | 工作流终态通知 | `PROCESS_COMPLETED`/`PROCESS_REJECTED` 且无 `viewPath` | 下发 `fallbackTargetKey=notice:site-message` | 不适用 | `WorkflowNoticeDomainEventSubscriberTest` 断言目标与兜底参数 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1182-01 | U1182-01/U1182-03 | 新增 `openNoticeTarget(router, targetKey, params)` 统一解析顺序：静态目标 → 菜单/已注册路由 → `fallbackTargetKey` → 消息中心；两个入口复用，不再各写一套跳转 | `mango-ui/packages/notice/src/client/targets.ts`、`views/site-message/index.vue`、`admin-shell/src/layout/navBars/index.vue` | 回退为原 `resolveNoticeTargetLocation` + 提示 |
| D1182-02 | U1182-01 | 新增静态目标 `notice:site-message` → `/profile` + 固定 `tab=notice-site-message`，不依赖菜单授权 | 同上 | 移除静态映射 |
| D1182-03 | U1182-02/U1182-04 | 后端终态通知在主目标等于默认目标时，兜底改为 `notice:site-message`；`viewPath` 场景保持原 `fallbackTargetKey` | `WorkflowNoticeDomainEventSubscriber` | 回退常量与判断 |
| D1182-04 | 全部 | 不绕过菜单授权：兜底只到当前用户必然可访问的消息中心，不打开无权页面 | 前端解析顺序 | 无需数据回滚 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I1182-01 | D1182-01/D1182-02 | 1 | `mango-ui/packages/notice/src/client/targets.ts`、`client.ts`、`views/site-message/targets.ts` | 解析顺序与静态兜底目标可用 |
| I1182-02 | D1182-01 | 2 | `mango-ui/packages/notice/src/views/site-message/index.vue`、`mango-ui/packages/admin-shell/src/layout/navBars/index.vue` | 两处入口复用统一解析并提示兜底原因 |
| I1182-03 | D1182-03 | 3 | `mango/mango-platform/mango-workflow/mango-workflow-starter/.../WorkflowNoticeDomainEventSubscriber.java` | 终态通知始终带可达兜底目标 |
| I1182-04 | 全部 | 4 | 前端/后端测试、`mango-ui/packages/notice/README.md`、workflow README、业务接入指南、变更索引 | 用例与文档同步 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| U1182-01/U1182-02 | 前端单元测试 | `pnpm --filter @mango/notice test` | PASS：12 个文件、59 个用例，含兜底到消息中心、fallbackTargetKey、已在消息中心返回 `none` | 本会话 vitest 输出 |
| U1182-03 | 前端单元测试 + 构建 | `pnpm --filter @mango/admin-shell test`；`pnpm --filter @mango/admin-shell build` | PASS：19 个文件、100 个用例；构建成功 | 本会话输出 |
| U1182-04 | 后端单元测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter test -Dtest=WorkflowNoticeDomainEventSubscriberTest` | PASS：10 个用例，终态通知目标 `workflow:task:done` + `fallbackTargetKey=notice:site-message` | 本会话 surefire 输出 |
| 受影响前端门禁 | PR 前端质量门禁 | `pnpm check:pr -- --base=origin/main --head=HEAD` | PASS（存在仓库既有 advisory：`@mango/workflow` 的 `startProcessIcons.spec.ts` 在 `main` 同样失败） | 本会话输出 |
| 受影响包构建 | 包构建 | `pnpm --filter @mango/notice build`、`pnpm --filter @mango/admin-shell build` | PASS | 本会话构建输出 |
| 文档门禁 | 文档检查 | `check-governance-intent`、`check-business-guides`、`check-capability-docs` | PASS | 本会话输出 |

## 7. 例外与剩余风险

- 未做浏览器端到端走查：本地未启动业务前端与后端联调环境，按钮点击后的实际视觉与网络行为未人工确认。风险为提示文案或兜底落点与预期不一致，需发布前由 QA 在真实环境点一次工作流终态消息。
- `catalog:check` 在 `main` 即失败（跟踪的 Catalog 投影与编译输出不一致），非本次改动引入，未纳入本次验证范围。
- 业务「按钮直达自己的详情页」仍需业务在申请快照 `customConfig.viewPath` 配置站内路径；平台不会猜业务页面，也不会绕过菜单授权。
