# Issue #825 通知渠道配置安全回显交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #825
- 交付模式：STANDARD
- 需求影响：L2 - 通知渠道配置编辑、Secret 存储与按需查看能力变化
- 方案风险：L2 - 新增受权限保护的单字段明文返回接口和存量明文惰性迁移
- 最终风险：L2
- 工作区决策：REUSE（复用既有任务工作树，提交范围独立隔离为 Issue #825）

## 2. 目标与范围

- 目标：渠道配置编辑时完整回显非敏感字段；已配置 Secret 默认显示 `****`，用户点击该字段的小眼睛后才通过受控接口查看明文。
- 成功条件：人工维护的 Secret 加密落库；列表和普通详情不返回明文；单字段查看具备独立权限、租户隔离、审计与 `no-store`；关闭、切换或再次隐藏后前端清除明文；存量明文在当前渠道被读取或保存时迁移为密文。
- 处理范围：`mango-notice-api/core/starter/starter-remote`、通知渠道前端页面与 API、通知权限资源、Notice 能力文档和定向测试。
- 不处理范围：Secret 引用资源的跨系统解密、通用密钥管理平台改造、历史全库离线迁移、通知渠道之外的凭据页面、发布与部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | 管理员编辑渠道 | 渠道已保存非敏感配置 | 表单按保存值完整回显 Webhook、登录开关、回调地址等非敏感字段 | 配置 JSON 非法时拒绝保存或以明确错误响应，不静默伪造值 | 编辑页字段与列表接口返回的 `configJson` 一致 |
| R2 | 管理员编辑人工渠道 | 某 Secret 已配置 | 输入框默认显示 `****`；未修改直接保存时保留原 Secret | 禁止把 `****` 当作 Secret 提交或覆盖 | 列表不含明文，保存后原 Secret 仍可使用 |
| R3 | 具备独立权限的管理员 | 点击单个 Secret 字段的小眼睛 | 仅请求并展示该字段明文，再次点击、关闭/切换渠道或超时后清除明文 | 无权限、跨租户、非法字段、解密失败均不返回明文，页面保持掩码 | 返回单字段；响应 `Cache-Control: no-store`；前端无持久化 |
| R4 | 系统保存或运行人工渠道 | 新增/更新 Secret 或遇到存量明文 | 新值以 `enc:` 密文存储；存量明文在当前渠道读取/保存路径惰性迁移 | 加密能力缺失或密文损坏时失败关闭，不降级明文写入 | 数据库中不出现新提交的 Secret 明文 |
| R5 | 审计人员 | 成功或失败尝试查看 Secret | 记录操作人、租户、渠道、字段和结果等安全元数据 | 审计快照不得包含 Secret 明文或密文 | 可按渠道定位查看行为，审计记录无敏感值 |
| R6 | Resource 管理渠道的管理员 | Secret 来自 Resource/环境引用 | 显示已配置/由引用管理状态，不把引用解析值暴露到页面 | 查看接口返回不可查看的业务错误 | 页面不把引用误报为空，也不出现明文 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R2-R4 | `secret_config_json` 保持 JSON 结构，但人工 Secret 的每个值通过 Mango `ICryptoService` 编码为带 `enc:` 前缀的密文；运行时按字段解密 | `mango-notice-core` | 回滚代码前先完成密文数据兼容，禁止直接回滚到只读明文实现 |
| D2 | R3-R6 | 新增 `GET /notice/channels/secret?channelConfigId=&secretKey=`，仅允许渠道定义中的 Secret key；返回单字段值 | `mango-notice-api`、`starter`、`starter-remote`、前端 API | 删除前端入口并停用接口；保留密文存储能力 |
| D3 | R3 | 接口使用独立权限 `notice:channel:secret:view`，查询依赖租户拦截后的实体；响应禁止缓存 | Controller、安全资源、HTTP 过滤/响应处理 | 撤销权限分配并停用接口 |
| D4 | R5 | 使用现有 `notice_audit_log` 表写入 Secret 查看审计；快照只记录字段、来源和结果，不记录值 | Notice entity/mapper/service | 停用查看接口，不删除既有审计数据 |
| D5 | R1-R3 | 前端根据 `configuredSecretKeys`/缺失状态构造掩码，不把掩码写入提交；每个字段独立按需请求，明文只保留在组件内存并定时清理 | `mango-ui/packages/notice` | 隐藏查看入口，恢复只写 Secret 交互 |
| D6 | R4 | 存量无 `enc:` 前缀值仅作为兼容输入，在该渠道保存、运行物化或成功查看时按当前租户/渠道范围写回密文；完成存量迁移后移除兼容分支 | `mango-notice-core`、能力文档 | 若写回失败则事务回滚并拒绝继续，不扩大到跨租户批处理 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1、D6 | 1 | `mango-notice-core` 依赖、codec、配置服务、物化器 | 新值加密，存量值有限惰性迁移，运行时可解密 |
| I2 | D2-D4 | 2 | API DTO/VO、服务接口、Controller/Feign、审计实体 Mapper、权限 JSON | 单字段接口具备校验、权限、租户、审计和禁缓存 |
| I3 | D5 | 3 | `mango-ui/packages/notice/src/api`、`types`、渠道页面 | 完整回显、默认掩码、小眼睛查看/隐藏、生命周期清理 |
| I4 | D1-D6 | 4 | 后端/前端测试与 Notice README | 关键安全和兼容场景有自动化证据，文档同步 |
| I5 | D1-D6 | 5 | 本记录与能力索引 | 定向验证结果和剩余风险可追踪 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1 | 后端转换与持久化集成测试、前端构建和组件测试 | `mvn -pl mango-platform/mango-notice/mango-notice-core test`；`pnpm --filter @mango/notice... build`；`pnpm --filter @mango/notice test` | PASS | 非敏感 Webhook、AccessKey ID、SecretId、AppKey、登录开关与回调地址保持原值；历史 Secret 别名映射为页面规范字段；Notice 构建通过，12 个测试文件、50 项测试通过 |
| R2、R4 | 真实 Mapper/H2/SM4 集成测试与全量模块 verify | 上游依赖 `-am -DskipTests install` 后，对 `mango-notice-core`、`mango-notice-starter`、`mango-notice-starter-remote` 执行不带 `-am/-amd` 的 direct-module `verify` | PASS | core 70、starter 26、starter-remote 4，共 100 项测试通过；关键集成链路使用 Mango `Sm4CryptoService`，覆盖新值密文落库、`***`/`****` 均不覆盖原值、未修改保存、运行物化、存量明文与别名惰性迁移、加密能力缺失和损坏密文失败关闭 |
| R3、R5 | API/HTTP/Feign 契约、权限与禁缓存测试，租户和审计集成测试 | 同上；`NoticeApiSurfaceContractTest`、`NoticeControllerAccessModeTest`、`NoticeFeignContractTest`、`NoticeChannelSecretNoStoreFilterTest`、`NoticeChannelSecretIntegrationTest` | PASS | 仅单字段返回；独立权限 `notice:channel:secret:view`；跨租户、非法字段和引用字段失败关闭；成功/失败审计不含明文或密文；响应为 `Cache-Control: no-store, max-age=0` 和 `Pragma: no-cache` |
| R6 | Resource 引用集成与组件测试 | `NoticeChannelSecretIntegrationTest`；`NoticeSecretInput.spec.ts` | PASS | 引用管理字段显示 `****` 和只读状态，不调用明文接口；后端拒绝把引用解析到页面 |
| R1-R6 | 前端生命周期与并发回归测试 | `pnpm --filter @mango/notice test` | PASS | 覆盖默认掩码、单字段查看/再次隐藏、替换时不提交掩码、失败保持掩码、关闭/切换/60 秒超时清理、丢弃切换渠道后的迟到响应 |
| R1-R6 | PMO 与静态质量门禁 | `test-quality-check.mjs`、backend mock audit、README/source facts/capability docs audit、changed-file static gate、Notice package exports、frontend quality/release tests、Checkstyle、SpotBugs | PASS WITH BASELINE | 测试质量 14 个文件通过；mock audit 0 block/2 warn（Materializer 单测仅 mock Mapper，不作为持久化证据，真实 Mapper 由集成测试覆盖）；README、源码事实与能力文档通过；Issue #825 前端文件 ESLint/Prettier/Stylelint changed-file gate 通过；Checkstyle 0 violation；本次新增 SpotBugs 告警已清零 |

