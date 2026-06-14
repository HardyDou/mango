# 模块 README 专家评审记录

日期：2026-06-13

本记录归档 3 名 Mango 开发人员、3 名架构专家、3 名业务开发人员对 README 能力说明的逐项评审意见和整改结果。它是交付证据，不是规范源。

## 1. 评审范围

- 后端平台能力 README。
- 后端基础设施 README。
- 前端 package README。
- CLI、business starter、topology、business PMO README。
- 能力地图和 README 模板。

## 2. Mango 开发人员评审

### A1：auth / identity / access / authorization / file

| README | 意见 | 整改结果 |
|--------|------|----------|
| `mango-auth` | 描述与代码一致；建议说明单模块 test 不等于完整登录闭环。 | 已补充完整登录闭环需 identity、authorization、captcha/kv 真实依赖数据。 |
| `mango-identity` | 路径、Feign、migration、表名和配置描述基本准确。 | 无需整改。 |
| `mango-access` | Servlet Filter 测试可见，Gateway starter 需要显式集成验收口径。 | 已补充 Gateway `AuthGlobalFilter` 不能由 Servlet 单测代表。 |
| `mango-authorization` | 资源同步、Controller、Feign、migration 描述准确；调试开关需防误用。 | 已补充 `debug-permit-all-filter-chain` 只用于调试，不作为交付验收。 |
| `mango-file` | remote starter 主要覆盖 `/file/files` 的 `FileApi`，不等同远程管理接口。 | 已补充 Feign 入口范围和 storage/settings/directories 管理接口边界。 |

### A2：job / workflow / captcha / persistence / kv

| README | 意见 | 整改结果 |
|--------|------|----------|
| `mango-job` | 模块事实准确；远程 Worker 注册兼容路径和 Feign URL 需说明。 | 已补充 JobCenter / Worker 入口和兼容路径验收口径。 |
| `mango-workflow` | Controller、配置、Flowable/业务表和业务接入边界准确。 | 无需整改。 |
| `mango-captcha` | 当前 store 由注入的 `IKvStore` 决定，`captcha_code` / storage 易被误解。 | 已补充当前最小闭环以 infra-kv `IKvStore` 为准，表属于历史或预留资产。 |
| `mango-infra-persistence` | 配置、多数据源、Flyway、审计、BaseCrudController 和测试入口准确。 | 无需整改。 |
| `mango-infra-kv` | 模块、配置、namespace、capability、Outbox 描述准确；长期验收需要沉淀测试入口。 | 已通过 `mango-infra-test` README 和能力地图补充测试模块入口。 |

### A3：crypto / doc / event / feign / realtime / web / fileproc aspose

| README | 意见 | 整改结果 |
|--------|------|----------|
| `mango-infra-crypto` | 配置和 SM/AES/RSA/HMAC/SHA 描述准确。 | 无需整改。 |
| `mango-infra-doc` | OpenAPI/Knife4j、模块分组和无业务 Controller 描述准确。 | 无需整改。 |
| `mango-infra-event` | outbox 条件装配准确；菜单来自 authorization 历史迁移需说明。 | 已补充历史迁移不是 event 新增菜单资产的长期归属方式。 |
| `mango-infra-feign` | 拦截器、内部调用、token 透传和 timeout caveat 准确。 | 无需整改。 |
| `mango-infra-realtime` | endpoint、Feign 服务名、无 SQL migration、依赖 infra-kv outbox 描述准确。 | 无需整改。 |
| `mango-infra-web` | `@Inner`、上下文、MDC、CORS、内部签名和 nonce 依赖准确。 | 无需整改。 |
| `fileproc aspose` | resource README 定位、配置、license 和验证入口与代码一致。 | 无需整改。 |

### A4：第二轮前端 / CLI / starter 事实核对

| README | 意见 | 整改结果 |
|--------|------|----------|
| `@mango/api-schema` | README 写 `pnpm -F @mango/api-schema build`，但 package.json 没有 `build` script。 | 已改为真实可执行的 Node 断言，校验 package main 和 `ApiId`、`R` 类型入口。 |
| `@mango/app-runtime` | README 写 `pnpm -F @mango/app-runtime test`，但 package.json 没有 `test` script。 | 已改为真实可执行的 Node 断言，校验 exports 和 runtime config 关键入口。 |
| `mango-business-starter` | README 容易让读者误以为 `mango init --preset full` 直接读取本目录；代码事实是 full init 读取 CLI 内置 `templates/full`，业务模块优先读取 CLI 内置 `templates/business-module`，本目录是模板资产和回退模板。 | 已补充 CLI 内置模板与 business starter 回退模板边界。 |
| 根 `README.md` | `pnpm dev` / `pnpm build` 当前基线描述缺少工作目录。 | 已改为 `cd mango-ui && pnpm dev/build` 口径。 |

## 3. 架构专家评审

### B1：platform 架构

