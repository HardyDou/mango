# Issue #348 模块运行态诊断设计

## 1. 摘要

Issue #348 要解决的不是“资源同步是否成功”，而是一个模块在当前运行实例中是否具备可用所需的完整证据。首版以 `mango-link`、单 JVM、单 Admin Shell、loopback 本地诊断为验收边界，串联安装与实际版本、Flyway、Resource、Authorization 和浏览器页面运行五段证据。

本设计不合并 Issue #346。#346 负责构建期 desired state、生成器和 lock；#348 只记录 observed state。没有独立期望提供者时，期望模块与期望版本必须为 `UNKNOWN`。

## 2. 背景与问题

当前 Resource `SUCCESS` 不能证明模块可用。mango-link 事故仍需要人工交叉检查 starter/classpath、Flyway、Resource registry/log、Authorization 菜单与 API、前端 page key、动态 loader 和浏览器路由。

现有能力存在以下缺口：

- `ModuleInfo` 只描述路由元数据，不描述安装、版本或运行状态。
- Flyway 按模块迁移，但迁移完成后没有只读运行态快照。
- Resource 历史同步日志不能证明当前声明集合已经物化。
- Authorization 没有把当前 Resource 声明与菜单/API 物化结果形成模块级诊断。
- `@mango/admin-pages` 不能枚举只读注册快照，也不能定向验证 loader/chunk。
- CLI 的既有 doctor 只诊断工作区，不具备模块运行态聚合能力。

## 3. 目标与非目标

### 3.1 目标

- 提供稳定、可扩展、只读的模块诊断契约。
- 对 `mango-link` 返回五段运行态证据和保守聚合状态。
- 分开显示后端和前端实际版本；无权威来源时返回 `UNKNOWN`。
- 任何超时、权限不足、缺 contributor、旧缓存或协议错误都不能得到 `READY`。
- 新增 `mango module doctor`，保持旧 doctor 命令不变。
- endpoint 与浏览器诊断入口默认关闭，不影响启动和 Kubernetes readiness。

### 3.2 非目标

- 不实现 `mango-module.yaml`、`mango-app.yaml`、generator 或 lock。
- 不扫描 Nacos、全部服务或全部实例。
- 不支持远程生产管理面；首版 URL 仅接受 `127.0.0.1` 与 `[::1]`。
- 不做自动 Flyway migrate/repair、Resource resync 或权限修复。
- 不查询任意 tenant、subject、role 或成员明细。
- 不建设永久历史库、管理大盘或时序监控。
- 不改变 liveness、readiness 或现有 `ModuleInfo`。

## 4. 风险与交付模式

- 需求影响：L3。能力跨 Module、Persistence、Resource、Authorization、Auth、Admin Pages、Admin Shell 和 CLI，错误 READY 会直接误导发布与联调判断。
- 方案风险：L3。涉及安全链、数据库观察、异步超时、缓存、浏览器运行态与公开契约。
- 最终风险：L3。
- 交付模式：FULL。
- 工作区：`feat/issue-348-module-doctor` / `/Users/hardy/Work/mango-issue-348`。
- 启用能力：M01、M08、M09、M10、M11、M12、M13、M14；不执行数据库重建和外部发布。

## 5. 专家讨论与独立评审

方案由 13 个视角收敛：平台模块架构、Spring Boot/Actuator、Flyway、Resource、Authorization、前端运行时、CLI/DX、分布式拓扑、供应链版本、SRE、应用安全、QA 和平台产品/兼容治理。

随后由三位独立评审人审查：

- 架构评审：补齐诊断 profile、缺 contributor 合成、权威 owner 范围和浏览器回传闭环后 ACCEPT。
- 安全运维评审：收缩为 loopback-only、把专用权限接入现有最高优先级安全链并补齐 fail-closed 后 ACCEPT。
- 实施与兼容评审：补齐模块关联键、Playwright 独立消费者契约和 Resource 当前 fingerprint 后 ACCEPT。

三位评审最终均无 blocker。

## 6. 总体架构

```text
mango module doctor
  |-- GET 127.0.0.1:<backend>/actuator/mangoModules
  |     |-- installation/version
  |     |-- persistence.flyway
  |     |-- resource.materialization
  |     `-- authorization.menuApi + pageRequirements
  `-- isolated Playwright -> Admin Shell diagnostic page
        `-- @mango/admin-pages targeted probe
              registrar -> page key -> loader -> Vue component/chunk
