---
documentId: TDD-RICH-TEXT-MANAGED-ASSETS
documentType: technical-design
pmoVersion: 1.3.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L2，公共富文本组件新增托管图片粘贴与附件工具栏承载能力并影响现有消费方；solution=L3，新增服务端远程图片导入入口并涉及跨前后端公开契约、租户文件存储和 SSRF 安全边界；final=max(requirement,solution)=L3
status: APPROVED
action: NEXT
owner: Mango 平台维护团队
approver: Mango Tech Lead（当前会话用户）
approvalEvidence: review/TDD-RICH-TEXT-MANAGED-ASSETS.md
upstreamDocumentId: NONE
upstreamDocumentHash: NONE
---

# 富文本托管图片与附件工具栏技术设计文档

本文仅定义 Mango 公共能力的技术方案，不包含业务审批数据结构改造，也不改变现有文件存储、租户、权限和访问级别语义。由于本任务未启用系统需求文档，以下确认基线由已确认需求直接编号，供本技术设计建立可验证追踪关系。

## 1. 设计输入、约束与决策

确认基线为：FR-001 要求 `MangoEditor` 工具栏上传图片后立即回显，`imageValueType=token` 时对外 HTML 只保存文件 ID token；FR-002 要求支持截图或本地图片、HTML 内 Base64 图片、网页 HTML 中远程图片三类粘贴上传，已有 Mango 文件 token 不重复上传；FR-003 要求普通文件保留现有 `MUpload` 和文件服务链路，但入口进入富文本工具栏；FR-004 要求 token HTML 按当前租户权限解析预览且对外值不变。

数据与安全约束为：DR-001 规定持久化图片使用 `<img src="mango-file:<id>" data-file-id="<id>">` 且不新增关联表；SA-001 规定远程图片由服务端受控导入并阻断 SSRF、DNS 重绑定、内网和元数据地址；NFR-001 禁止 Base64、Blob URL、短时预览 URL、下载 URL和第三方图片 URL进入对外 HTML或持久化数据。

