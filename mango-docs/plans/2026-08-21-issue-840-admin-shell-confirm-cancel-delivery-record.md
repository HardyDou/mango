# 标准交付记录

Issue #840 Admin Shell 确认取消异常边界

## 1. 元数据

- 任务 ID：GitHub Issue #840
- 交付模式：STANDARD
- 需求影响：L2 - 所有使用 Admin Shell 的管理端确认取消行为和全局异常提示发生变化
- 方案风险：L2 - 修改 Shell 共享错误边界，并覆盖外层应用与 runtime outlet 创建的内层 Vue App
- 最终风险：L2
- 工作区决策：REUSE（复用同一任务的 `fix/issue-840-admin-shell-confirm-cancel` 与 `/Users/hardy/Work/mango-issue-840`）

## 2. 目标与范围

- 目标：Admin Shell 将 Element Plus MessageBox 的 `cancel` 和 `close` 识别为正常用户取消，不再记录错误或显示系统错误。
- 成功条件：外层 Shell、runtime outlet 内层页面和 Mango 自带管理端使用同一分类规则；取消后不调用业务动作；真实异常继续记录并提示。
- 处理范围：`@mango/admin-shell` 错误边界、Mango Admin 全局错误入口、包级测试、代表性浏览器回归和能力说明。
- 不处理范围：逐个改写业务页面的确认框、Workflow Issue #760 已有局部保护、请求错误提示协议、发布和业务项目依赖升级。

## 3. 可观察系统要求

| ID  | 参与者或入口                            | 输入或前置条件                                              | 预期行为                                     | 失败语义                               | 验收标准                              |
| --- | --------------------------------------- | ----------------------------------------------------------- | -------------------------------------------- | -------------------------------------- | ------------------------------------- |
| R1  | 使用 `createMangoAdminApp` 的业务管理端 | 业务页面直接等待 `ElMessageBox.confirm`，用户点击取消或关闭 | 当前动作正常结束，无错误日志、无系统错误提示 | 不调用确认后的业务接口，不改变页面状态 | `cancel`、`close` 均不进入错误报告    |
| R2  | runtime outlet 本地业务页面             | Shell 为页面创建独立 Vue App                                | 内层 App 与外层 App 使用相同取消分类         | 宿主私有 errorHandler 不作为必要条件   | 每次 `installShellApp` 都安装统一边界 |
| R3  | Mango 自带管理端                        | Vue 事件异常或未处理 Promise rejection                      | 取消静默结束，非取消异常保持原错误处理       | 请求错误仍按现有规则避免重复消息       | 两个全局异常入口复用同一分类器        |
| R4  | 平台维护者                              | 抛出非取消异常                                              | 保留错误级日志并显示“系统错误，请刷新页面”   | 不得因取消修复吞掉真实异常             | 单测覆盖真实 Error 和非取消值         |

## 4. 技术决定

| ID  | 对应要求   | 接口/数据/权限/兼容性决定                                                                                                                | 影响路径                                            | 回滚方式                          |
| --- | ---------- | ---------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- | --------------------------------- |
| D1  | R1-R4      | 在 `@mango/admin-shell` 提供 `isElementPlusMessageBoxCancellation(error)`，仅识别 `cancel`、`close` 及带同名 `action` 的 MessageBox 结果 | `packages/admin-shell/src/errorHandling.ts`、包入口 | 删除分类器并恢复原错误处理        |
| D2  | R1、R2、R4 | `installShellApp` 在记录日志前过滤正常取消；runtime outlet 已统一调用该安装入口，不增加业务注入协议                                      | `appBootstrap.ts`、`runtimeHost.ts` 既有调用链      | 恢复 `appBootstrap.ts` 原 handler |
| D3  | R3         | Mango Admin 的 Vue handler 与 `unhandledrejection` 复用公共分类器；不保留第二份字符串判断                                                | `apps/mango-admin/src/main.ts`、`App.vue`           | 恢复应用本地判断                  |
| D4  | R1-R4      | 以包级单测证明分类和多 App 安装，以参数配置删除确认的浏览器用例证明 runtime outlet 用户结果                                              | Admin Shell tests、Mango Admin E2E                  | 删除新增测试不影响运行代码        |

## 5. 实施清单