| 项目 | 意见 | 整改结果 |
|------|------|----------|
| 已覆盖 README | access/auth/authorization/identity 边界清楚；job/workflow/file/captcha 基本守住模块边界。 | 保留。 |
| 覆盖范围 | 缺 calendar、domain、file-preview、notice、numgen、org、seed、system、template README。 | 已全部新增。 |
| 能力地图 | 只列部分 platform 能力，无法支撑全量索引。 | 已升级为全量 platform 索引。 |
| 依赖方向 | 缺少跨模块组合入口。 | 已增加登录到菜单、文件到预览、审批、任务等组合接入入口。 |

### B2：infra 架构

| 项目 | 意见 | 整改结果 |
|------|------|----------|
| 已覆盖 README | kv/event/realtime/web/feign/persistence 边界和协作关系清楚。 | 保留。 |
| 覆盖范围 | 缺 context、fileproc、ip-location、log、module、sensitive、test README。 | 已全部新增。 |
| KV capability | event/realtime 依赖 KV capability，接入方容易只配 store。 | 已在相关 README 保留 capability 说明，并新增 infra-test README。 |
| 验证口径 | 多处“未发现独立测试类”会削弱验收。 | 能力地图和 README 增加 `mango-infra-test` 集成测试入口。 |

### B3：frontend / CLI / starter / governance

| 项目 | 意见 | 整改结果 |
|------|------|----------|
| frontend README | `mango-ui/README.md` 有大量长期规则正文，和 PMO 唯一规范源冲突。 | 已把长期规则收口为 PMO 规则链接，README 保留使用说明。 |
| frontend package 覆盖 | 能力地图只链接 job，缺 auth/rbac/system/file/workflow/common/app-runtime 等。 | 已新增全部 frontend package README，并更新能力地图全量索引。 |
| README 模板 | “默认值”表述可能诱导复制长期规则。 | 已改为“关键配置入口和配置来源；默认值以配置类、模板或 PMO 规则源为准”。 |
| 审计脚本 | 只扫描已存在 README，不能发现缺失 README。 | 已改为受管模块根目录缺 README 也失败。 |
| 链接检查 | Markdown 链接只校验文件存在，不能发现 `#anchor` 断裂。 | 已补充锚点检查。 |
| 能力文档映射 | 顶层后端模块源码路径可能误映射到 `src/README.md`。 | 已补充 `mango-common` 等顶层后端模块映射自测。 |

## 4. 业务开发人员评审

### C1：平台能力接入

| 项目 | 意见 | 整改结果 |
|------|------|----------|
| 单能力接入 | 已覆盖的 platform README 对业务接入基本够用。 | 保留。 |
| 多能力组合 | 缺 `identity -> auth -> authorization -> access -> menu/page` 端到端链路。 | 能力地图新增“登录到菜单闭环”。 |
| 示例 | 部分 README 示例偏模块内，不够业务项目级。 | 已在能力地图增加组合接入入口；模块 README 保留最小闭环。 |

### C2：infra / framework 接入

| 项目 | 意见 | 整改结果 |
|------|------|----------|
| infra README | 多数 README 对业务正确使用有帮助。 | 保留。 |
| `mango/README.md` | 有过时 `.claude` 链接。 | 已改为链接 `mango-pmo/rules/backend/05-module.md` 和能力地图。 |
| 顶层入口 | `mango-common`、`mango-app`、`mango-admin-starter` 没有最小闭环结构。 | 已统一到 README 模板，并补充 extension、parent、tools。 |

### C3：frontend / CLI / starter 接入

| 项目 | 意见 | 整改结果 |
|------|------|----------|
| 创建项目 | CLI、starter、topology README 能支撑创建项目和本地验证。 | 保留。 |
| PR 闭环 | 缺从 `mango init` 到验证和 PR 的集中入口。 | 能力地图新增“业务项目创建到 PR”组合入口；PR 模板和检查脚本已落地。 |
| 前端包使用 | 需要全量包 README 便于业务引用。 | 已新增全部受管 frontend package README。 |
| PR 判断维度 | PR 模板未显式提示页面、租户、运行时行为等能力影响判断维度。 | 已把 Not applicable reason 示例扩展为 public API、配置、菜单、权限、租户、页面、启动、验收、运行时行为。 |

## 5. 验证结论

| 验证项 | 结果 |
|--------|------|
| 9 名角色评审意见已归档 | PASS |
| 必须整改项已落地 | PASS |
| README 审计覆盖缺失 README | PASS |
| 受管 README 全量通过审计 | PASS |
| 能力地图覆盖 platform / infra / frontend / starter | PASS |
| 第二轮开发阻塞项已整改并复验 | PASS |

## 6. 未验证项

- 本记录基于静态 README、代码入口和脚本验证，未替代真实服务启动、数据库、Redis、浏览器和端到端业务验收。
- README 中的 Maven / pnpm 命令未全部逐个运行；本轮已用审计脚本拦截不存在的前端 package script，并对 A 类 Maven 命令做了代表性验证。
