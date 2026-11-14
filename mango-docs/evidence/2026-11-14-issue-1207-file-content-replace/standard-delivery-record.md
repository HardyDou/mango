# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1207
- 交付模式：STANDARD
- 需求影响：L2 — 新增公开 HTTP 接口（`PUT /file/files`、`POST /file/files/replace`）、`file_upload_session` 新列和两个菜单权限码；`fileId` 和既有上传、预览、下载接口保持不变。
- 方案风险：L2 — 替换涉及物理对象、引用计数、秒传映射和预览失效；采用新对象路径，失败可回退到旧对象。
- 最终风险：L2
- 工作区决策：CREATE。worktree `/data/Mango/mango-issue-1207`，分支 `feat/issue-1207-file-content-replace`，基于 `origin/main` 的 `5bd1fe050`。

## 2. 目标与范围

- 目标：文件管理列表新增“编辑”，允许上传新文件替换指定 `fileId` 的物理内容，并同步更新文件信息；支持大文件分片替换。
- 成功条件：替换后 `fileId` 和业务引用不变，下载、预览、秒传校验按新内容工作；不出现 `file_record`、`file_object`、`file_hash_mapping` 不一致。
- 处理范围：`mango-platform/mango-file` 后端、`@mango/file` 前端包、文件菜单权限声明、模块与前端 README、能力地图。
- 不处理范围：`mango-file-preview` 的错误信息透出（Issue 中列为可另开 Issue）；旧预览产物清理策略变更；历史数据修复。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | 文件管理列表 | 用户有 `file:files:edit`，文件未归档 | 操作列出现“编辑”，打开信息编辑弹窗 | 无权限时按钮不渲染 | 前端单测覆盖编辑入口与信息校验 |
| R2 | `PUT /file/files` | 文件名、用途、访问级别、业务标记、目录 | 只更新文件信息，内容、`fileId`、`fileHash`、`fileSize` 不变 | 扩展名变化返回 `FILE_REPLACE_EXTENSION_MISMATCH`；目录或文件名冲突返回既有错误码 | 单测 R2 通过 |
| R3 | `POST /file/files/replace` | 有 `file:files:replace`，新文件扩展名与原记录一致 | 写入新物理对象，更新大小、哈希、内容类型和存储定位，旧引用递减 | 扩展名不一致、超过 `maxSize`、记录归档或状态非完成时明确失败 | 单测覆盖扩展名拒绝、内容变更、幂等 |
| R4 | 幂等替换 | 新内容哈希和大小与当前一致 | 不写入新对象，不改变引用计数，只更新文件信息和修改时间 | — | 单测验证 `putObject` 未被调用、对象未变化 |
| R5 | 分片替换 | 文件超过分片阈值 | 会话携带 `replaceFileId`，完成后更新目标记录而不是新建记录 | 会话扩展名与目标记录不一致时失败 | 单测覆盖会话创建与完成分支 |
| R6 | 物理删除开关 | `physicalDeleteEnabled=true` 且旧对象无活跃引用 | 旧物理对象被删除，引用计数和秒传映射同步失效 | 存储删除失败返回 `FILE_READ_FAILED` | 单测覆盖删除调用 |
| R7 | 权限与升级 | 已有环境升级 | 菜单声明版本 3 携带 `file:files:edit`、`file:files:replace`，平台管理员套餐默认获得 | 未授权角色看不到编辑入口 | 权限资源测试断言两个权限码和版本 3 |

## 4. 技术决定

### 4.1 三视角复核（Agent 角色视角分析）

- **事实与用户视角**：Issue 场景是生产附件损坏后无法保留 `fileId` 修复。用户明确允许同时修改文件信息、支持分片替换、新权限码默认授予管理员。来源：Issue #1207 正文和本次会话确认。
- **技术与风险视角**：新增接口不放入 `FileApi`，避免破坏 Feign 契约和既有测试桩；替换沿用记录原存储配置，避免跨存储搬迁；采用行锁 + 新对象路径，避免并发双写和覆盖唯一原件；旧对象只递减引用计数，是否删除由 `physicalDeleteEnabled` 决定。
- **验证与交付视角**：后端单测覆盖幂等、扩展名、引用计数、物理删除和分片分支；前端单测覆盖 API 路由与表单校验；真实 UI 和真实 MySQL 联调因环境缺失未执行，标记为未验证。
- **反对意见或未解决冲突**：`fileName` 允许改名但不允许改扩展名，这与 Issue 原文“保持文件名不变”存在语义扩展；本次按用户明确答复执行，并在 README 记录。
- **评审选择**：已向用户询问是否需要外部同行评审，未获指定，按 `AGENT_ONLY` 继续，保留再次发起外部评审的入口。

