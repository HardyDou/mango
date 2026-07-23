# Issue #348 模块运行态诊断实施计划

## 1. 目标

按 [技术设计](../designs/2026-07-22-issue-348-module-runtime-doctor-design.md) 完成 `mango-link` 的 loopback 单体运行态诊断闭环。阶段 A 只形成默认关闭的后端基础；阶段 B 完成真实浏览器和 CLI 纵向验收后，才能认为 Issue #348 已完成。

## 2. 范围

### 阶段 A：后端诊断基础与安全

- Module API/Core/Starter 诊断契约、profile、聚合、安装与实际版本。
- mango-link 显式 persistence 关联键。
- Flyway 运行态状态 registry 与 contributor。
- Resource 当前声明 fingerprint 状态与 contributor。
- Authorization menu/API 物化 contributor 与 page requirements。
- 默认关闭 Actuator endpoint 与真实 Auth security chain 专用授权。
- 后端单元、集成、API、安全和兼容回归。

### 阶段 B：前端探针、CLI 与纵向验收

- Admin Pages 不可变快照、可选版本 metadata 与定向 probe。
- Admin Shell 默认关闭诊断页和 loopback challenge/result 协议。
- CLI `mango module doctor`、固定 loopback URL、后端请求、临时 callback 与项目 Playwright 解析。
- JSON schema、文本输出与退出码。
- mango-link 真实浏览器事故回归和独立 tarball consumer。

## 3. 不处理

- Issue #346 desired-state manifest/generator/lock。
- 远程管理面、Nacos、全服务/全实例判断。
- current SubjectReady、任意租户/角色/成员诊断。
- 自动修复、永久历史、管理大盘和 readiness 接入。

## 4. 实施任务

| ID | 任务 | 主要路径 | 完成标准 |
|---|---|---|---|
| TASK-001 | 新增纯 Java 诊断契约与聚合 | `mango-infra-module-api/core` | profile 缺项不 READY，状态优先级和 stale 语义测试通过 |
| TASK-002 | 安装元数据与实际版本 | `mango-infra-module-starter` | module.properties origin 能关联版本；ModuleInfo 不变 |
| TASK-003 | Flyway 运行态 registry | `mango-infra-persistence-starter` | 真实 Flyway 成功/失败/停用状态可读，原异常传播不变 |
| TASK-004 | Resource 当前证据 | `mango-resource-*` | 本次 fingerprint 与 registry/handler 一致才 PASS，历史成功不能假绿 |
| TASK-005 | Authorization 物化证据 | `mango-authorization-*` | 当前声明的 menu/API 缺失可定位，输出安全 page requirements |
| TASK-006 | 安全 endpoint | `mango-infra-module-starter`、`mango-auth-starter`、`mango-authorization-starter` | 默认关闭；真实 chain 401/403/200；query/cookie/internal/IP/permit 无绕过 |
| TASK-007 | Admin Pages probe | `mango-ui/packages/admin-pages` | 快照不可变；指定 key 的 loader/Vue/chunk 正反例通过 |
| TASK-008 | Shell bridge | `mango-ui/apps/mango-admin-shell`、`packages/admin-shell` | 默认关闭；nonce/origin/Host/body/单次/超时约束通过 |
| TASK-009 | CLI 编排 | `mango-ui/packages/mango-cli` | loopback-only、无重定向、JSON 单对象、exit 0/1/2/3 契约通过 |
| TASK-010 | 纵向回归与文档 | E2E、README、能力地图、evidence | 正常与历史事故场景完成真实入口验证 |

## 5. 测试用例映射

