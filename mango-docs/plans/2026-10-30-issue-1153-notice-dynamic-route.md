# 标准交付记录

## Issue #1153 Notice 动态 ShellMenu 目标解析

> 本记录同时承载本次 STANDARD 技术决定、实施清单和验收映射。它不新增长期规范。

## 0. 先读

- **一句话目标**：让 Notice 自定义按钮继续通过事件交给业务处理，并让 ROUTE 目标复用 Admin Shell 当前用户的动态菜单树，不再把合法 ShellMenu 路径误判为未注册。
- **真实场景**：财务用户点击站内信“去确认”，目标为 `/guarantee/finance/payment-vouchers`，Shell 根据授权菜单和页面组件加载收款确认页面。
- **输入/输出**：输入为 `targetKey`、动作参数和当前用户菜单树；输出为可导航路径、业务事件或明确失败提示。
- **成功边界**：已授权且页面可运行的动态路径可以跳转，`receiptId/orderNo/focus` 等 query 保留；弹窗和其它自定义动作仍通过 `interaction` 事件交给宿主。
- **失败边界**：不在菜单树、无权限、页面不可运行或路径不安全的目标不得导航；不能把不存在的目标当作成功。
- **明确不做**：不改后端通知协议，不为每个动态菜单调用 `router.addRoute()`，不让 Notice 公共包依赖 Admin Shell store，不用“允许所有安全绝对路径”绕过菜单校验。
- **阅读顺序**：先看当前目标解析与 Shell 菜单加载，再看技术决定、测试映射和剩余风险。

## 1. 元数据