```

每个领域只报告自己拥有的事实。Module 聚合层不直连业务表，不解析 contributor 的自然语言，也不让 Resource 充当跨域 fan-out。

## 7. 诊断契约

### 7.1 状态

Condition 状态：

- `PASS`：当前权威证据明确通过。
- `WARN`：能力可用但有非阻断风险。
- `FAIL`：当前证据明确证明条件不成立。
- `UNKNOWN`：不可观察、超时、权限不足、协议错误、缺 contributor 或缺期望。
- `SKIPPED`：只有权威证据表明不适用或明确关闭。

模块聚合状态：

- `FAILED`：任一 required condition 为 FAIL。
- `UNKNOWN`：无 FAIL，但 required condition 为 UNKNOWN 或 stale。
- `DEGRADED`：required 均通过，optional condition 为 FAIL/WARN。
- `READY`：全部适用 required condition 为 PASS/SKIPPED，且没有 optional FAIL/WARN。
- `DISABLED`：模块被明确配置为停用；用户显式请求时不视为成功。

阻断不能用 `SKIPPED` 表达；`BLOCKED_BY_*` 必须是 `UNKNOWN`。optional UNKNOWN 不改变聚合状态，但设置 `incompleteOptional=true`。

### 7.2 请求级 profile

首版完整 profile 为 `ADMIN_MODULE_RUNTIME_V1`，固定 required condition IDs：

- `installation`
- `persistence.flyway`
- `resource.materialization`
- `authorization.menuApi`
- `frontend.pageRuntime`

执行前对照 profile。缺 contributor 或缺 condition 时合成 `UNKNOWN/MISSING_CONTRIBUTOR`。profile 是单次诊断检查清单，不是 #346 的持久化模块清单。

### 7.3 快照字段

快照至少包含：

- `schemaVersion=1`
- `profile`
- `reportScope=AUTHORITATIVE_OWNER|INSTANCE_OBSERVATION`
- `service`
- `instanceId`
- `observedAt`
- `modules[]`
- `conditions[]`
- `reasonCode`
- 白名单 evidence
- `durationMs`
- `stale`

版本按运行面分列：backend actual、frontend actual、expected。读取不到 artifact 或 registrar 版本时 actual 为 UNKNOWN；没有 #346 provider 时 expected 为 UNKNOWN，但不因此降低实际运行状态。

## 8. 模块关联键

不能用删除 `mango-` 前缀推断领域键。`mango-link` 首版在 `META-INF/mango/module.properties` 增加可选运行态诊断元数据：

```properties
diagnostic.persistence-module=link
```

新诊断 metadata 独立于 `ModuleInfo`。映射缺失且无法唯一确认时返回 `UNKNOWN/MAPPING_UNRESOLVED`。

Resource 当前 `AUTH_MENU` / `API_RESOURCE` 声明派生稳定的 `pageRequirements`，只含 module code 与 component page key 等探针必需信息；禁止输出 API 路径、handler、租户或主体明细。

## 9. 后端实现

### 9.1 Module API/Core/Starter

- API 放纯 Java 不可变契约和 SPI，不依赖 Spring/Jackson/Actuator。
- Core 负责稳定排序、异常隔离、profile 补缺和聚合。
- Starter 从 `module.properties` 的原始资源 URL 关联 artifact，优先读取对应 JAR `META-INF/maven/**/pom.properties`，Manifest 作为 fallback。
- 保留原 `ModuleInfo` 构造器、字段和解析行为。
- Actuator endpoint ID 为 `mangoModules`，bean enable 与 exposure 两个开关默认都关闭。
- 不新增 `HealthIndicator`，诊断失败不改变 readiness。

### 9.2 Persistence

在现有模块迁移流程中维护线程安全只读 registry：

- 迁移前记录 RUNNING。
- migrate 后从本次 Flyway 的 `MigrationResult` / `info()` 记录 APPLIED、current version、pending count、history table 和耗时。
- 失败记录 FAILED 后继续原异常传播。
- 全局或模块明确关闭记录 DISABLED。
- 不从 doctor 触发 migrate、repair 或 validate。

若 Flyway 导致应用启动失败，HTTP endpoint 不可用；保留结构化启动日志作为边界证据，CLI 只能报告 transport UNKNOWN。

### 9.3 Resource

- 每次同步开始计算模块级当前声明 fingerprint，并先将进程内状态置为 RUNNING/UNKNOWN。
- 只有本 JVM 本次 `doSync` 成功后才记录该 fingerprint 成功。
- PASS 还要求 registry row 的 sourceHash/status/targetId/targetTable 与当前声明逐项一致，并且 handler/dispatcher 可解析。
- 历史 SUCCESS 只作 WARN evidence，不能使 condition PASS。
- 未取得锁、同步异常、旧 PASS 刷新失败为 UNKNOWN 或 FAIL，不沿用旧 PASS。
- 首版不新增数据库 batchId/snapshotId migration。

### 9.4 Authorization

Authorization contributor 只验证当前 Resource 声明可推导的 menu/API 物化不变量，返回计数、缺失数、reason code 与 page requirements。无权威期望的套餐、角色、租户和 current subject 完整性返回 UNKNOWN/SKIPPED，不进入共享缓存。

### 9.5 安全 endpoint

首版只支持默认同端口 `GET /actuator/mangoModules`。Auth 现有 security chain 在 permit path、internal call、IP whitelist 和通用 API Resource 授权之前精确匹配该路径：

- 只接受 `Authorization: Bearer`。
- query token、`MANGO_TOKEN` cookie、非 GET 和 malformed bearer 均拒绝。
- 调用独立诊断授权 provider 检查 `diagnostic:read`；provider 缺失、异常、主体不完整或权限不足均 deny。
- endpoint auto-configuration 仅在固定名称授权 bean 存在时创建，security chain 在 bean 意外缺失时仍 deny。
- 授权不依赖被诊断的 `authorization_api_resource` 登记。
- 自定义 management base path/context path/port 首版不支持。

## 10. 执行、缓存和审计

- contributor 接收不可变显式 scope，不在线程中读取 `SecurityContextHolder` 或 `MangoContextHolder`。
- 首版只缓存 JVM/app 全局 ModuleReady；不缓存 subject/tenant 结果。
- contributor 预算约 2 秒，总预算约 5 秒；使用隔离小执行器和有界队列。缓存命中不占调用许可；cache miss 调用者与存活 flight 分别受 `maxKeys` 公平信号量限制，超限立即 UNKNOWN/OVERLOADED。缓存使用短锁保护的 access-order 表，写入后同步淘汰，任何并发时刻容量都不超过 `maxKeys`。
- 数据库检查设置 query timeout，禁止 N+1。
- 过期 PASS 刷新失败必须降为 UNKNOWN；过载为 UNKNOWN/OVERLOADED 或 HTTP 429。
- 审计记录调用者、app、来源地址、module set 摘要、结果、耗时、拒绝或限流原因、requestId；禁止记录 token、nonce、callback URL、完整 evidence、SQL、路径或响应正文。

## 11. 前端与 CLI

### 11.1 Admin Pages

公开能力包括：

- 不可变 registered-page snapshot。
- 可选 module/package version metadata，旧注册调用保持兼容。
- 只针对明确 module/page key 的 probe：registrar、page key、loader、Vue component、chunk。

内部 loader Map 不对外暴露，probe 不批量加载所有页面。

### 11.2 Admin Shell 诊断入口

诊断页通过 runtime config/env 显式启用，默认关闭。页面只接受 CLI 生成的 challenge：

- nonce 至少 128 bit，短 TTL、单次消费。
- nonce 与 callback challenge 通过 Playwright 控制通道和 URL fragment 注入，不进入日志、截图或持久历史。
- 通用 Shell 保留 `Referrer-Policy: no-referrer`，不固化会阻断远程 runtime entry 的全局 CSP；诊断 E2E 由 loopback HTTP 响应施加严格 CSP，并阻断第三方网络和 Service Worker。
- callback 只允许 loopback IP literal，精确 Host、端口和 Admin Shell origin。
- 只 POST JSON 文本；使用 CORS safelisted 的 `text/plain;charset=UTF-8` 避免 loopback 跨端口预检噪声，服务端仍按 JSON 解析并限制 body、字段与嵌套深度，OPTIONS 不消耗 nonce。
- 浏览器上下文无后端 token、无现有用户目录、无多余环境变量，完成后销毁。

### 11.3 CLI

新增命令：

```bash
mango module doctor mango-link \
  --app internal-admin \
  --backend-url http://127.0.0.1:18081 \
  --frontend-url http://127.0.0.1:30001 \
  [--json] [--strict]