| ID  | 对应决定 | 顺序 | 改动路径                                                                            | 完成条件                                  |
| --- | -------- | ---- | ----------------------------------------------------------------------------------- | ----------------------------------------- |
| I1  | D1、D2   | 1    | `mango-ui/packages/admin-shell/src/errorHandling.ts`、`appBootstrap.ts`、`index.ts` | 所有 Shell App 统一过滤取消，真实异常不变 |
| I2  | D3       | 2    | `mango-ui/apps/mango-admin/src/main.ts`、`App.vue`                                  | 自带管理端不复制取消规则                  |
| I3  | D4       | 3    | Admin Shell 单测、Mango Admin E2E                                                   | 覆盖取消、关闭、真实异常、零业务请求      |
| I4  | D1-D4    | 4    | README、业务接入指南、changeset、本记录                                             | 消费方升级影响和验证结果可追踪            |

## 6. 验收映射与结果

| 要求 ID | 验证方式           | 命令或步骤                                                                                                                                         | 结果             | 证据                                                                                                                                                                                        |
| ------- | ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| R1-R4   | M10 包级单测       | Node 22.23.1 下执行 `pnpm --filter @mango/admin-shell test`                                                                                        | PASS             | 14 files / 71 tests passed                                                                                                                                                                  |
| R1-R4   | M09 构建和静态检查 | Admin Shell build、Prettier、定向 ESLint、`git diff --check`、能力文档审计                                                                         | PASS             | build passed；ESLint 0 error（1 个既有 any warning）；模块 README、源码事实和业务接入指南审计通过                                                                                           |
| R1-R4   | M09 affected 检查  | `pnpm check:affected -- --base=origin/main --head=HEAD`                                                                                            | BASELINE_BLOCKED | 全仓检查在既有 `packages/mango-pmo/skills/mango-submit-pr/agents/openai.yaml` Prettier 基线诊断处失败，未发现本次文件诊断                                                                   |
| R1-R4   | M09 发布影响       | `pnpm release:impact --base=origin/main --head=HEAD`                                                                                               | RELEASE_BLOCKED  | 策略要求 `@mango/admin-shell` 1.0.62、`@mango/admin` 1.0.68 版本号变更；本任务未获发布/版本升级授权，已提供 changeset                                                                       |
| R1-R3   | M13 浏览器回归     | Node 22.23.1、真实 Bootstrap + runtime 后端、MySQL 隔离库和 Mango Admin 下，Chromium 定向运行 `config-management.spec.ts` 的平台管理员参数维护用例 | PASS             | 独立运行 3 次通过，并以 `--repeat-each=5` 连续通过；最终候选工作树再次通过 1 次。取消后 0 DELETE、记录仍在、无系统错误和 console/pageerror；确认后仅 1 DELETE 且记录删除；残留测试参数 0 条 |
| R1-R4   | STANDARD 记录检查  | `node mango-pmo/tools/check-standard-delivery-record.mjs <本文件>`                                                                                 | PASS             | 结构和风险字段检查通过                                                                                                                                                                      |

## 7. 例外与剩余风险

- 发布和业务项目依赖升级不在本任务授权范围；源码修复完成后仍需发布新的 `@mango/admin-shell` patch 版本并由消费项目升级。
- 全仓 affected 门禁受既有 Prettier 基线阻断，不代表本次代码失败：包级测试、构建、定向静态检查、`git diff --check` 和真实浏览器回归已通过；发布前需先完成版本号发布流程并修复或豁免既有 Prettier 基线诊断。
- 直接执行 Admin Shell `vue-tsc` 仍会报告仓库既有类型错误（如菜单模型、Pinia 持久化和 `vue-i18n` 声明），本次新增错误边界文件未引入新的定向 ESLint 错误。
- 当前本机 `mango` CLI 未实现仓库 `mango.dev.json` 已声明的 `mangoLifecycle/processMode` 自动编排；本次验收显式完成 `bootstrap apply --strategy=cold` 后以同一 release tuple 启动 `runtime`。该兼容性缺口不影响本次 E2E 结果，但后续重新执行 `mango dev start` 前仍需升级 CLI 或显式提供 lifecycle 参数。
- 同文件全量运行时，Issue #840 目标用例通过；既有“A 公司权限隔离”用例在登录前置失败，因为本次隔离库只包含“芒果集团”，`/auth/login` 返回 `1403`“机构不存在、已停用或当前账号未加入该机构”。该失败未进入本次错误边界或确认取消逻辑，不改变目标用例结论；恢复 A 公司 fixture 后才能复核该权限隔离用例。
