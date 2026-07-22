---
documentId: PLAN-RICH-TEXT-MANAGED-ASSETS
documentType: implementation-plan
pmoVersion: 1.3.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L2，公共富文本组件增加托管图片粘贴与附件工具栏承载能力并影响现有消费方；solution=L3，服务端远程图片导入涉及跨前后端公开契约、租户文件存储和 SSRF 安全边界；final=max(requirement,solution)=L3
status: APPROVED
action: NEXT
owner: Mango 平台维护团队
approver: Mango 实施负责人（当前会话用户）
approvalEvidence: review/PLAN-RICH-TEXT-MANAGED-ASSETS.md
upstreamDocumentId: TDD-RICH-TEXT-MANAGED-ASSETS
upstreamDocumentHash: 9c35c67ff630715ee7244a0fb085f30f64ea6ddd31bae8f1fc37b48a78ce64ed
---

# 富文本托管图片与附件工具栏实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, DEC-002, DEC-005, MOD-001, MOD-002, DM-001, FLOW-001, FLOW-003, API-001, API-003, DB-001, SEC-004, SEC-005, ERR-001, ERR-005, UI-001, UI-002, UI-003, UI-005, TC-001, TC-002, TC-004, TC-006, TC-007, IMP-001, IMP-002 | `MangoEditor` 托管图片双态 HTML、工具栏上传、粘贴分类、token 回显、严格出站序列化及前端文件 API 适配 | `mango-ui/packages/common/components/Editor/**`、`mango-ui/packages/common/api/upload.ts` | 显式 `imageValueType=token` 与 `pasteImageMode=upload` 时所有图片只以文件 ID token 对外；编辑态只使用详情返回的临时预览地址；默认、`url`、`id` 模式保持兼容；组件测试覆盖成功、失败、竞态和混合 HTML | SAC-001, SAC-002, SAC-004；TC-001, TC-002, TC-004, TC-006, TC-007 | 不自动迁移历史 URL HTML；不把附件写入富文本；不保存 Base64、Blob、预览、下载或第三方 URL |
| DEL-002 | DEC-003, MOD-003, MOD-004, MOD-005, MOD-006, DM-002, FLOW-002, API-002, DB-002, SEC-001, SEC-002, SEC-003, ERR-002, ERR-003, ERR-004, TC-002, TC-003, IMP-003 | 远程图片导入 Java/HTTP/Feign 契约、受控下载、安全策略、文件保存接入、配置和自动化测试 | `mango/mango-platform/mango-file/mango-file-api`、`mango-file-core`、`mango-file-starter`、`mango-file-starter-remote` | `POST /file/files/import-image` 仅接受登录态 JSON 契约；逐跳校验 URI/DNS/实际连接目标，限制协议、端口、重定向、超时、大小、MIME 与魔数；成功统一通过 `IFileService.save(SaveFileCommand)` 以 `purpose=image`、`PRIVATE` 保存，失败不产生完成文件记录 | SAC-002, SAC-003；TC-002, TC-003 | 不开放调用方请求头、HTTP 方法、purpose、accessLevel 或目标 IP；不增加表、字段、migration、关联表；不绕过现有文件服务事务和补偿 |
| DEL-003 | DEC-004, MOD-001, FLOW-004, API-001, ERR-006, UI-004, TC-005, IMP-004 | Editor `toolbar-actions` slot 与现有 `MUpload` 基础能力示例 | `mango-ui/packages/common/components/Editor/index.vue`、`mango-ui/packages/admin-shell/src/views/demo/components/EditorView.vue` | slot 与 wangEditor Toolbar 位于同一 flex 流且窄屏自然换行；未传 slot 时 DOM 与行为兼容；示例继续使用现有 `MUpload`、`purpose=attachment`、`PRIVATE`、附件 v-model 和列表交互；common 不反向依赖 file | SAC-005；TC-005 | 不重写 `MUpload` 状态和接口，不增加附件持久化模型，不把附件 ID 写入富文本 HTML；不修改审批、workflow 或其它业务组件 |
| DEL-004 | IMP-001, IMP-002, IMP-003, IMP-004, TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007 | 公共 Editor、File API、安全配置和业务接入说明，以及 TC-001～TC-007 验收证据 | `mango-ui/packages/common/README.md`、`mango/mango-platform/mango-file/README.md`、`mango-docs/capabilities/README.md`、`mango-docs/evidence/2026-07-22-rich-text-managed-assets/` | 文档说明 props/events/slot/token、粘贴失败语义、远程导入配置/安全边界/错误码和兼容策略；验收证据记录命令、环境、数据、租户、结果、截图与异常 | IMP-001～IMP-004；TC-001～TC-007 | 不复制 PMO 长期规则；不在计划阶段声称测试结果；不执行发布、版本升级、commit、push 或 PR |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | API-002, ERR-002, ERR-003, ERR-004, MOD-003 | DEL-002 | Backend Dev | `mango-file-api` | NONE | 按 API-002 落地 `ImportRemoteImageCommand`、`FileImportApi` 与稳定 `FileCode` 契约，固定调用方不可传 purpose、accessLevel、Header、方法和目标 IP | Bean Validation、Java API 返回、错误码及序列化契约明确，API 模块测试可独立验证 | VAL-002, VAL-003 | B1-契约 | PLANNED |
| TASK-002 | SEC-001, SEC-002, DEC-003, MOD-004 | DEL-002 | Backend Dev / Security Reviewer | `mango-file-core` 远程地址策略与网络端口 | TASK-001 | 实现可注入 URI 规范化、DNS 解析、地址分类与连接目标锁定策略；首次请求和每个重定向前全量校验，任一禁止解析结果即拒绝 | 覆盖 IPv4/IPv6、localhost、内网、链路本地、多播、未指定、保留、云元数据、编码主机、混合解析、DNS 重绑定与三跳上限；已校验地址与实际连接目标一致 | VAL-003 | B2-后端安全 | PLANNED |
| TASK-003 | SEC-002, SEC-003, ERR-003, ERR-004, MOD-004 | DEL-002 | Backend Dev / Security Reviewer | `mango-file-core` 受限下载与内容检测 | TASK-002 | 实现禁用自动重定向的受限下载、最小请求头、TLS 主机名校验、连接/总读取超时、Content-Length 预检、计数流硬限制及 MIME/魔数白名单 | 默认 3 秒连接、10 秒总读取、10 MiB 上限可配置；不转发凭据；SVG、伪 MIME、慢流、无长度超限与 TLS 失败均关闭流并返回稳定错误 | VAL-003 | B2-后端安全 | PLANNED |
| TASK-004 | DM-002, FLOW-002, DB-002, MOD-004 | DEL-002 | Backend Dev | `mango-file-core` 导入服务与 `IFileService` | TASK-003 | 按 DM-002 编排 VALIDATING、FETCHING、SAVING、COMPLETED 内存状态，将通过校验的响应流映射为 `SaveFileCommand` 并调用现有 `IFileService.save` | 服务端固定 `purpose=image`、`FileAccessLevel.PRIVATE` 和当前租户；保存失败沿用存储补偿；源 URL 不入库、不写完整日志 | VAL-003 | B2-后端安全 | PLANNED |
| TASK-005 | API-002, MOD-005, SEC-001, SEC-002, SEC-003 | DEL-002 | Backend Dev | `mango-file-starter` Controller、配置、自动装配与模块依赖 | TASK-004 | 按 API-002 暴露登录态 Controller，装配导入开关、超时、跳数、端口、字节上限、DNS/连接策略和内容检测实现 | HTTP 路径、权限、参数校验、配置默认值与关闭语义符合 TDD；关闭时不发起网络请求；启动上下文测试通过 | VAL-002, VAL-003 | B2-后端安全 | PLANNED |
| TASK-006 | MOD-006, API-002 | DEL-002 | Backend Dev | `mango-file-starter-remote` | TASK-001 | 按 FileImportApi 公开契约提供 Feign 继承入口并接入现有远程自动装配 | Feign contextId、路径和方法与 Controller 一致，API/starter-remote 编译与契约测试通过 | VAL-003 | B2-后端安全 | PLANNED |
| TASK-007 | MOD-002, API-001, API-002, API-003 | DEL-001 | Frontend Dev | `mango-ui/packages/common/api/upload.ts` 及测试 | TASK-001 | 扩展 `importRemoteImage` 与详情查询结果归一化，保持 ID 字符串语义并区分稳定文件 ID与仅供当前渲染的预览地址 | 远程导入只提交 `sourceUrl` 与批准的业务归属参数；图片上传固定 `image/PRIVATE`；API mock 覆盖参数和返回，不把 URL 作为业务值 | VAL-001, VAL-002, VAL-004 | B1-契约 | PLANNED |
| TASK-008 | DEC-001, DM-001, FLOW-003, DB-001, SEC-004, SEC-005, UI-003, UI-005 | DEL-001 | Frontend Dev | `mango-ui/packages/common/components/Editor` 托管 HTML 适配器 | TASK-007 | 使用 DOM 解析与属性白名单实现 token 解析、去重详情查询、编辑态预览映射、失败占位、内容版本竞态保护及出站克隆序列化 | 对外 `modelValue`、`change`、`getHtml` 只输出规范 token；失效/无权限文件保留 token 并显示占位；Base64、Blob、预览和第三方 URL 不出站 | VAL-001, VAL-004, VAL-006 | B3-前端托管 | PLANNED |
| TASK-009 | DEC-002, FLOW-001, FLOW-002, ERR-001, UI-001, UI-002 | DEL-001 | Frontend Dev | `MangoEditor` 上传与 paste 适配器 | TASK-005, TASK-008 | 实现工具栏图片上传及 `pasteImageMode=upload` 分类处理剪贴板 File、Data URI、远程 HTML 图片和既有 token，按内容版本丢弃过期任务并汇总局部错误 | 每张新图片只调用一次上传/导入；既有 token 不重复上传；失败图片移除而文字、链接、列表等非图片 HTML 保留；上传状态与错误事件可选监听 | VAL-001, VAL-002, VAL-007 | B3-前端托管 | PLANNED |
| TASK-010 | DEC-004, MOD-001, FLOW-004, UI-004 | DEL-003 | Frontend Dev | `MangoEditor` 模板与样式 | TASK-008 | 在 Toolbar 同一 flex 流中提供语义化 `toolbar-actions` slot，补 disabled 与窄屏换行行为 | slot 可选且无 slot 时旧 DOM/样式行为不变；slot 内容与格式按钮不遮挡、不绝对定位；组件测试锚点稳定 | VAL-005, VAL-006 | B3-前端托管 | PLANNED |
| TASK-011 | UI-004, IMP-004, TC-005, ERR-006 | DEL-003 | Frontend Dev / QA | `mango-ui/packages/admin-shell/src/views/demo/components/EditorView.vue` | TASK-010 | 在 MangoEditor 基础能力示例复用现有 `MUpload` trigger 接入 `toolbar-actions`，保留附件列表、v-model、错误和重试数据流 | demo 可验证 `purpose=attachment`、`PRIVATE`、附件值、禁用态和窄屏布局；公共 common 包不依赖 `@mango/file`，不改任何业务数据契约 | VAL-005 | B4-基础示例 | PLANNED |
| TASK-012 | TC-001, TC-002, TC-004, TC-006, TC-007, IMP-001, IMP-002 | DEL-001 | Frontend Dev / QA | `Editor.spec.ts`、upload API 测试 | TASK-009, TASK-010 | 建立 token、粘贴来源、回显失败、竞态、兼容与混合 HTML 的组件/API 自动化覆盖 | TC-001、TC-002 前端部分、TC-004、TC-006、TC-007 均有稳定 DOM、事件、请求次数和最终 HTML 断言 | VAL-001, VAL-002, VAL-004, VAL-006, VAL-007 | B5-自动化 | PLANNED |
| TASK-013 | TC-002, TC-003, SEC-001, SEC-002, SEC-003 | DEL-002 | Backend Dev / QA / Security Reviewer | `mango-file-api/core/starter/starter-remote` 测试目录 | TASK-005, TASK-006 | 使用可控 DNS、HTTP/HTTPS 服务和文件服务 test double/集成 fixture 覆盖契约、逐跳安全、资源限制、凭据隔离、保存归属和稳定错误码 | TC-002 后端部分与 TC-003 自动化通过；禁止地址没有收到连接；成功文件归属当前租户且 PRIVATE；失败不落响应体或完成记录 | VAL-002, VAL-003 | B5-自动化 | PLANNED |
| TASK-014 | IMP-001, IMP-002, IMP-003, IMP-004 | DEL-004 | Tech Writer / Frontend Dev / Backend Dev | Common/File README、能力地图、Editor demo | TASK-011, TASK-012, TASK-013 | 同步公共 API、token/paste/slot 示例、远程导入配置/错误/安全边界、兼容策略与消费指引 | README 与能力地图事实一致，组件示例覆盖基础、禁用、错误和附件 slot；能力文档检查通过 | VAL-005, VAL-006 | B6-文档与验收 | PLANNED |
| TASK-015 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007 | DEL-004 | QA / Dev | 正式测试入口与 `mango-docs/evidence/2026-07-22-rich-text-managed-assets/` | TASK-012, TASK-013, TASK-014 | 执行 TC-001～TC-007 对应命令和浏览器步骤，记录环境、数据、租户、网络/控制台、截图、报告与失败项 | 七个 TC 均有真实结果和证据；任一 P0、安全或持久化红线失败即停止交付，不以服务启动或接口 200 代替验收 | VAL-001, VAL-002, VAL-003, VAL-004, VAL-005, VAL-006, VAL-007 | B6-文档与验收 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001, TASK-007 | TDD 已 APPROVED/NEXT 且计划获用户确认 | Java/HTTP/前端请求契约、ID/URL 边界和稳定错误码可由 mock/序列化测试验证 | NONE | TASK-001, TASK-007 在已批准契约字段不变前提下可并行 | 契约字段或错误语义有缺口时停止并回到 TDD，由 Tech Lead 决策 | Tech Lead / Backend Dev / Frontend Dev |
| MS-002 | TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-013 | MS-001 契约稳定 | 远程导入逐跳安全、受限下载、现有文件保存、Controller/Feign 和 TC-003 自动化均满足完成标准 | MS-001 | TASK-006 可与 TASK-002～TASK-005 并行；TASK-013 在对应实现稳定后分层补齐 | 无法保证已校验 DNS 与实际连接目标一致时立即阻断远程导入，不用弱化校验替代 | Backend Lead / Security Reviewer |
| MS-003 | TASK-008, TASK-009, TASK-010, TASK-012 | MS-001 完成，API mock 可用 | Editor token 双态、四类粘贴、slot、兼容性与前端自动化满足 TC-001、TC-002、TC-004、TC-006、TC-007 | MS-001 | TASK-010 可在 TASK-008 公共状态边界稳定后与 TASK-009 并行 | 任一对外 HTML 含禁止 URL 或默认模式回归时停止合入 | Frontend Lead |
| MS-004 | TASK-011, TASK-014, TASK-015 | MS-002、MS-003 完成 | 基础能力示例、说明资产和 TC-001～TC-007 证据完整，未关闭阻断为 0 | MS-002, MS-003 | TASK-011、TASK-014 可在公共 slot 契约稳定后与验证准备并行 | TC-005 示例失真、安全 P0 失败或证据不完整时升级当前用户/Tech Lead，不进入提交阶段 | Implementation Owner / QA |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001 | TASK-007, TASK-008, TASK-009, TASK-012, TASK-015 | 前端组件/API | 执行 `pnpm -C mango-ui --filter @mango/common test` 中工具栏上传用例，并检查 `getHtml`、`update:modelValue`、`change` 与编辑态 DOM | worktree 前端 Vitest/happy-dom | 小于限制的 PNG；上传 mock 返回长整型字符串 ID 与短时 previewUrl | 登录态契约；当前租户 PRIVATE 文件 | 编辑区显示预览；所有对外值只含同一 `mango-file:<id>` 与 `data-file-id`，不含 Base64、Blob 或预览 URL | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-001.md` | Frontend Dev / QA | 任一禁止值泄漏即 P0 失败，停止后续交付并修复序列化边界 |
| VAL-002 | TC-002 | TASK-001, TASK-005, TASK-007, TASK-009, TASK-012, TASK-013, TASK-015 | 前端组件与后端接口 | 执行 common Vitest 四类粘贴用例及 FileImportController MockMvc 契约用例 | 前端 mock + 后端测试上下文 | PNG File、Data URI、含公网 `<img>` 的 HTML、既有 token HTML | 登录态；详情和导入均使用当前租户 | 前三类各产生一次上传/导入；token 不重复上传；非图片 HTML 保留；最终只输出托管 token | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-002.md` | Frontend Dev / Backend Dev / QA | 调用次数、最终 HTML、权限或内容保留不符即失败，不降级保存来源 URL |
| VAL-003 | TC-003 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-013, TASK-015 | 后端单元、接口、安全集成与模块质量 | 执行 `mvn -pl mango/mango-platform/mango-file -am test`；实现完成后对直接修改模块执行 `mvn -pl mango/mango-platform/mango-file verify`，使用可控 DNS/HTTP/HTTPS fixture 断言网络记录和文件记录 | worktree JDK/Maven 测试环境；可控双栈 DNS 与 TLS 测试服务 | 合法 PNG/JPEG/WebP/GIF、IPv4/IPv6 禁止地址、重绑定、重定向、慢流、超限、伪 MIME、SVG、TLS 失败 | 登录态；当前租户保存；跨租户详情不可见；无 Cookie/Authorization 转发 | 合法图片以 image/PRIVATE 保存；每跳校验且实际连接目标受控；危险目标未收到连接；超时/超限/伪图片返回稳定错误且无完成记录 | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-003.md` | Backend Dev / Security Reviewer / QA | 任一内网连接、凭据泄漏、上限绕过、响应落盘或错误码漂移即阻断 |
| VAL-004 | TC-004 | TASK-007, TASK-008, TASK-012, TASK-015 | 前端组件 | 执行 common Vitest token 初值、部分失败、无 previewUrl 和快速切换用例 | worktree 前端 Vitest/happy-dom | 两个可见 ID、一个不可见/删除 ID、过期异步响应 | 当前租户和跨租户文件 ID | 可见图片预览、失败占位、原 token 不变、过期响应不覆盖新内容，DOM/日志不保留失败签名 URL | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-004.md` | Frontend Dev / QA | URL 泄漏、token 丢失或竞态覆盖即 P0 失败 |
| VAL-005 | TC-005 | TASK-010, TASK-011, TASK-014, TASK-015 | 浏览器 UI/E2E 与网络断言 | 启动 Mango 本地环境，在 Editor demo 上传图片、PDF、超限文件；检查常规/窄屏布局、请求 payload、附件列表、禁用态、console 与 network | Mango workspace 本地服务；mango-admin 浏览器 | 图片、PDF、超限文件；常规和窄屏视口 | 登录用户；沿用文件权限与当前租户 | 文件按钮与格式按钮同一工具栏且不遮挡；请求保持 attachment/PRIVATE；附件 v-model、列表、错误和回显不变 | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-005.md` 及定向截图 | Frontend Dev / QA | 按钮脱离工具栏、布局遮挡、网络/控制台异常或附件值变化即失败 |
| VAL-006 | TC-006 | TASK-008, TASK-010, TASK-012, TASK-014, TASK-015 | 前端组件回归与包构建 | 执行 `pnpm -C mango-ui --filter @mango/common test` 与 `pnpm -C mango-ui --filter @mango/common build`，覆盖未传新属性及 url/id/token 四组 props | worktree Node/pnpm | 四组 props 与既有 HTML | 无新增权限 | 默认、url、id 行为与现状一致；token 严格规范化；公共包类型与构建产物完整 | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-006.md` | Frontend Dev / QA | 任一默认行为或公开类型回归即阻断发布准备 |
| VAL-007 | TC-007 | TASK-009, TASK-012, TASK-015 | 前端组件 | 执行 common Vitest 混合 HTML 局部失败用例，断言 DOM、事件、最终规范 HTML 与失败汇总 | worktree 前端 Vitest/happy-dom | 一张成功图片、一张失败图片、中文、链接、列表 | 登录态 mock | 成功图片成为 token；失败图片被移除；文字、链接、列表保留；失败来源 URL 不出站 | `mango-docs/evidence/2026-07-22-rich-text-managed-assets/tc-007.md` | Frontend Dev / QA | 非图片节点丢失或失败 URL 出站即失败 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|