验收基线为：SAC-001 验证工具栏图片上传、预览和只存 ID；SAC-002 验证全部粘贴来源进入文件服务且 token 不重复上传；SAC-003 验证合法公网导入和危险地址阻断；SAC-004 验证 token 回显及失败占位；SAC-005 验证普通文件按钮进入工具栏并保持 `purpose=attachment`、`accessLevel=PRIVATE` 和现有返回值。

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 图片预览地址与持久化 ID 如何兼容 | 直接把预览 URL 写入 HTML；保存 Base64；编辑态与持久化态双表示 | 双态 HTML：编辑器内部使用可显示地址，对外事件统一序列化为 token HTML | 预览 URL 可能过期且 Base64 会放大数据；文件 ID 才是稳定业务引用 | FR-001, FR-004, DR-001, NFR-001；`mango-ui/packages/common/components/Editor/index.vue` | 否 | 调整 Editor 的入站解析、内部状态和出站序列化 | 异步回显和用户编辑可能产生竞态 | 若消费方未启用 `imageValueType=token`，继续走原有 URL 行为 |
| DEC-002 | 粘贴图片应由谁接管 | 依赖 wangEditor 默认粘贴；业务页面分别处理；公共 Editor 选择性拦截 | 新增 `pasteImageMode=upload`，由公共 Editor 分类处理剪贴板文件、Data URI、远程 `<img>` 和 token | 统一保证图片进入文件服务，同时通过默认值 `default` 保持旧消费方兼容 | FR-002, NFR-001；`mango-ui/packages/common/components/Editor/index.vue` | 否 | 新增粘贴适配器和异步替换逻辑 | 混合 HTML 粘贴失败时可能损失非图片格式 | 关闭 `pasteImageMode=upload` 后恢复 wangEditor 默认粘贴 |
| DEC-003 | 浏览器能否直接下载远程图片再上传 | 浏览器直连；服务端代理；禁止远程图片 | 新增服务端受控远程图片导入 API，下载流复用 `SaveFileCommand` 保存 | 浏览器受 CORS 限制，且客户端无法可靠实施 SSRF 和重定向策略；现有保存命令支持 `InputStream` | FR-002, SA-001；`mango-file-api/command/SaveFileCommand.java` | 否 | 新增公开 API、核心导入服务、HTTP 适配与安全校验 | SSRF、超大响应、慢连接和伪造 MIME | 通过配置关闭远程导入后，远程图片粘贴保留文字和格式并提示不支持 |
| DEC-004 | 普通文件按钮如何进入工具栏 | Editor 内重新实现上传；绝对定位到右上角；提供工具栏扩展槽 | 在 Editor 工具栏流式布局中增加 `toolbar-actions` slot，由消费方复用现有 `MUpload` | 不复制附件状态逻辑，不改变文件存储方式，按钮与格式工具处于同一工具栏和换行规则 | FR-003, SAC-005；`mango-ui/packages/file/src/components` | 否 | 公共组件增加可选 slot，Editor 基础能力示例复用现有上传触发器验证接入方式 | slot 内容尺寸可能影响窄屏换行 | 未提供 slot 时 DOM 与现有工具栏行为保持一致 |
| DEC-005 | 是否改变现有 `url`、`id` 模式 | 全量迁移 token；仅新增 token 正确序列化；替换旧属性 | `url`、`id` 模式保持兼容；严格双态和粘贴托管仅在 `token` 加 `pasteImageMode=upload` 时启用 | 控制公共组件升级影响，允许消费方逐页迁移 | FR-001, FR-002 | 否 | 新增能力默认不改变存量页面 | 旧页面仍可能保存外部 URL | 能力稳定并完成消费审计后再单独评估默认值升级 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-ui/packages/common/components/Editor` | 管理编辑态与持久化态 HTML、工具栏上传、粘贴分类、token 回显和工具栏扩展槽 | 修改 | Editor 依赖 common upload API；不反向依赖 `@mango/file` 或任何业务包 | `imageValueType`、`pasteImageMode`、`toolbar-actions` slot 和上传状态事件 | FR-001, FR-002, FR-003, FR-004 | rules/frontend/01-vue-code.md, rules/frontend/03-component-development.md | Vitest 组件测试、类型检查和 demo 页面交互验证 |
| MOD-002 | `mango-ui/packages/common/api/upload.ts` | 提供本地图片上传、远程图片导入、文件详情查询及 token 归一化 | 修改 | 依赖 `/file/files` HTTP 契约 | `uploadImage`、`importRemoteImage`、`getUploadedFileDetail` | FR-001, FR-002, FR-004 | rules/frontend/12-business-api.md | API 单元测试与 mock 契约测试 |
| MOD-003 | `mango-file-api` | 声明远程图片导入 Command、VO 返回和 Java API 契约 | 新增 | starter 和 Feign 依赖 API；API 不依赖 core | `FileImportApi.importImage(ImportRemoteImageCommand)` | FR-002, SA-001 | rules/backend/03-api.md, rules/backend/05-module.md | 编译、Bean Validation 测试和序列化测试 |
| MOD-004 | `mango-file-core` | 校验远程目标、受限下载图片流并调用现有文件保存能力 | 新增 | core 依赖现有 `IFileService.save` 与可替换的远程获取端口 | `IRemoteFileImportService` | FR-002, SA-001, NFR-001 | rules/backend/01-code.md, rules/backend/06-security.md | 单元测试、伪造 DNS 与重定向集成测试 |
| MOD-005 | `mango-file-starter` | 暴露 HTTP Controller、配置导入限制、装配远程获取实现 | 新增 | starter 依赖 API 与 core | `POST /file/files/import-image` | FR-002, SA-001 | rules/backend/03-api.md, rules/backend/06-security.md | MockMvc 接口测试与启动上下文测试 |
| MOD-006 | `mango-file-api` Feign 适配 | 为服务间调用提供与 HTTP 入口一致的 Feign 契约 | 新增 | Feign client 依赖 `FileImportApi` | `FileImportFeignClient.importImage` | FR-002 | rules/backend/03-api.md, rules/backend/05-module.md | Feign 契约编译和路径断言测试 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001 | 表示富文本中的 Mango 托管图片引用 | `fileId`，HTML 中同时使用 `mango-file:<id>` 和 `data-file-id` | 一个 HTML 可引用多个现有文件记录；不新增关系表 | `RESOLVING`、`READY`、`FAILED` 仅存在于前端内存 | 继续由现有文件记录和业务表审计，不新增历史表 | 文件详情按当前租户解析，HTML 自身不携带租户和签名地址 | 对外 HTML 中 `src` 与 `data-file-id` 必须指向同一 ID；无法解析时不改写 token |
| DM-002 | FR-002 | 表示一次远程图片导入请求和受控下载结果 | 请求级追踪 ID；成功后以新 `fileId` 为稳定标识 | 每次成功导入创建一条现有文件记录，可由存储去重能力按现状处理 | `VALIDATING`、`FETCHING`、`SAVING`、`COMPLETED`、`FAILED` | 记录结构化安全日志，不保存来源 URL 全文 | 使用请求上下文中的当前租户，访问级别固定 `PRIVATE` | 只有校验和下载全部成功后才调用保存；失败不得创建完成文件记录 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | RESOLVING | 载入 token HTML 或上传返回文件 ID | READY | 当前租户可读取文件详情且得到有效 `previewUrl` | 编辑态 `src` 替换为预览地址，保留 `data-file-id` 与规范 token | 转为 FAILED 占位，保留原 token，不向外暴露失败 URL | FR-004, SAC-004 |
| DM-001 | READY | Editor 触发 `update:modelValue` 或 `change` | READY | 节点含合法 `data-file-id` | 克隆并规范化 HTML，向外发出 token HTML | 检测到 Base64、Blob 或非托管 URL 时从出站 HTML 移除该图片并发出错误 | DR-001, SAC-001 |
| DM-002 | VALIDATING | 接收远程图片导入请求 | FETCHING | URI、解析地址、端口和协议均通过安全策略 | 发起不携带用户凭据的受限请求 | 转为 FAILED，返回稳定错误码 | FR-002, SAC-003 |
| DM-002 | FETCHING | 响应头和内容流通过限制 | SAVING | 状态为成功、类型和魔数为图片、累计字节未超限 | 构造 `SaveFileCommand`，固定 `purpose=image`、`accessLevel=PRIVATE` | 关闭流并转为 FAILED，不保留部分内容 | FR-002, SAC-002 |
| DM-002 | SAVING | 现有文件服务保存成功 | COMPLETED | 文件记录属于当前租户 | 返回 `FileRecordVO` | 沿用文件存储失败处理与补偿 | FR-002, SAC-002 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, FR-002, SAC-001, SAC-002 | 工具栏选图、剪贴板 `File` 或 Data URI `<img>` | MOD-001, MOD-002 | 读取二进制到内存 `File`；调用现有图片上传；取得 `fileId` 和预览地址；用 wangEditor `insertImage` 插入编辑态节点；出站时规范为 token | 文件上传沿用现有单文件事务；前端无跨请求事务 | DM-001 从 RESOLVING 到 READY | 沿用现有文件上传的内容散列与秒传策略，不新增业务幂等键 | 每个图片节点带本次异步任务标识；组件销毁或内容版本变化后忽略过期回调 | 上传失败时不插入未托管图片，保留非图片剪贴内容并显示明确错误 | 图片上传完成后在光标位置可见；保存值只有文件 ID token |
| FLOW-002 | FR-002, SAC-002, SAC-003 | 粘贴 HTML 中的公网 `<img src="http/https">` | MOD-001, MOD-002, MOD-003, MOD-004, MOD-005 | 前端提取图片 URL；后端逐跳校验并受限下载；验证图片后用 `SaveFileCommand` 保存；返回文件记录；前端插入 READY 节点 | 网络获取不进入数据库事务；保存阶段沿用文件服务事务和存储补偿 | DM-002 从 VALIDATING 到 COMPLETED，随后创建 DM-001 READY 引用 | 单次请求不保证同 URL 幂等；沿用现有文件内容去重配置 | 单次粘贴限制图片数量并限制并行度；同一节点只接受最后一次任务结果 | 任一图片失败只移除对应未托管图片，非图片 HTML 继续插入；汇总提示失败原因 | 合法图片转为 Mango 文件并显示；危险地址被拒绝且不会发起到内网的连接 |
| FLOW-003 | FR-004, SAC-004 | `modelValue` 初始加载或外部更新 | MOD-001, MOD-002 | 解析 HTML；收集并去重 file ID；并行查询详情；生成编辑态 HTML；设置 Editor；出站仍使用原 token | 只读查询，无事务 | DM-001 从 RESOLVING 到 READY 或 FAILED | file ID 集合去重；同一轮解析只查询一次 | 使用递增内容版本号丢弃过期详情响应 | 单个 ID 失败不阻断其他内容，保留 token 并显示失败占位 | 可访问图片正常预览；不可访问图片显示不可预览状态 |
| FLOW-004 | FR-003, SAC-005 | 消费方在 `toolbar-actions` slot 放入现有 `MUpload` 触发器 | MOD-001 | Editor 渲染同一工具栏流；业务组件把原附件上传触发器迁入 slot；`MUpload` 继续处理选择、上传、列表和值回写 | 完全沿用附件上传事务 | 附件状态不由 Editor 接管 | 沿用 `MUpload` 和文件服务现状 | 沿用 `MUpload` 并发与分片策略 | 沿用现有附件上传失败、重试和移除行为 | 文件按钮与格式按钮同处工具栏，附件列表和保存结果不变 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, FR-002, SAC-001, SAC-002 | `uploadImage` 与 `MUpload` | MOD-005 | 现有 multipart HTTP 入口 | POST /file/files | multipart `file` 加 `purpose`、`accessLevel`、业务归属参数 | R<FileRecordVO> | 沿用文件大小、类型、名称和设置校验；图片调用固定传 `purpose=image` | `ApiAccess(LOGIN)`；当前租户；图片与附件均为 `PRIVATE` | 沿用现有上传和秒传语义 | 沿用现有文件上传错误码 | 方法、路径、请求和返回不变 | rules/backend/03-api.md | 现有 FileBinaryController 测试加 Editor API mock 测试 |
| API-002 | FR-002, SAC-002, SAC-003 | `importRemoteImage`、Java API 或 Feign 调用方 | MOD-003 | 新增 JSON HTTP 与 Java API 入口 | POST /file/files/import-image | `ImportRemoteImageCommand`：`sourceUrl` 必填且长度受限；可选业务归属参数，不开放 purpose 与 accessLevel | R<FileRecordVO> | URL 规范化；只允许 http/https；禁止 userinfo；限制端口、重定向、超时、字节数、图片 MIME 与魔数 | `ApiAccess(LOGIN)`；使用当前租户；服务端固定 `purpose=image` 和 `accessLevel=PRIVATE` | 非幂等；沿用文件内容去重配置；无分页排序 | `FILE_REMOTE_URL_INVALID`、`FILE_REMOTE_ADDRESS_FORBIDDEN`、`FILE_REMOTE_FETCH_FAILED`、`FILE_REMOTE_IMAGE_TOO_LARGE`、`FILE_REMOTE_CONTENT_INVALID` | 纯新增接口，不改变 `FileApi` 与上传入口 | rules/backend/03-api.md, rules/backend/06-security.md | Controller、Java API、Feign 契约和远程安全集成测试 |
| API-003 | FR-004, SAC-004 | `MangoEditor` token 回显解析 | MOD-005 | 现有 Query HTTP 入口 | GET /file/files/detail | `id` 查询参数，前端使用 `getUploadedFileDetail` | R<FileRecordVO> | 文件 ID 为正数 | `ApiAccess(LOGIN)`；现有租户、目录、状态、访问级别和归属校验 | 前端按去重后的 ID 并行查询，不改变接口协议 | 沿用详情查询错误码 | 方法、路径、请求和返回不变 | rules/backend/03-api.md | 现有详情测试加 token 回显组件测试 |

`ImportRemoteImageCommand` 不接收 Cookie、Authorization、任意请求头、HTTP 方法或目标 IP，避免调用方扩大远程访问能力。返回继续使用精简后的 `FileRecordVO`，前端只消费 `id`、`previewUrl`、`fileName`、`fileSize` 和 `contentType`。

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001, DR-001 | 业务现有富文本字段 | 无字段和表结构变化 | 新写入的托管图片 `src` 必须为 `mango-file:<id>`，并带相同 `data-file-id`；禁止持久化 `data:`、`blob:`、预览、下载和第三方 URL | 无新增索引 | 沿用业务表租户与审计字段；HTML 不复制文件租户信息 | 无新增 Mapper；业务模块按现状保存 HTML | Editor 规范化后的 `modelValue` | 无 migration；历史 URL 内容不自动回填，避免未经授权抓取外部资源 | 回滚前端能力不会删除已上传文件；token HTML仍可作为稳定引用保留 | rules/backend/04-db.md, rules/backend/07-persistence.md | 出站序列化单元测试和业务保存集成测试 |
| DB-002 | DM-002, FR-002 | 现有文件记录与当前存储实现 | 无字段变化；远程导入创建普通图片文件记录 | `purpose=image`、`accessLevel=PRIVATE`；内容、大小、类型和文件名满足现有文件约束 | 沿用文件表索引和去重索引 | 由现有 `IFileService.save` 写入当前租户与审计信息 | 只调用现有 Mapper 和服务；不新增 JDBC 或注解 SQL | 经过安全校验和大小限制的远程响应流 | 无 migration 和回填 | 下载或保存失败按现有文件服务关闭流、清理临时对象或补偿文件记录 | rules/backend/04-db.md, rules/backend/07-persistence.md | 文件服务集成测试和数据库记录断言 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001, SAC-003 | 远程目标地址校验 | 登录态文件导入能力 | `ApiAccess(LOGIN)`，不新增角色权限码，与上传保持一致 | `RemoteImageUrlPolicy` 在首次请求和每次重定向前执行 | 远程源不携带租户；保存时使用当前请求租户 | 仅允许 http/https；拒绝 userinfo、localhost、IP 字面量或 DNS 解析到 loopback、site-local、link-local、multicast、unspecified、保留和云元数据地址；解析结果全量通过才可连接 | 提示“图片地址不可访问或不允许导入”，不展示内部 IP | 记录请求追踪 ID、源主机散列、拒绝原因和目标地址类别，不记录查询参数和完整 URL | rules/backend/06-security.md | IPv4、IPv6、混合解析、十进制或编码主机、DNS 切换和重定向用例 |
| SEC-002 | SA-001, FR-002, SAC-003 | 受限 HTTP 下载 | 登录态文件导入能力 | 继承 API-002 | `RemoteImageFetcher` 禁用自动重定向，逐跳显式处理；连接前重新解析并校验目标；最多 3 跳 | 不向远程服务器发送 Mango 租户、用户或会话信息 | 只发送最小化 `Accept: image/*` 与固定 User-Agent；不转发 Cookie、Authorization、Proxy-Authorization、Referer 和调用方 Header；HTTPS 必须正常校验证书与主机名 | 超时、重定向或下载失败使用稳定错误消息 | 记录耗时、字节数、响应状态和跳数，不记录响应体 | rules/backend/06-security.md | 恶意重定向、DNS 重绑定、凭据泄漏和 TLS 失败测试 |
| SEC-003 | SA-001, SAC-003 | 响应资源限制与内容鉴别 | 登录态文件导入能力 | 继承 API-002 | `RemoteImageFetcher` 与内容检测器 | 保存行为仍进入当前租户 | 连接超时默认 3 秒、读取总超时默认 10 秒、最大 10 MiB；先检查 Content-Length，再用计数流硬限制；只接受受支持的 `image/*` 且魔数一致；禁止 SVG 等可执行或主动内容格式，具体白名单与现有图片设置取交集 | 区分超限、超时和非图片错误，不回显远端响应体 | 记录归一化内容类型和安全分类 | rules/backend/06-security.md | 慢响应、无长度、压缩炸弹、伪 MIME、SVG 与超限流测试 |
| SEC-004 | FR-004, DR-001, SAC-004 | token 回显和文件可见性 | 现有文件详情能力 | `ApiAccess(LOGIN)` | API-003 现有文件查询校验 | 只能解析当前租户可见文件 | 前端不得自行拼接存储地址；只使用详情返回的有效 `previewUrl`；失败时不降级为下载 URL | 显示不可预览占位，不删除原 token | 沿用文件详情审计和访问日志 | rules/backend/06-security.md, rules/frontend/12-business-api.md | 跨租户 ID、归档或删除文件、过期预览地址测试 |
| SEC-005 | DR-001, SAC-001, SAC-002 | 出站 HTML 防泄漏 | 不适用，前端组件内部约束 | 启用 token 模式即强制 | `serializeManagedHtml` 在所有对外事件和 expose 的 `getHtml` 前执行 | HTML 不携带租户、签名、bucket 或 objectName | 只允许合法 token 和 `data-file-id`；未完成、失败或非托管图片节点不得出站 | 阻止静默保存并触发 `image-error`；非图片文本与格式保留 | 前端不打印 HTML、Base64 或签名 URL | rules/frontend/03-component-development.md | Base64、Blob、短时签名 URL、第三方 URL 泄漏单元测试 |

远程地址校验必须在网络连接层与重定向处理层落地，不能只在 Controller 做一次字符串判断。主机 DNS 若返回任一禁止地址则整次请求拒绝；每一跳重新进行 URI 规范化、DNS 解析和地址分类，避免公共地址重定向到内网。实现阶段若所选 HTTP 客户端不能保证连接目标与已校验解析结果一致，必须引入可注入 DNS 解析与连接策略或等效网络出口保护，未满足前不得启用远程导入。

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001, FR-002, SAC-001, SAC-002 | 本地或 Base64 图片上传失败 | 现有上传 API 失败、缺少文件 ID 或组件在上传中销毁 | 沿用文件上传错误码；前端 `EDITOR_IMAGE_UPLOAD_FAILED` | 前端受控错误事件 | “图片上传失败，请重试”；不插入未托管图片 | 组件动作、文件类型、大小、任务 ID；不记录内容 | 前端错误上报计数 | 用户可重新粘贴或上传；不自动无限重试 | 不记录 Base64、Blob URL 和签名 URL |
| ERR-002 | FR-002, SAC-003 | 远程 URL 非法或指向禁止网络 | URI 非 http/https、含 userinfo、端口禁止、解析结果禁止或重定向越界 | `FILE_REMOTE_URL_INVALID` 或 `FILE_REMOTE_ADDRESS_FORBIDDEN` | `RemoteFileImportSecurityException` | “图片地址不可访问或不允许导入” | 追踪 ID、主机散列、地址分类、跳数、拒绝规则 | 按错误码统计拒绝次数；异常突增告警 | 不重试；保留非图片粘贴内容 | URL 查询与片段不入日志，IP 按安全日志权限保护 |
| ERR-003 | FR-002, SAC-003 | 远程下载失败或超时 | DNS、连接、TLS、非成功响应、连接或读取超时 | `FILE_REMOTE_FETCH_FAILED` | `RemoteFileFetchException` | “远程图片获取失败，请确认图片仍可访问” | 追踪 ID、主机散列、状态、耗时和跳数 | 失败率、超时率和耗时分布 | 单次不自动重试，避免重复外连；用户可主动重试 | 不记录响应体、认证头或完整 URL |
| ERR-004 | FR-002, SAC-003 | 远程内容过大或并非安全图片 | Content-Length 或计数流超限；MIME 与魔数不符；格式不在白名单 | `FILE_REMOTE_IMAGE_TOO_LARGE` 或 `FILE_REMOTE_CONTENT_INVALID` | `RemoteFileContentException` | “图片大小超过限制”或“链接内容不是受支持的图片” | 声明类型、检测类型、累计字节数；不记录内容 | 超限和伪装内容计数 | 立即关闭流，不保存，不重试 | 不落响应内容和临时文件 |
| ERR-005 | FR-004, SAC-004 | token 对应文件不可见或预览地址不可用 | API-003 返回无权限、不存在、非完成状态或无有效预览地址 | 沿用文件详情错误码；前端 `EDITOR_IMAGE_PREVIEW_UNAVAILABLE` | 前端受控错误状态 | 图片位置显示“暂不可预览”，原 HTML token 保留 | file ID、内容版本和错误分类；不记录预览 URL | 回显失败计数 | 用户可刷新；预览地址过期时重新请求详情 | 不在 DOM 属性和日志保留失败签名 URL |
| ERR-006 | FR-003, SAC-005 | 附件工具栏上传失败 | `MUpload` 现有校验、上传或分片失败 | 沿用现有附件上传错误码 | 沿用 `MUpload` 错误 | 保持现有错误提示和重试入口 | 沿用文件上传日志 | 沿用文件服务指标 | 沿用现有重试与移除 | 无新增敏感信息 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-001, SAC-001 | 工具栏上传图片 | 公共组件，无固定路由 | `MangoEditor` 内置 wangEditor `insertImage` | `imageValueType`、上传任务和 Editor 内部节点 | API-001 | disabled 时工具栏不可操作；上传中避免重复提交同一任务 | 上传期间保留光标锚点；失败不插入图片并提示 | `data-testid="mango-editor-image-upload"` | 复用现有 `uploadImage` 和 wangEditor 插图能力 | rules/frontend/01-vue-code.md, rules/frontend/03-component-development.md |
| UI-002 | FR-002, SAC-002, SAC-003 | 粘贴图片和混合 HTML | 公共组件，无固定路由 | Editor `paste` 适配器与离屏 DOM 解析器 | `pasteImageMode`、剪贴板数据和内容版本 | API-001, API-002 | disabled 时不拦截；启用 upload 时禁止未托管图片直接进入 Editor | 单图失败仅移除对应图片，保留文字、链接、表格等非图片内容；汇总提示 | `data-testid="mango-editor-paste-status"` | 公共封装，业务页面不重复监听 paste | rules/frontend/01-vue-code.md, rules/frontend/03-component-development.md |
| UI-003 | FR-004, SAC-004 | token HTML 回显 | 公共组件，无固定路由 | 入站 HTML 解析、预览地址缓存和失败占位 | `modelValue` 与递增内容版本 | API-003 | 详情权限失败时不尝试拼接 URL | 解析中显示轻量占位；单图失败不阻塞文本和其他图片 | `data-testid="mango-editor-image-unavailable"` | 复用 `getUploadedFileDetail`，不新增预览 API | rules/frontend/03-component-development.md, rules/frontend/12-business-api.md |
| UI-004 | FR-003, SAC-005 | 普通文件上传按钮进入工具栏 | MangoEditor 基础能力示例页 | `.editor-toolbar-row` 内的 `toolbar-actions` slot 与现有 `MUpload` | 继续使用示例附件 v-model 和 `MUpload` 内部状态 | API-001 | Editor disabled 时同步禁用 slot 中的上传入口 | 继续显示现有附件列表、loading 与错误；窄屏随工具栏自然换行 | `data-testid="mango-editor-attachment-upload"` | 公共 Editor 只提供 slot；示例层复用 `MUpload`，不让 common 反向依赖 file | rules/frontend/02-element-plus-ui.md, rules/frontend/03-component-development.md |
| UI-005 | FR-001, FR-004, SAC-001, SAC-004 | 对外值读取与变更事件 | 公共组件，无固定路由 | `serializeManagedHtml`、`update:modelValue`、`change`、expose `getHtml` | Editor 内部 HTML 的规范化克隆 | API-003 | token 模式下不允许非托管图片出站 | 检测到非法图片时发出 `image-error`；上传进行中发出 `uploading-change`，业务可禁用提交 | `data-testid="mango-editor"` | 在现有事件契约上收紧 token 模式语义，新增事件可选监听 | rules/frontend/01-vue-code.md, rules/frontend/03-component-development.md |

`pasteImageMode` 取值为 `default | upload`，默认 `default`。`upload` 模式的剪贴板处理顺序为：优先读取 `clipboardData.files` 中的图片；再解析 `text/html` 的每个 `<img>`；`mango-file:` token 直接进入回显解析，`data:image/` 在内存中转换为 `File` 后调用 API-001，`http/https` 调用 API-002，无法识别的 `blob:` 或其他 scheme 被移除。仅作为纯文本出现的 URL 保持文本，不自动抓取。

入站转换与出站序列化必须使用 DOM 解析和属性白名单，不使用正则直接重写整段 HTML。编辑态节点保留 `data-file-id` 与内存映射，`src` 可使用 API 返回的短时 `previewUrl`；序列化时克隆 DOM，将所有合法托管节点重写为 DR-001 形式，并清除内部状态属性。`valueHtml` 只服务 wangEditor 渲染，对外 `modelValue` 独立维护规范 HTML，避免预览地址通过 Vue watcher 回流。

普通文件入口不注册为 wangEditor 图片菜单，也不把附件写入富文本 HTML。`toolbar-actions` 与 `<Toolbar>` 放在同一个 flex 流中，顺序跟随消费方声明位置，禁止使用绝对定位到右上角；窄屏允许整组自然换行。公共 Editor 不直接依赖 `@mango/file`，基础能力示例页把现有 `MUpload` trigger 放入该 slot，附件列表按组件原交互显示在编辑器下方。

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-005, ERR-001, UI-001, UI-005 | 工具栏上传 PNG 成功后编辑区显示预览，对外事件与 `getHtml` 只含同一文件 ID token | P0 | 前端组件测试 | AUTO | 小于限制的 PNG，上传返回 id 与 previewUrl | 登录态，当前租户文件 | token 结构、事件顺序和预览 DOM | `pnpm -C mango-ui --filter @mango/common test` | Vitest 断言与 DOM 快照 | 任一出站值含 Base64、Blob 或预览 URL 即失败 | rules/frontend/04-test.md |
| TC-002 | SAC-002 | DEC-002, MOD-001, MOD-002, FLOW-001, FLOW-002, API-001, API-002, ERR-001, UI-002 | 分别粘贴剪贴板 File、Data URI、远程 HTML 图片和既有 token；前三者上传或导入，token 不重复上传 | P0 | 前端组件与接口测试 | AUTO | PNG File、Data URI、含公网图片的 HTML、token HTML | 登录态；mock 当前租户详情 | 每个新图片仅产生一次文件服务调用；非图片 HTML 保留 | 前端 Vitest 与 FileImportController MockMvc | Vitest 与 MockMvc 报告 | 调用次数、最终 HTML 或保留内容不符即失败 | rules/frontend/04-test.md, rules/backend/08-test.md |
| TC-003 | SAC-003 | DEC-003, MOD-003, MOD-004, MOD-005, MOD-006, DM-002, FLOW-002, API-002, SEC-001, SEC-002, SEC-003, ERR-002, ERR-003, ERR-004 | 远程图片导入接受合法公网图片并拒绝所有禁止地址、危险重定向、超时、超限和伪图片 | P0 | 后端单元与接口集成测试 | AUTO | 可控 DNS、HTTP 和 HTTPS 测试服务，IPv4 与 IPv6 地址集 | 登录态；断言当前租户和 PRIVATE；跨租户不可见 | 每跳校验、无凭据转发、硬字节上限和稳定错误码 | `mvn -pl mango/mango-platform/mango-file -am test` | JUnit、MockMvc、网络请求记录和文件记录断言 | 任一禁止地址收到连接、响应体落盘或错误码漂移即失败 | rules/backend/06-security.md, rules/backend/08-test.md |
| TC-004 | SAC-004 | DEC-001, MOD-001, MOD-002, DM-001, FLOW-003, API-003, SEC-004, ERR-005, UI-003, UI-005 | token 初始值回显；部分文件无权限、被删除、无 previewUrl 或外部值快速切换 | P0 | 前端组件测试 | AUTO | 两个可见 ID、一个不可见 ID、过期异步响应 | 当前租户和跨租户文件 ID | 可见图片预览、失败占位、原 token 不变、过期响应不覆盖新内容 | `pnpm -C mango-ui --filter @mango/common test` | Vitest DOM、请求次数和事件断言 | 出现 URL 泄漏、token 丢失或竞态覆盖即失败 | rules/frontend/04-test.md |
| TC-005 | SAC-005 | DEC-004, MOD-001, FLOW-004, API-001, ERR-006, UI-004 | MangoEditor 基础能力示例中的附件按钮与格式工具处于同一工具栏，上传和附件值保持 `MUpload` 现状 | P1 | UI 测试 / E2E | MANUAL | 图片、PDF 和超限文件；窄屏与常规宽度 | 登录用户；沿用文件权限与当前租户 | `purpose=attachment`、`PRIVATE`、附件 v-model 与列表交互不变 | MangoEditor demo 浏览器验证 | 截图、网络请求、附件值和回显记录 | 按钮脱离工具栏、布局遮挡或附件数据变化即失败 | rules/04-test-assets.md, rules/frontend/04-test.md |
| TC-006 | SAC-001 | DEC-005, MOD-001, DB-001, SEC-005, UI-005, IMP-001 | 未传新属性、`imageValueType=url`、`id`、`token` 各模式兼容性回归 | P1 | 前端组件回归测试 | AUTO | 四组 props 和既有 HTML | 不涉及新增权限 | 默认模式旧行为不变；token 严格规范化 | `pnpm -C mango-ui --filter @mango/common test` | Vitest 回归断言 | 任一默认行为变化即阻断发布 | rules/frontend/04-test.md |
| TC-007 | SAC-002 | DEC-002, DM-001, FLOW-001, FLOW-002, SEC-005, UI-002, IMP-002 | 混合 HTML 中一张图片成功、一张失败时保留文字、链接、列表和成功图片，移除失败图片 | P1 | 前端组件测试 | AUTO | 含两张图片、链接、列表和中文的 HTML | 登录态 | 局部失败不丢非图片内容，不持久化失败源 URL | `pnpm -C mango-ui --filter @mango/common test` | 规范 HTML 与错误事件断言 | 非图片节点丢失或失败 URL 出站即失败 | rules/frontend/04-test.md |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-005, UI-005 | `MangoEditor` 现有消费页面 | 默认上传图片使用 URL；`imageValueType=id/token` 把值直接作为 `src`，token 可能无法显示 | 仅显式启用 token 托管模式的消费方使用双态 HTML和严格出站序列化 | `pasteImageMode` 默认 `default`；`url`、`id` 及未传属性的行为保持不变 | 先在 Editor 基础能力示例验证，再由消费方按清单逐页显式启用；不自动改写历史内容 | 更新 `@mango/common` Editor props、events、slot、token HTML 契约和示例 | TC-006 | Mango 前端维护者 |
| IMP-002 | DEC-002, UI-002 | 浏览器粘贴行为 | wangEditor 按默认策略插入剪贴板 HTML，可能保留 Base64 或外部 URL | 显式 upload 模式把图片转为 Mango 文件，非图片内容尽量保持 | 纯文本 URL 不抓取；局部图片失败不丢其他内容；关闭新模式即恢复旧行为 | 消费页面显式传 `pasteImageMode=upload`，无需迁移历史 HTML | 增加四类粘贴来源、失败语义和禁止持久化来源说明 | TC-002, TC-007 | Mango 前端维护者 |
| IMP-003 | DEC-003, API-002 | Mango 文件服务部署与网络出口 | 没有远程图片导入入口 | 新增默认受限的远程图片导入能力 | 纯新增 API；通过配置可关闭远程导入；不改变上传、下载、预览和数据库 | 部署前确认服务网络出口策略、DNS 行为、超时和最大字节配置；关闭时前端显示明确提示 | 更新 File API、配置、安全边界、错误码和调用示例 | TC-003 | Mango 文件服务维护者 |
| IMP-004 | DEC-004, UI-004 | MangoEditor 通用附件工具栏扩展 | 公共 Editor 没有与格式工具同流的通用操作扩展位 | 提供可选 `toolbar-actions` slot，示例用同一个 `MUpload` trigger 验证附件工具栏接入 | slot 可选，不提供时公共 Editor DOM 和行为保持原状；附件组件和值结构不变 | 只在基础能力示例增加接入，不迁移任何业务页面或历史附件 | 更新 Editor slot 与 `MUpload` 基础接入示例 | TC-005 | Mango 前端维护者 |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| FR-001 | DEC-001, DEC-005, MOD-001, MOD-002, DM-001, FLOW-001, API-001, DB-001, SEC-005, ERR-001, UI-001, UI-005, IMP-001 | TC-001, TC-006 | 覆盖工具栏图片上传、即时预览、token 出站和旧模式兼容。 |
| FR-002 | DEC-002, DEC-003, MOD-001, MOD-002, MOD-003, MOD-004, MOD-005, MOD-006, DM-002, FLOW-001, FLOW-002, API-001, API-002, DB-002, SEC-001, SEC-002, SEC-003, ERR-001, ERR-002, ERR-003, ERR-004, UI-002, IMP-002, IMP-003 | TC-002, TC-003, TC-007 | 覆盖所有图片粘贴来源、文件服务保存及远程导入边界。 |
| FR-003 | DEC-004, MOD-001, FLOW-004, API-001, ERR-006, UI-004, IMP-004 | TC-005 | 覆盖附件按钮进入工具栏且原上传数据流不变。 |
| FR-004 | DEC-001, MOD-001, MOD-002, DM-001, FLOW-003, API-003, SEC-004, ERR-005, UI-003, UI-005 | TC-004 | 覆盖 token 回显、部分失败、租户可见性和异步竞态。 |
| DR-001 | DEC-001, DM-001, DB-001, SEC-004, SEC-005, UI-005 | TC-001, TC-004, TC-006 | 覆盖持久化 HTML 结构和禁止临时地址泄漏。 |
| SA-001 | DEC-003, MOD-003, MOD-004, MOD-005, MOD-006, DM-002, FLOW-002, API-002, SEC-001, SEC-002, SEC-003, ERR-002, ERR-003, ERR-004, IMP-003 | TC-003 | 覆盖 SSRF、重定向、资源限制、内容鉴别和网络出口要求。 |
| NFR-001 | DEC-001, DEC-002, DEC-003, MOD-004, DB-001, DB-002, SEC-003, SEC-005 | TC-001, TC-002, TC-003, TC-004, TC-007 | 覆盖 Base64、Blob、预览、下载和第三方 URL 不持久化。 |
| SAC-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-005, ERR-001, UI-001, UI-005 | TC-001, TC-006 | 验收图片上传、预览和只存 ID。 |
| SAC-002 | DEC-002, MOD-001, MOD-002, FLOW-001, FLOW-002, API-001, API-002, ERR-001, UI-002, IMP-002 | TC-002, TC-007 | 验收剪贴板来源处理、既有 token 复用和局部失败。 |
| SAC-003 | DEC-003, MOD-003, MOD-004, MOD-005, MOD-006, DM-002, FLOW-002, API-002, SEC-001, SEC-002, SEC-003, ERR-002, ERR-003, ERR-004, IMP-003 | TC-003 | 验收远程导入安全和稳定错误契约。 |
| SAC-004 | DEC-001, MOD-001, MOD-002, DM-001, FLOW-003, API-003, SEC-004, ERR-005, UI-003, UI-005 | TC-004 | 验收 token 回显、不可访问占位和内容稳定。 |
| SAC-005 | DEC-004, MOD-001, FLOW-004, API-001, ERR-006, UI-004, IMP-004 | TC-005 | 验收附件工具栏布局和现有文件存储语义。 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/2026-07-22-rich-text-managed-assets-design.md` 执行结果为 PASS。 |
| 生命周期 handoff | PASS | 用户以 Mango Tech Lead 或授权责任人身份批准，审批记录见 `review/TDD-RICH-TEXT-MANAGED-ASSETS.md`；执行 `check-lifecycle-handoff.mjs` 截至 TDD 阶段校验。 |
| 专项规范检查计划 | PASS | 已在实施计划前复核 `rules/backend/03-api.md`、`rules/backend/06-security.md`、`rules/frontend/03-component-development.md` 和 `rules/frontend/04-test.md`；实现后仍须执行前后端测试、安全专项测试与页面验收。 |
| 未关闭阻断数量 | 0 | 当前设计未发现阻断；远程 HTTP 客户端必须满足已校验 DNS 与实际连接目标一致，作为实施准入条件。 |
| Tech Lead 审批 | APPROVED | Mango Tech Lead 或授权责任人于 2026-07-22 确认公共 API、SSRF 边界、默认兼容策略和能力说明影响，证据见 `review/TDD-RICH-TEXT-MANAGED-ASSETS.md`。 |