- 任务 ID：`issue-1153-notice-dynamic-route`
- Pull Request：`#1156`（已合并，merge commit：`847a36430b183be6843bdf72bc3a0489550cb28c`；当前 `main` 已同步）
- 交付模式：STANDARD
- 需求影响：L2 - 影响已登录用户的通知主操作、业务页面导航、权限和异常边界。
- 方案风险：L2 - 修改共享 Notice 目标解析、Admin Shell 动态菜单解析和跨包 provider 边界。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-1153-notice-route`
- 评审选择：用户请求 `EXTERNAL_PEER_REVIEW`；当前环境无真实外部评审人，以下为隔离的 Agent 角色复核，不冒充人工评审。

## 2. 目标与范围

- 目标：统一 Notice ROUTE 目标与 Shell 动态菜单模型。
- 成功条件：
  1. 动态 ShellMenu 路径可以从站内信和顶部铃铛动作导航。
  2. 动态 menuCode 可以解析为当前用户可访问路径。
  3. 未授权或不存在目标不会被误报为成功。
  4. 自定义弹窗/页面调整动作仍由 `interaction` 事件交给业务宿主。
  5. query 参数经过现有敏感字段过滤后继续传递。
- 处理范围：`@mango/notice`、`@mango/admin-shell`、`@mango/admin-extension` 目标导航 provider、单元测试和能力说明。
- 不处理范围：后端通知数据模型、业务项目收款确认页面、Vue Router 全量动态注册、历史固定别名删除。

## 3. 可观察系统要求

| ID          | 参与者或入口    | 输入或前置条件                                               | 预期行为                                                      | 失败语义                             | 验收标准                             |
| ----------- | --------------- | ------------------------------------------------------------ | ------------------------------------------------------------- | ------------------------------------ | ------------------------------------ |
| AC-1153-001 | 站内信动作      | 已登录用户拥有动态菜单 `/guarantee/finance/payment-vouchers` | 点击 ROUTE 按钮进入目标页面，并保留 `receiptId/orderNo/focus` | 无菜单或无权限时不导航并提示不可访问 | 动态路径解析测试通过                 |
| AC-1153-002 | 顶部铃铛动作    | 同上                                                         | 与站内信页面使用同一目标解析规则                              | 不允许两入口结果不一致               | 顶部处理器和目标解析单测通过         |
| AC-1153-003 | 自定义业务动作  | `interactionType=ROUTE/FLOW/EVENT` 携带业务参数              | Notice 派发语义事件；业务宿主决定页面调整、弹窗或事件提交     | Notice 不推断业务弹窗实现            | 事件 payload 测试通过                |
| AC-1153-004 | 非法/不存在目标 | 外部 URL、控制字符、未授权菜单或未注册页面                   | 不执行导航，给出明确失败提示                                  | 不得显示成功或静默吞错               | 安全路径、菜单缺失和组件缺失测试通过 |

## 4. 技术决定

| ID          | 对应要求        | 接口/数据/权限/兼容性决定                                                                                                                                       | 影响路径                                                                                                                    | 回滚方式                                      |
| ----------- | --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| TD-1153-001 | AC-1153-001/002 | 在 `@mango/admin-extension/notice` 增加轻量 `MangoNoticeNavigationProvider` 注册表。Provider 只返回宿主确认过的路径，不依赖 Vue、Router 或 Shell store。        | `mango-ui/packages/admin-extension/src/notice.ts`、`mango-ui/packages/notice/src/client/targets.ts`                         | 删除 provider 分支，保留现有静态/命名路由逻辑 |
| TD-1153-002 | AC-1153-001/004 | Admin Shell provider 使用当前 `routesList`，按精确动态路径或 `menuCode` 查找菜单；只接受存在页面组件、iframe 或外部入口的菜单，隐藏但可直接访问的菜单允许通过。 | `mango-ui/packages/admin-shell/src/runtime/menuHost.ts`、`mango-ui/packages/admin-shell/src/runtime/ShellRuntimeOutlet.vue` | 恢复原有 `router.resolve` 校验                |
| TD-1153-003 | AC-1153-003     | Notice 保持现有 `interaction` 事件协议。公共 Notice 组件不直接依赖宿主 Router、菜单或权限；页面跳转由宿主 provider 处理，弹窗/页面状态由业务消费者处理。        | `mango-ui/packages/notice/src/client/NoticeClientMessageCenter.vue`、`mango-ui/packages/notice/src/client/interaction.ts`   | 不改变事件字段，仅改变 ROUTE 解析入口         |
| TD-1153-004 | AC-1153-001/004 | Provider 存在时，安全绝对路径以动态菜单树为权威；Provider 不存在时保留独立 Notice 宿主的静态/命名路由兼容行为。                                                 | `mango-ui/packages/notice/src/client/targets.ts`                                                                            | 删除 provider 注册，不影响非 Shell 宿主       |
| TD-1153-005 | 全部            | 不新增后端字段和数据库数据；保留 query 敏感字段过滤和 fallback 语义。                                                                                           | Notice target tests、README                                                                                                 | 单次回退到旧版本                              |

## 5. 隔离 Agent 角色复核

### 5.1 事实与用户视角

- 来源：Issue #1153、当前 Notice target resolver、Shell router/menu runtime。
- 结论：用户需要的是合法动态业务页面可达，以及自定义按钮继续交给业务处理；不是把所有业务页面改成静态 Vue Router 路由。
- 反对意见：如果业务宿主不是 Admin Shell，provider 可能不存在。
- 处理：保留无 provider 的静态/命名路由兼容路径，并不改变独立 Notice 宿主合同。

### 5.2 技术与风险视角

- 来源：`router.ts`、`menuHost.ts`、`ShellRuntimeOutlet.vue`、`admin-extension/notice.ts`。
- 结论：菜单树是 Shell 当前用户访问能力的权威来源；Notice 不能直接依赖 Shell store，provider 是最小依赖反转边界。
- 反对意见：全局 provider 可能出现旧 Shell 实例残留。
- 处理：provider 提供清理入口；Shell 在初始化后注册，在销毁时清理；测试显式 reset。当前 Admin Shell 已采用同类全局 Notice provider 机制，未引入新的运行时依赖。

### 5.3 验证与交付视角

- 来源：现有 Notice target tests、Admin Shell menuHost tests、前端测试规范。
- 结论：必须补充动态路径、动态 menuCode、无权限和事件 payload 测试；仅测试 `isSafeNoticePath` 不足以证明修复。
- 反对意见：当前仓库缺少 baohan-system 真实业务页面和账号，不能在本仓库声称完成业务现场验收。
- 处理：完成 Mango 本地合同、单元和构建验证；在 baohan-system 真实环境补充站内信和顶部铃铛收款确认跳转回归。站内信与顶部铃铛均已通过真实浏览器验证；业务自定义弹窗仍不属于本次自动化证据范围。

### 5.4 复核结论

- Agent 角色复核：`PASS WITH CONDITIONS`。
- 阻断项：无代码设计阻断；真实外部同行评审未完成，不能将本结论称为人工同行评审通过。
- 必须满足的条件：不扩大 Notice 公共包对 Shell 的依赖；Provider 缺失时保持兼容；动态菜单缺失时不能放行；补齐单测和构建验证。

## 5. 实施清单

| ID            | 对应决定        | 顺序 | 改动路径                                                           | 完成条件                                   |
| ------------- | --------------- | ---: | ------------------------------------------------------------------ | ------------------------------------------ |
| TASK-1153-001 | TD-1153-001     |    1 | `mango-ui/packages/admin-extension/src/notice.ts`                  | provider 类型、注册、读取和 reset API 完成 |
| TASK-1153-002 | TD-1153-002     |    2 | `mango-ui/packages/admin-shell/src/runtime/menuHost.ts`            | 动态路径/menuCode 解析函数和边界测试完成   |
| TASK-1153-003 | TD-1153-001/002 |    3 | `mango-ui/packages/admin-shell/src/runtime/ShellRuntimeOutlet.vue` | Shell 菜单加载后注册 provider，卸载时清理  |
| TASK-1153-004 | TD-1153-003/004 |    4 | `mango-ui/packages/notice/src/client/targets.ts`                   | provider 优先解析动态目标，兼容独立宿主    |
| TASK-1153-005 | 全部            |    5 | Notice/Admin Shell 测试和 README                                   | 回归测试、能力说明和 package build 完成    |

## 6. 验收映射与结果

| 要求 ID     | 验证方式      | 命令或步骤                                                                  | 结果                                                                                           | 证据                                                                                                                                                                |
| ----------- | ------------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| AC-1153-001 | 单元/组件     | `pnpm --filter @mango/notice test`、`pnpm --filter @mango/admin-shell test` | PASS：Notice 54 tests、Admin Shell 100 tests                                                   | 本地 Vitest 输出                                                                                                                                                    |
| AC-1153-002 | 单元          | `pnpm --filter @mango/admin-shell test`                                     | PASS：动态路径、动态 menuCode、隐藏可运行页面和缺失页面覆盖                                    | `mango-ui/packages/admin-shell/src/__tests__/menuHost.spec.ts`                                                                                                      |
| AC-1153-003 | 组件/公共契约 | `pnpm --filter @mango/notice test`                                          | PASS：既有 interaction payload 契约保持；新增导航 provider 导出                                | `mango-ui/packages/notice/src/client.ts`、Notice Vitest 输出                                                                                                        |
| AC-1153-004 | 单元/构建     | 三个 package test/build、Prettier、`git diff --check`                       | PASS：admin-extension 6 tests、Notice 54 tests、Admin Shell 100 tests；三个 package build 通过 | 本地命令输出                                                                                                                                                        |
| AC-1153-001 | 业务现场      | baohan-system 财务用户从站内信点击收款确认动作                              | PASS：真实动态路径、`orderNo/receiptId/focus` 参数和目标行高亮通过                             | `/Users/hardy/Work/Yunxin/baohan-system-issue-1153-real-notice/baohan-test/.runtime/baohan-test/issue-1153-notice-navigation-20261030151932-manual-fix/result.json` |
| AC-1153-002 | 业务现场      | baohan-system 财务用户从顶部铃铛打开收款确认通知并点击查看订单              | PASS：顶部铃铛展示未读消息、打开详情、动态跳转和目标行高亮通过                                 | `/Users/hardy/Work/Yunxin/baohan-system-issue-1153-real-notice/baohan-test/.runtime/baohan-test/issue-1153-notice-bell-navigation-20261030155839-pass/result.json`  |

## 7. 例外与剩余风险

- 真实外部同行评审：当前环境无法提供独立评审人，已明确标记为未完成，不伪造人工结论；本次仅完成隔离 Agent 角色复核。
- 真实 baohan-system 浏览器验收：站内信和顶部铃铛收款确认跳转已完成真实回归；顶部铃铛证据记录为 `/Users/hardy/Work/Yunxin/baohan-system-issue-1153-real-notice/baohan-test/.runtime/baohan-test/issue-1153-notice-bell-navigation-20261030155839-pass/result.json`。业务自定义弹窗仍需由对应业务模块接入并单独验收。
- Release impact checker 在当前已封存的 release source tuple 上要求同步完整 npm 版本批次；本次未执行版本批次发布，不能据此声明发布完成。
- Provider 使用现有 admin-extension 全局注册模式；若未来支持同页面多 Admin Shell 实例，需要将 provider 生命周期提升为实例级上下文，本次不扩展该范围。