本任务的 DB-001、DB-002 明确不改变表、字段、索引、Mapper、migration 或历史数据；因此不创建 `DATA-xxx`。远程导入只通过现有 `IFileService.save` 产生普通文件记录，历史 URL 富文本不自动抓取、不回填。

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001, IMP-002, IMP-004, DEL-001, DEL-003 | `mango-ui/packages/common/README.md` 与 `mango-ui/packages/admin-shell/src/views/demo/components/EditorView.vue` | 说明 `imageValueType`、`pasteImageMode`、`toolbar-actions`、事件、token HTML、失败语义、兼容默认值和 MUpload slot 示例 | Frontend Dev / Tech Writer | API 表、示例、禁用/错误/业务集成与禁止持久化来源说明完整，示例只使用公开包能力 | `pnpm -C mango-ui --filter @mango/common test && pnpm -C mango-ui --filter @mango/common build` | NONE |
| DOC-002 | IMP-003, DEL-002 | `mango/mango-platform/mango-file/README.md` | 说明 import-image 契约、配置开关/上限、SSRF 与网络出口边界、稳定错误码、Feign/Java API 用法和 image/PRIVATE 固定语义 | Backend Dev / Security Reviewer / Tech Writer | README 与 API、配置类、默认值及测试入口一致，不暴露内部或可滥用请求能力 | `node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD` | NONE |
| DOC-003 | IMP-001, IMP-002, IMP-003, IMP-004, DEL-004 | `mango-docs/capabilities/README.md` 与 `mango-docs/evidence/2026-07-22-rich-text-managed-assets/` | 更新 Editor/File 能力索引，并以 acceptance-evidence 同结构记录 TC-001～TC-007 真实结果 | QA / Tech Writer | 能力入口可追踪到模块 README；证据包含命令、环境、数据、租户、结果、截图/报告和失败项 | `node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/evidence/2026-07-22-rich-text-managed-assets/acceptance.md` | NONE |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | HTTP 客户端只做连接前 DNS 校验，实际连接时重新解析或无法锁定已校验地址 | DNS 重绑定可绕过 SSRF 边界并访问内网/元数据服务 | 采用可注入 DNS 解析和连接目标锁定；禁用自动重定向；每跳重新校验；TC-003 记录实际目标 | 无法证明连接一致性时保持远程导入关闭并升级 Security Reviewer，不接受字符串校验替代 | Backend Lead / Security Reviewer | 2026-07-25 | OPEN | NONE | NONE |
| RISK-002 | L2 | RISK | token 回显、粘贴上传与外部 modelValue 快速切换并发 | 过期响应覆盖新内容、预览 URL 回流或用户编辑丢失 | 内容版本号、任务标识、DOM 克隆序列化、组件卸载取消/忽略；TC-004/TC-007 竞态测试 | 定位到具体状态边界后修复并补回归；不得用延时或全量覆盖临时规避 | Frontend Lead | 2026-07-25 | OPEN | NONE | NONE |
| RISK-003 | L2 | RISK | 公共 Editor 默认值、工具栏 DOM、url/id 模式或 wangEditor 粘贴行为被新能力影响 | 现有消费页面回归或保存值变化 | 新能力显式开启；slot 可选；TC-006 四模式回归和公共包构建 | 任一默认回归立即回退对应实现并收窄启用条件，不改变已批准 token 严格语义 | Frontend Lead | 2026-07-25 | OPEN | NONE | NONE |
| RISK-004 | L1 | RISK | 把通用 Editor 附件工具栏能力误解为审批组件接入 | 计划可能越权修改 workflow 或其它业务数据契约 | 公共 common 只提供 slot，`admin-shell` demo 负责组合 `MUpload`；TDD 与计划显式排除审批和业务组件 | 用户已于 2026-07-22 澄清范围，TDD 与 Plan 已同步并刷新上游摘要 | Mango Tech Lead / Frontend Lead | 2026-07-22 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DM-001, DB-001, SEC-004, SEC-005 | DEL-001 | TASK-008, TASK-012 | VAL-001, VAL-004, VAL-006 | MS-003, RISK-002 | 双态 HTML、token 回显、严格出站和无数据库变化由适配器与组件回归覆盖。 |
| DEC-002, FLOW-001, FLOW-002, ERR-001, UI-002 | DEL-001 | TASK-007, TASK-009, TASK-012 | VAL-002, VAL-007 | MS-003, RISK-002 | 四类粘贴、局部失败与非图片 HTML 保留由 API 适配、paste 适配器和组件测试覆盖。 |
| DEC-003, MOD-003, MOD-004, MOD-005, MOD-006, DM-002 | DEL-002 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-013 | VAL-002, VAL-003 | MS-001, MS-002, RISK-001 | 远程导入契约、逐跳受控下载、保存链路、Controller/Feign 与安全集成测试完整承接。 |
| DEC-004, MOD-001, FLOW-004, ERR-006, UI-004 | DEL-003 | TASK-010, TASK-011 | VAL-005, VAL-006 | MS-003, MS-004, RISK-004 | common 提供通用 slot，Editor demo 组合现有 MUpload 验证附件工具栏基础能力，不涉及业务组件。 |
| DEC-005, UI-005 | DEL-001 | TASK-008, TASK-009, TASK-010, TASK-012 | VAL-001, VAL-006 | MS-003, RISK-003 | 默认/url/id 兼容与 token 严格行为由四模式回归锁定。 |
| MOD-002, API-001, API-003 | DEL-001 | TASK-007, TASK-008 | VAL-001, VAL-004 | MS-001, MS-003 | 沿用上传与详情接口，只扩展远程导入适配并保持 ID/运行时 URL 边界。 |
| API-002, DB-002, SEC-001, SEC-002, SEC-003 | DEL-002 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-013 | VAL-002, VAL-003 | MS-002, RISK-001 | API、文件保存、SSRF、重定向、资源限制与租户归属由后端实现和 TC-003 覆盖。 |
| ERR-002, ERR-003, ERR-004 | DEL-002 | TASK-001, TASK-002, TASK-003, TASK-005, TASK-013 | VAL-003 | MS-002, RISK-001 | 非法地址、获取失败、超限和内容非法映射到稳定错误契约且不泄露敏感内容。 |
| ERR-005, UI-003 | DEL-001 | TASK-007, TASK-008, TASK-012 | VAL-004 | MS-003, RISK-002 | token 不可见、删除、无预览和异步过期由占位和原 token 保留测试覆盖。 |
| UI-001 | DEL-001 | TASK-009, TASK-012 | VAL-001 | MS-003, RISK-003 | 工具栏图片上传、即时预览、错误事件和上传状态由组件测试覆盖。 |
| TC-001 | DEL-001 | TASK-007, TASK-008, TASK-009, TASK-012, TASK-015 | VAL-001 | MS-003, MS-004 | 自动化和最终证据验证工具栏上传、预览和只存 token。 |
| TC-002 | DEL-001, DEL-002 | TASK-001, TASK-005, TASK-007, TASK-009, TASK-012, TASK-013, TASK-015 | VAL-002 | MS-002, MS-003, MS-004 | 前后端联合覆盖四类粘贴来源与不重复上传。 |
| TC-003 | DEL-002 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-013, TASK-015 | VAL-003 | MS-002, MS-004, RISK-001 | 网络安全、资源限制、稳定错误与文件记录断言形成 P0 安全证据。 |
| TC-004 | DEL-001 | TASK-007, TASK-008, TASK-012, TASK-015 | VAL-004 | MS-003, MS-004, RISK-002 | token 回显、部分失败与外部快速切换由组件竞态测试覆盖。 |
| TC-005 | DEL-003, DEL-004 | TASK-010, TASK-011, TASK-014, TASK-015 | VAL-005 | MS-004, DOC-001, RISK-004 | MangoEditor 基础能力示例验证工具栏附件 UI、原 MUpload 数据流与 responsive 布局。 |
| TC-006 | DEL-001, DEL-004 | TASK-008, TASK-010, TASK-012, TASK-014, TASK-015 | VAL-006 | MS-003, DOC-001, RISK-003 | 公共 Editor 四模式兼容、包构建和使用说明同步覆盖。 |
| TC-007 | DEL-001 | TASK-009, TASK-012, TASK-015 | VAL-007 | MS-003, MS-004, RISK-002 | 混合 HTML 局部失败时内容保真与禁止 URL 出站由组件测试覆盖。 |
| IMP-001, IMP-002 | DEL-001, DEL-004 | TASK-008, TASK-009, TASK-012, TASK-014 | VAL-001, VAL-002, VAL-006, VAL-007 | DOC-001, DOC-003, RISK-003 | Editor 兼容、粘贴接入和公共说明同步纳入同一交付批次。 |
| IMP-003 | DEL-002, DEL-004 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-013, TASK-014 | VAL-003 | DOC-002, DOC-003, RISK-001 | File API、配置、安全边界、网络出口和错误码说明与测试一致。 |
| IMP-004 | DEL-003, DEL-004 | TASK-010, TASK-011, TASK-014 | VAL-005 | DOC-001, DOC-003, RISK-004 | slot 与既有 MUpload 的通用基础接入说明和 demo 验证保持追踪。 |
| FLOW-003 | DEL-001 | TASK-008, TASK-012 | VAL-004 | MS-003, RISK-002 | 入站 token 解析、并发详情查询和过期响应丢弃由组件测试承接。 |
| ERR-003, ERR-004, ERR-005 | DEL-001, DEL-002 | TASK-003, TASK-008, TASK-012, TASK-013 | VAL-003, VAL-004 | MS-002, MS-003 | 后端下载/内容错误与前端预览不可用分别保持稳定反馈和敏感信息边界。 |
| MOD-001, MOD-002, MOD-003, MOD-004, MOD-005, MOD-006 | DEL-001, DEL-002, DEL-003 | TASK-001, TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-007, TASK-008, TASK-009, TASK-010, TASK-011 | VAL-001, VAL-002, VAL-003, VAL-004, VAL-005, VAL-006, VAL-007 | MS-001, MS-002, MS-003, MS-004 | 所有设计模块均映射到明确任务、依赖、验证和里程碑。 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/plans/2026-07-22-rich-text-managed-assets-implementation-plan.md` 执行结果为 PASS。 |
| 生命周期 handoff | PASS | 用户确认实施范围与计划，审批记录见 `review/PLAN-RICH-TEXT-MANAGED-ASSETS.md`；执行截至 Plan 的 FULL 生命周期检查。 |
| 依赖图 | PASS | 实施计划 checker 已验证 TASK-001～TASK-015 前置关系存在且无环；关键路径为 MS-001 契约、MS-002/MS-003 后端与前端并行、MS-004 消费接入与验收收口。 |
| 未关闭阻断数量 | 0 | 用户已澄清只交付富文本基础能力；RISK-004 已关闭，当前无 BLOCKER。 |
| 实施审批 | APPROVED | 当前会话用户于 2026-07-22 确认富文本基础能力范围与实施计划，证据见 `review/PLAN-RICH-TEXT-MANAGED-ASSETS.md`。 |