## 7. 例外与剩余风险

- 存量明文不做跨租户全库自动扫描，仅在当前渠道实际保存、运行或查看时惰性迁移，避免扩大权限与事务范围。
- Resource/环境引用只回显“由引用管理”的配置状态，不通过本接口解析并暴露第三方 Secret。
- 生产运行必须提供有效的 `mango.crypto.sm4.secret-key`；缺失或无效时密码能力按基础设施约定启动/调用失败关闭。
- 已使用独立数据库启动真实后端与管理端，后端 Actuator 为 `UP`、管理端与代理健康接口均返回 HTTP 200；尚未完成浏览器内点击小眼睛及 Network `no-store` 的人工走查。当前交互另由 Notice 依赖闭包构建、50 项前端测试和后端真实 Mapper/H2 集成测试覆盖，上线前仍需在带有效 SM4 密钥和权限资源初始化的环境复核该人工场景。
- Notice 单包 `vue-tsc` 仍被存量 Realtime 导出缺失和既有隐式 `any` 阻断，错误未指向本次 Secret 组件；依赖闭包 Vite 构建及 Issue #825 changed-file 静态门禁已通过。
- 无关 Issue 改动已从当前工作目录和提交范围隔离，最终前端门禁使用纯 #825 HEAD 执行，不以其它任务的诊断作为例外。
- `mvn mango:check` 当前扫描到 808 个仓库存量架构/数据库基线问题后失败；PMD 6.42 对 Java 21 源码产生全模块 processing error，且仓库配置为不阻断。独立 Checkstyle、SpotBugs 和 `verify` 已执行；SpotBugs 剩余 6 项均位于未修改的存量类。
- 标准 Changesets CLI 会把仓库保留的 `.changeset/release-notes-template.md` 当作 changeset 并报 frontmatter 错误；本次 `quiet-ravens-notice-825.md` 自身 frontmatter 合法，Notice package export 与仓库 release tests 通过。
- 前端最终提交门禁使用仓库声明范围内的 Node.js 22.23.1 与 pnpm 11.14.0。