### 4.2 决定表

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R2 | 新增 `PUT /file/files`，使用 `UpdateFileRecordCommand`，权限 `file:files:edit`；不加入 `FileApi` 契约 | `FileController`、`IFileService`、`FileEditOperations` | 移除路由和菜单权限码 |
| D2 | R3 | 新增 `POST /file/files/replace`（multipart），权限 `file:files:replace`；扩展名必须与原记录一致 | `FileBinaryController`、`FileService`、`FileEditOperations` | 移除路由并恢复菜单声明版本 |
| D3 | R5 | `CreateFileUploadSessionCommand` 新增 `replaceFileId`，`file_upload_session` 新增 `replace_file_id`（Flyway `V7`） | 上传会话命令、实体、迁移脚本 | 保留新列，旧版本忽略；移除会话替换分支 |
| D4 | R3/R4 | 新内容写入新对象路径；相同哈希+大小时幂等；旧对象按引用计数和物理删除开关处理 | `FileEditOperations.applyReplacement` | 旧对象未被删除时可直接指回 |
| D5 | R7 | 菜单声明版本 2→3，追加两个权限码，默认随 `platform_admin` 套餐生效 | `file-common-menu.json`、前端 `v-auth` | 回退版本号与权限码 |
| D6 | 全部 | `FileService` 原有 1995 行，新增逻辑会突破 2000 行门禁；抽出 `FileEditOperations`（编辑与替换编排）和 `FileInput`（输入抽象），`FileService` 保留共享物理对象、秒传映射和引用计数职责 | `FileEditOperations.java`、`FileInput.java`、`FileService.java` | 纯结构拆分，回退即合并回 `FileService` |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1/D2 | 1 | `mango/mango-platform/mango-file/mango-file-api/.../command/ReplaceFileContentCommand.java`、`UpdateFileRecordCommand.java`、`enums/FileCode.java` | 编译通过 |
| I2 | D3 | 2 | `mango-file-core/.../entity/FileUploadSessionEntity.java`、`command/CreateFileUploadSessionCommand.java`、`db/migration/file/V7__file_upload_session_replace_target.sql` | 迁移可执行、编译通过 |
| I3 | D1/D2/D4 | 3 | `mango-file-core/.../service/impl/FileEditOperations.java`、`FileInput.java`、`FileService.java`、`IFileService.java` | 替换与信息编辑逻辑可读、单测通过 |
| I4 | D1/D2 | 4 | `mango-file-starter/.../FileController.java`、`FileBinaryController.java` | 路由和权限注解就位 |
| I5 | D5 | 5 | `file-common-menu.json`、`frontend file-common-menu` 权限测试 | 声明版本 3、两个权限码可见 |
| I6 | R1 | 6 | `mango-ui/packages/file/src/api/file.ts`、`views/files/index.vue`、`FileEditDialog.vue`、`fileEdit.ts` | 编辑入口、弹窗和替换/更新调用可用 |
| I7 | 全部 | 7 | 后端与前端 README、`mango-docs/capabilities/README.md` | 能力、权限、升级与排障说明同步 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R2/R3/R4/R5/R6 | 后端单测 | `mvn -o -Dspotbugs.skip=true -Dcheckstyle.skip=true test-compile` 后执行 `mvn -o org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test -Dtest='!FileServiceConcurrentSaveIntegrationTest'`（`mango-file-core`） | PASS（88 个测试，0 失败） | 本地 surefire 输出 |
| R2/R3/R5 | 后端单测（定向） | 同上，`-Dtest=FileServiceReplaceContentTest,FileServiceUploadSessionTest` | PASS（12 个测试） | 本地 surefire 输出 |
| R7 | 权限与访问模式单测 | `mango-file-starter` 执行 surefire 3.2.5 | PASS（12 个测试，含 `FilePermissionResourceTest`、`FileControllerAccessModeTest`） | 本地 surefire 输出 |
| R1/R3 | 前端单测 | `packages/file` 执行 `vitest run --config vitest.config.ts` | PASS（69 个测试，含 `fileReplace.spec.ts`、`fileEdit.spec.ts`） | 本地 vitest 输出 |
| R1/R2 | 前端静态检查 | `eslint` 检查改动文件；`prettier --check` | PASS | 本地命令输出 |
| R3/R5 | 真实 MySQL + 真实 UI 联调 | 启动后端与前端工作区，上传、替换、预览走查 | 未执行（环境无 MySQL 和 `mango` CLI），标记为未验证 | 无 |
| 全仓类型检查 | `run-typecheck.mjs` | 39 个目标中 35 个失败 | 基线失败（工作区包未构建，报 `Cannot find module '@mango/common'` 等），不作为本次证据 | `.runtime/frontend-quality/typecheck.json` |

## 7. 例外与剩余风险

- `mango` CLI 不在 PATH，`mango-ui/node_modules` 也无 `@mango/cli`，规则要求的 `mango workspace init` 未执行。
- 未执行真实 MySQL 迁移和真实 UI 走查，`R3/R5` 的端到端行为未验证；合并前必须在可用环境补做。
- `FileServiceConcurrentSaveIntegrationTest` 依赖本机 MySQL，本次未执行。
- 全仓前端 `typecheck` 在当前环境基线失败（工作区包未构建），需要 CI 或完整构建环境复核。
- 新权限码默认随 `platform_admin` 套餐生效；其它角色升级后需要手工授权，否则看不到“编辑”按钮。
- 旧预览产物不会随替换删除，仍按既有归档策略累积；本次不改变该行为。
- `fileName` 允许改名但不允许改扩展名，属于对 Issue 原文的产品语义扩展，按用户确认执行。