```

首版 URL 只接受规范化后的 `127.0.0.1` 或 `[::1]` base origin；拒绝 localhost、userinfo、path、query、fragment、IPv4-mapped IPv6 和重定向。CLI 自行拼固定 endpoint 与诊断路径。

CLI 从 `--project-dir` 对应前端工程显式解析 `playwright` 或 `@playwright/test`，不从 CLI 安装位置解析，也不自动下载浏览器。包或 Chromium 缺失时 frontend required condition 为 UNKNOWN，退出 3，并给出安装提示。必须用 npm tarball 的独立 consumer 验证这一行为。

stdout 的 JSON 模式只输出一个对象。退出码：

- 0：全部 required condition PASS/SKIPPED，且显式请求模块不是 DISABLED。
- 1：确定性 FAIL；required WARN；`--strict` 下 DEGRADED/incompleteOptional。
- 2：参数或本地配置错误。
- 3：401/403、网络、协议、required UNKNOWN/stale、429/OVERLOADED 或浏览器不可用。

## 12. 兼容性

- `ModuleInfo` 源码和二进制契约不变。
- 现有 `module.properties` 仍有效，诊断映射字段可选。
- 旧模块只能获得基础 installation 事实；完整 profile 缺 contributor 时明确 UNKNOWN。
- `registerModulePages` 只增加可选 metadata，旧调用无需改动。
- 原 `mango doctor`、`mango dev doctor`、`mango workspace doctor` 行为和退出码不变。
- endpoint 和 bridge 默认关闭，原项目无需改配置即可启动。

## 13. 备选方案与取舍

### 13.1 CLI 直连数据库

拒绝。会复制各领域 SQL、扩大凭据与租户边界、无法作为微服务真实实例权威。

### 13.2 Resource 作为全局 fan-out

拒绝。会把 Resource 变成全平台耦合中心并放大级联故障；采用领域自报告、CLI 编排。

### 13.3 使用 HealthIndicator/readiness

拒绝。诊断包含跨域和浏览器观察，不应影响 Pod 流量门控。

### 13.4 静态 manifest 代替浏览器 probe

拒绝作为 PASS 依据。静态文件不能证明 registrar 已执行、chunk 能加载或 loader 返回 Vue component。

### 13.5 首版支持远程 URL

拒绝。远程 SSRF、DNS rebinding、管理面 TLS 与 owner 完整性会显著扩大风险；首版限定 loopback，后续以独立任务设计。

## 14. 故障模式

- contributor 缺失：UNKNOWN/MISSING_CONTRIBUTOR。
- 映射缺失：UNKNOWN/MAPPING_UNRESOLVED。
- Resource 历史成功但当前 fingerprint 不可证明：WARN/UNKNOWN。
- Auth menu/API 缺失：FAIL。
- page key 缺失、loader reject、非 Vue component 或 chunk 404：FAIL。
- endpoint 401/403、网络超时、schema 不兼容：UNKNOWN，CLI 退出 3。
- Flyway 启动失败：HTTP 不可达，CLI UNKNOWN；启动日志保留相同 reason code。
- stale PASS 刷新失败：UNKNOWN。
- required contributor 过载：UNKNOWN/OVERLOADED。

## 15. 验收

正常 mango-link 需要五段证据均成立，CLI 才能退出 0 并显示后端/前端实际版本。必须长期保留以下反例：

- Resource 本次同步成功但 Auth menu/API 被删除。
- 后端要求的 page key 未注册。
- loader 异常或 chunk 404。
- required contributor 缺失。
- query/cookie/internal/IP/permit path 尝试绕过诊断授权。
- endpoint 403、超时、旧缓存、协议错误或浏览器缺失。

上述场景不得出现错误 READY。

## 16. 上线与回滚

先在本机非生产环境以默认关闭的影子诊断运行，不作为 CI 发布门禁。只有完整纵向验收通过后才可选择使用 `--strict`。

回滚通过关闭 endpoint bean/exposure 和 Shell bridge 完成。首版不新增数据库表、不改变 migration 和业务 API，回滚不需要数据修复。
