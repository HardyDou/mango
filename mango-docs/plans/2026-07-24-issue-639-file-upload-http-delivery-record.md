# 标准交付记录

任务：Issue #639 HTTP 环境大文件上传

## 1. 元数据

- 任务 ID：ISSUE-639-FILE-UPLOAD-HTTP
- 交付模式：STANDARD
- 需求影响：L2 - `@mango/file` 在 HTTP IP 环境达到默认分片阈值后因 Web Crypto 不可用而中断，影响业务大文件上传主路径和文件中心运行时配置契约。
- 方案风险：L2 - 同时调整前端公共组件、文件 API、后端上传会话、服务端分片合并哈希和 Flyway 数据结构，但范围限定在文件模块并可通过回退本次代码与 V3 migration 恢复。
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-issue-639`，`fix/issue-639-file-upload-http`
- 保障措施：M01、M08、M09、M10、M11、M15

## 2. 目标与范围

- Issue：[HardyDou/mango#639](https://github.com/HardyDou/mango/issues/639)。
- 根因：达到 20 MiB 的文件无条件调用 `crypto.subtle.digest`；HTTP IP 页面不是安全上下文，上传会话创建前即失败。
- 目标：HTTP IP、非 HTTPS 环境可以完成大文件上传；管理后台可以配置是否启用分片上传及分片临界值；HTTPS/localhost 和 S3 原生分片保持兼容。
- 处理范围：`@mango/file` 上传策略与 `MUpload`、文件设置管理页、文件设置 API/服务/资源、上传会话与服务端分片合并、Flyway migration、模块 README 和定向测试。
- 不处理范围：发布 npm/Maven 新版本、调整 Spring/Tomcat 或反向代理上传上限、改造客户端增量哈希 Worker、修改其它业务模块。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | HTTP IP 浏览器使用 `MUpload` | 文件达到分片临界值且 `crypto.subtle` 不可用 | 不抛出 digest 异常，创建无客户端哈希的上传会话并走 `SERVER_CHUNK` | Web Crypto 缺失不得中断上传 | 前端哈希能力测试和后端无哈希会话测试通过 |
| REQ-002 | 文件管理员使用“文件配置” | 当前租户已登录且有配置编辑权限 | 可启停大文件分片并配置临界值 | 非法或空临界值不得保存 | 配置序列化、资源初始化和服务校验测试通过 |
| REQ-003 | 普通业务用户上传文件 | 已登录并可读取当前租户文件设置 | `fileApi.upload` 和 `MUpload` 按运行时开关、临界值选择普通或分片上传 | 设置读取失败时回退兼容默认值 | 前端策略单测和构建通过 |
| REQ-004 | 无客户端哈希的服务端分片完成 | 所有分片已上传 | 服务端流式计算 SHA-256，并写回会话、文件对象、文件记录和秒传映射 | 不得以空哈希完成文件记录 | 后端编译、服务测试和 MySQL 迁移验证通过 |
| REQ-005 | HTTPS/localhost 或支持 Web Crypto 的浏览器 | 存储支持原生 multipart 且客户端提供哈希 | 保持 `S3_MULTIPART` 和上传前秒传能力 | HTTP 降级不得破坏原生分片 | 有哈希 S3 上传会话回归测试通过 |
| REQ-006 | 新库和已部署旧库升级 | 顺序执行文件模块 Flyway migration | V3 增加设置列并将会话哈希改为可空，clean-db 不重复加列 | migration 失败时禁止启动 | MySQL 8.4 顺序执行 `V1/V2/V3` 成功 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-005 | 调用哈希前检测 `globalThis.crypto?.subtle`，digest 不可用或拒绝时返回空哈希；不引入非安全上下文专用加密实现 | `mango-ui/packages/file/src/api` | 回退前端上传支持改动 |
| DEC-002 | REQ-002、REQ-003 | `FileSettings` 增加 `multipartEnabled`、`multipartThreshold`；读取接口保持 LOGIN，保存保持 `file:settings:edit` | 前后端文件设置契约、管理页和资源 | 回退设置字段及资源版本 |
| DEC-003 | REQ-001、REQ-004、REQ-005 | 空哈希会话固定使用 `SERVER_CHUNK`；服务端合并时流式计算 SHA-256；有哈希且存储支持时继续使用 `S3_MULTIPART` | `FileService`、上传会话 API | 回退服务实现；已升级数据库保留可空列不影响旧代码读取 |
| DEC-004 | REQ-006 | 保持历史 V1 不变，只新增 V3 演进列，避免 fresh database 重复执行 ADD COLUMN | 文件模块 Flyway migration | 回退应用前删除未执行的 V3；已执行环境按数据库变更流程恢复 |
| DEC-005 | REQ-003 | `fileSettingsApi` 只合并并发请求，不跨请求缓存，避免租户切换后复用旧租户策略 | `fileSettings.ts`、`MUpload.vue` | 回退并发合并和运行时策略读取 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | DEC-001、DEC-005 | 1 | `mango-ui/packages/file/src/api` | Web Crypto 降级、运行时策略和并发请求合并实现并有单测 |
| IMP-002 | DEC-002、DEC-005 | 2 | `MUpload.vue`、文件设置管理页 | 组件按租户策略上传并提前校验文件中心大小上限，页面可编辑分片配置 |
| IMP-003 | DEC-002、DEC-003 | 3 | `mango-file-api`、`mango-file-core`、资源 YAML | 设置字段、会话空哈希、服务端哈希和资源初始化完成 |
| IMP-004 | DEC-004 | 4 | `V3__multipart_upload_settings.sql` | MySQL 8.4 新库顺序迁移成功 |
| IMP-005 | 全部 | 5 | 前后端测试和 README | 自动化覆盖与公开使用说明同步完成 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-003 | 前端单元/组件测试 | `pnpm --filter @mango/file test` | PASS - 6 个测试文件、27 个用例通过 | `uploadSupport.spec.ts`、`fileSettings.spec.ts` |
| REQ-001、REQ-002、REQ-003 | 前端生产构建 | `pnpm --filter @mango/file build` | PASS | Vite 构建和类型生成成功 |
| REQ-001、REQ-002、REQ-004、REQ-005 | 后端定向测试 | `mvn -B -ntp -pl :mango-file-core -am -Dtest=FileSettingsServiceTest,FileServiceUploadSessionTest,FileResourceHandlerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true test` | PASS - 10 个用例通过 | Surefire 结果 |
| REQ-004 | MySQL 并发集成测试 | 在隔离库 `mango_issue639_concurrency` 设置 `MANGO_DB_*` 后执行 `FileServiceConcurrentSaveIntegrationTest` | PASS - 1 个集成用例通过，隔离库已删除 | Maven/Surefire 结果 |
| REQ-006 | MySQL 8.4 clean-db 验证 | 在隔离库依次执行文件模块 `V1/V2/V3`，查询新增列后删除隔离库 | PASS - MySQL 8.4.8；开关默认 1、临界值默认 20971520、会话哈希可空 | MySQL schema 查询结果 |
| REQ-002、REQ-003 | Starter 权限契约 | 执行 `FileControllerAccessModeTest` | PASS - 设置读取保持 LOGIN | Surefire 结果 |
| 全部 | 测试质量与差异检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`git diff --check` | PASS | 5 个测试文件通过质量检查，差异格式通过 |

## 7. 例外与剩余风险

- 本次没有启动完整管理端和后端执行真实 HTTP IP 浏览器上传，因此没有把 M13 UI/E2E 标记为通过；自动化覆盖了无 Web Crypto 分支、后端上传模式、配置持久化和生产构建。剩余风险是特定浏览器、反向代理和部署环境仍可能受各自请求大小或超时配置影响。
- 本次只提交源码、migration、测试和说明，不发布 npm/Maven 新版本；业务项目需等待后续版本发布后才能消费修复。
