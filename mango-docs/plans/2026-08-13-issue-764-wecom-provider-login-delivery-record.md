# 标准交付记录

任务：Issue #764 企业微信登录新旧授权流程混用修复。

## 1. 元数据

- 任务 ID：Mango Issue #764
- 交付模式：STANDARD
- 需求影响：L2 - 企业微信扫码登录主流程被阻断，且旧手工 code 入口可被业务自定义登录页继续使用
- 方案风险：L2 - 删除 `@mango/auth` 已公开的旧企微前端 API，需要同步业务自定义登录页到统一 Provider API
- 最终风险：L2
- 工作区决策：REUSE（`/Users/hardy/Work/mango-fix-764-wecom-provider-login`，`fix/764-wecom-provider-login`）
- 启用能力：M01、M08、M09

## 2. 目标与范围

- 目标：让管理端企业微信登录只使用后端签发一次性 state 的统一 Provider 授权链路。
- 成功条件：`@mango/auth` 不再提供旧企微配置读取、前端 state 拼接、`mwc.` state、手工 code 登录或旧回调解析入口；默认登录页和二维码弹窗使用 `/auth/providers/authorize`，公开回调页继续使用 `/auth/providers/complete`。
- 处理范围：`@mango/auth` 登录 composable、旧企微 API 导出、声明文件、登录初始化和能力说明。
- 不处理范围：后端 `/auth/wecom/*` 兼容接口、Provider 配置数据、真实企业微信扫码验收、npm 发布。

## 3. 可观察系统要求

| ID      | 参与者或入口             | 输入或前置条件                   | 预期行为                                                                          | 失败语义                                                      | 验收标准                                         |
| ------- | ------------------------ | -------------------------------- | --------------------------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------ |
| REQ-001 | 默认或自定义管理端登录页 | 用户选择租户并使用企业微信登录   | 调用 `/auth/providers/authorize`，跳转或以二维码展示后端返回的 `authorizationUrl` | 请求 `/auth/wecom/login-config` 或前端自行生成 state 视为失败 | Auth 源码无旧入口，Provider API 保留新版请求     |
| REQ-002 | 企业微信回调             | 厂商把 `code/state` 返回前端入口 | Hash Router 转入 `/provider-callback`，回调页调用 `/auth/providers/complete`      | 手工输入 code 或调用 `/auth/wecom/login` 视为失败             | 回调页及 Router 捕获能力保留，旧回调解析代码删除 |
| REQ-003 | 业务自定义登录页         | 升级后的 `@mango/auth` 公共 API  | 使用 Provider API，不再获得旧二维码和手工 code 登录 API                           | 旧 API 仍被包入口导出视为失败                                 | 类型声明、源码导出和 README 同步收敛             |

## 4. 技术决定

| ID      | 对应要求         | 接口/数据/权限/兼容性决定                                                                                                                               | 影响路径                                                            | 回滚方式                                       |
| ------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ---------------------------------------------- |
| DEC-001 | REQ-001、REQ-003 | `prepareWecomLogin()` 改为请求统一 Provider 授权地址，保留只读 `wecomQrUrl`；删除手工 code、旧回调状态和 `api/sys.ts` 旧企微封装，不保留运行时 fallback | `packages/auth/src/composables`、`api/sys.ts`、`index.d.ts`         | 恢复被删前端 API，但会重新引入新旧链路混用风险 |
| DEC-002 | REQ-001、REQ-002 | 默认登录页初始化只加载租户和 Provider 摘要；Provider 授权及公开回调维持现有新版实现                                                                     | `views/login.vue`、`api/provider.ts`、`views/provider-callback.vue` | 恢复旧初始化会重新消费 `mwc.` state            |
| DEC-003 | REQ-003          | README 和能力地图声明自定义登录页迁移方式；后端兼容接口保持不变                                                                                         | Auth README、Views README、能力地图                                 | 回滚文档无法改变运行时，需与代码同步回滚       |

## 5. 实施清单

| ID      | 对应决定 | 顺序 | 改动路径                                           | 完成条件                                                                 |
| ------- | -------- | ---: | -------------------------------------------------- | ------------------------------------------------------------------------ |
| IMP-001 | DEC-001  |    1 | `useMangoLoginFlow.ts`、`api/sys.ts`、`index.d.ts` | 二维码使用后端授权地址，手工 code、旧企微前端 API 和 `mwc.` 逻辑全部删除 |
| IMP-002 | DEC-002  |    2 | `views/login.vue`                                  | 默认登录页不再执行旧回调初始化                                           |
| IMP-003 | DEC-003  |    3 | Auth README、Views README、能力地图                | 新版接入方式和旧入口退出边界可发现                                       |
| IMP-004 | 全部     |    4 | 静态扫描、包构建、边界与文档检查                   | 启用检查通过或如实记录限制                                               |

## 6. 验收映射与结果

| 要求 ID                   | 验证方式               | 命令或步骤                                                                                                    | 结果   | 证据                |
| ------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------- | ------ | ------------------- |
| REQ-001、REQ-002、REQ-003 | M09 旧入口扫描         | Auth 源码和构建产物扫描 `/auth/wecom/*`、`mwc.`、手工 code 方法，并确认 `/auth/providers/authorize`、`/auth/providers/complete` 保留 | PASS | `rg` 输出；dist/index.js 与声明文件 |
| REQ-001、REQ-002、REQ-003 | M09 包构建             | `pnpm --filter @mango/auth build`                                                                                | PASS | Vite 构建与类型生成输出 |
| REQ-001、REQ-002、REQ-003 | M09 前端边界与文档检查 | `pnpm frontend-boundaries:check`、`pnpm architecture:check`、`pnpm admin:styles:check`、`pnpm admin:module-styles:check`、README 审计、标准记录检查、`git diff --check` | PASS | 对应命令输出 |
| REQ-001、REQ-002、REQ-003 | M09 文档站构建         | `cd mango-docs && npm ci --ignore-scripts --no-audit --no-fund && npm run docs:build`                           | PASS | VitePress 构建完成；仅有既有 chunk size warning |

## 7. 例外与剩余风险

- 按本任务选用的 `simple` skill 指令跳过测试；以静态扫描、类型生成、包构建和边界检查作为本次最低验证，不声明真实企业微信扫码已验收。
- 删除旧公开前端 API 会使仍使用手工 code 弹窗的业务自定义登录页在升级时编译失败；业务项目需改用 `startProviderAuthorization()`，这是阻止运行时继续进入旧链路的预期迁移行为。
- 本任务不执行 npm 发布；业务环境需要在包含本修复的 `@mango/auth` 新版本发布并升级后生效。