| 用例 ID | 优先级 | 场景 | 层级 | 自动化 | 预期 |
|---|---|---|---|---|---|
| TC-348-001 | P0 | 五段证据完整 | 入口流程/UI-E2E | AUTO | READY，CLI exit 0 |
| TC-348-002 | P0 | required contributor 缺失 | 单元/装配 | AUTO | UNKNOWN/MISSING_CONTRIBUTOR，exit 3 |
| TC-348-003 | P0 | Resource 成功但 Auth menu/API 缺失 | 集成/入口流程 | AUTO | FAILED，exit 1 |
| TC-348-004 | P0 | page key 未注册 | 组件/UI-E2E | AUTO | FAIL/PAGE_NOT_REGISTERED |
| TC-348-005 | P0 | loader reject 或 chunk 404 | 组件/UI-E2E | AUTO | FAIL，exit 1 |
| TC-348-006 | P0 | 诊断 endpoint 鉴权 | API/集成 | AUTO | 匿名 401、普通用户 403、专权用户 200 |
| TC-348-007 | P0 | query/cookie/internal/IP/permit 绕过 | API/集成 | AUTO | 全部拒绝 |
| TC-348-008 | P1 | stale PASS 刷新失败 | 单元/集成 | AUTO | UNKNOWN，不能沿用 READY |
| TC-348-009 | P1 | 32 并发同 key | 集成 | AUTO | single-flight 一次，有界队列/连接占用 |
| TC-348-010 | P1 | CLI auth/network/protocol/overload | CLI 契约 | AUTO | exit 3，stdout JSON 不污染 |
| TC-348-011 | P1 | CLI packed 独立 consumer 无 Playwright/Chromium | 消费者回归 | AUTO | 明确 UNKNOWN/安装提示，不下载 |
| TC-348-012 | P1 | 旧 ModuleInfo、旧 doctor、readiness | 兼容回归 | AUTO | 行为不变 |

## 6. 验证命令

按实际修改模块使用定向 Maven `verify`，不以 partial reactor 改写全局债务预算；依赖构建和消费者编译单独验证。前端至少执行 admin-pages/Admin Shell/CLI 测试与构建、真实 Chromium E2E、packed consumer，以及：

```bash
pnpm admin:styles:check
pnpm admin:module-styles:check
```

公开能力说明变化后执行模块 README 与 source-fact 审计。正式页面验收使用 acceptance evidence checker。

## 7. 完成定义

- 正常 mango-link 五段证据真实成立，CLI exit 0，显示 backend/frontend actual version。
- TC-348-002 至 TC-348-011 不出现错误 READY。
- endpoint 与 bridge 默认关闭，doctor 失败不影响 readiness。
- `ModuleInfo`、旧 doctor 命令和既有项目无配置启动保持兼容。
- README、CLI 帮助和能力地图明确首版只支持默认同端口 Actuator、空或根 context path `/`、loopback Admin Shell 和 mango-link。
- 所有实际执行的检查、失败、阻塞和未验证项记录到交付证据；未跑真实浏览器不得声明 Issue #348 完成。

## 8. 回滚

关闭诊断 endpoint bean/exposure 与 Shell runtime flag；CLI 是新增子命令，不影响旧入口。首版不新增数据库 migration，无需数据回滚。

## 9. 实施复盘

- `ModuleDiagnosticCoordinator` 首轮并发验证发现 single-flight 存在 TOCTOU 窗口：等待者可能在旧请求移除后再次启动执行。实现改为 `putIfAbsent` 取得执行权、执行前二次检查短缓存，并在唤醒等待者前写入成功结果；同 key 并发验证稳定为一次执行。
- 严格 CSP 的真实 Chromium 验证发现完整登录 UI 会引入与诊断无关的请求和运行时表达式噪声。Shell 在显式开启诊断且存在有效 challenge 时采用诊断专用 bootstrap，只执行 feature registrar 和 bridge，不挂载登录 UI；严格 CSP 由诊断 E2E 的 loopback HTTP 响应施加，通用模板不固化会阻断远程 runtime entry 的源白名单。
- Playwright 在 CLI 主动销毁浏览器时会把已经完成的本地 callback 连接报告为 `ERR_ABORTED`。callback 结果已有 nonce、Host、Origin、body 和 schema 的独立强校验，并额外等待页面 bridge 状态进入 `COMPLETE`；因此通用资源失败监听排除该权威回执 URL，其他页面请求、chunk、console error 和 page error 继续零容忍。
- 最终验收补充执行完整 `mango-monolith-app`、独立 MySQL 8.4.8、真实 admin token、Admin Shell 和源码 CLI 纵向链路；五个 required condition 全部 PASS，CLI 返回 READY/exit 0。HTTP fixture 仍只承担协议负例，不替代真实应用证据；本机验收不表述为生产实例证明。
