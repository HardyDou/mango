# 标准交付记录

> Issue #1041：`@mango/file` 与 `@mango/common` 上传 multipart Content-Type 修复

## 1. 元数据

- 任务 ID：Issue #1041
- 交付模式：STANDARD
- 需求影响：L2 - 公共文件上传能力受损，低于分片阈值的小文件直传全部被后端拒绝，业务需人工绕行。
- 方案风险：L1 - 仅在 Mango 自身上传调用点显式声明请求头，不改传输契约、签名、权限或数据模型。
- 最终风险：L2
- 工作区决策：CREATE：`/Users/hardy/Work/mango-issue-1041`，分支 `fix/issue-1041-file-multipart-header`
- 决策复核：`AGENT_ONLY`；未指定外部同行评审。

## 2. 目标与范围

- 目标：让 `@mango/file` 与 `@mango/common` 的 FormData 上传请求显式声明 `Content-Type: multipart/form-data`，消除宿主默认 `application/json` 头导致 FormData 被 axios 序列化为 JSON 的缺陷。
- 成功条件：低于分片阈值的小文件普通上传、批量上传和服务端分片上传请求均带 multipart 头且 body 为 FormData；既有非上传请求行为不变；`@mango/file`、`@mango/common` 测试与构建通过。
- 处理范围：`mango-ui/packages/file/src/api/file.ts`、`mango-ui/packages/common/api/upload.ts`、对应 API 测试、`@mango/file`/`@mango/common` changeset。
- 不处理范围：不修改 `@mango/http-client` 默认头机制，不改造后端 `/file/files` 契约，不改分片、秒传、直传、权限、租户逻辑，不改业务仓 `uploadPdfForCompression.ts` 适配。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | `fileApi.upload(file)` | 文件小于 `multipartThreshold`，走普通上传 | `POST /file/files` 携带 `Content-Type: multipart/form-data`，body 为 FormData | 缺头时 axios 序列化 FormData 为 JSON，后端返回 500 非 multipart | 请求契约测试断言 header 与 FormData body |
| SR-002 | `fileApi.uploadBatch(files)` | 多文件批量上传 | `POST /file/files/batch` 携带 multipart 头 | 同上 | 请求契约测试断言 header 与 FormData body |
| SR-003 | `fileApi.uploadServerPart(...)` | 服务端分片模式接收分片 | `POST /file/files/uploads/{id}/parts` 携带 multipart 头 | 同上 | 请求契约测试断言 header 与 FormData body |
| SR-004 | `createUploadApi(httpClient).uploadFile/uploadImage/uploadExcel/uploadMultiple` | 宿主注入 HttpClient | 实例版上传请求携带 multipart 头 | 宿主默认 JSON 头时会重现同类失败 | 单元测试断言 header 与 FormData body |
| SR-005 | 其它普通 JSON 请求 | 会话、完成、签名、删除等 | 行为与请求头不变 | 不得回归 | `@mango/file`、`@mango/common` 全量测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002, SR-003 | `@mango/file` 定义 `MULTIPART_HEADERS` 常量并在三条 FormData 请求显式传入；不改变公开方法签名与返回类型 | `mango-ui/packages/file/src/api/file.ts` | 删除常量与三处 `headers` |
| TD-002 | SR-004 | `@mango/common` 实例版 `createUploadApi` 的旧版辅助同源写法复用同一 multipart 头；旧版函数维持现状 | `mango-ui/packages/common/api/upload.ts` | 删除常量与实例分支两处 `headers` |
| TD-003 | SR-005 | 不改 `@mango/http-client` 默认头与 `@mango/common/utils/request.ts` 全局 JSON 头；只在上传调用点显式声明，避免全局行为变化 | 不新增全局分支 | 无需回滚，未改全局 |
| TD-004 | SR-001, SR-004 | 新增请求契约回归测试与 patch changeset，发布语义为缺陷修复 | `src/api/__tests__`、`mango-ui/.changeset` | 删除对应测试与 changeset |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | `mango-ui/packages/file/src/api/file.ts` | 三处 FormData 请求带 multipart 头 |
| IM-002 | TD-002 | 2 | `mango-ui/packages/common/api/upload.ts` | 实例版 `upload*` 请求带 multipart 头 |
| IM-003 | TD-004 | 3 | `mango-ui/packages/file/src/api/__tests__/fileUploadMultipart.spec.ts`、`mango-ui/packages/common/api/__tests__/upload.spec.ts` | 普通、批量、分片、实例上传覆盖完成 |
| IM-004 | TD-004 | 4 | `mango-ui/.changeset/issue-1041-file-multipart-header.md` | `@mango/file`、`@mango/common` patch 说明完成 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001, SR-002, SR-003 | 回归测试（修复前删因验证） | `pnpm --filter @mango/file test` | 修复前 3 个新增用例 FAIL，失败点为缺 `headers['Content-Type']=multipart/form-data`；修复后 PASS | Vitest 输出（本次会话） |
| SR-001, SR-002, SR-003, SR-005 | `@mango/file` 全量测试 | `pnpm --filter @mango/file test` | PASS：12 个测试文件，58 个用例 | Vitest 输出（本次会话） |
| SR-004, SR-005 | `@mango/common` 全量测试 | `pnpm --filter @mango/common test` | PASS：36 个测试文件，369 个用例（含 2 个新增实例上传用例） | Vitest 输出（本次会话） |
| SR-001, SR-004 | 生产构建 + 类型生成 | `pnpm --filter @mango/file build`；`pnpm --filter @mango/common build` | PASS：两包 Vite build 与类型生成成功 | 各包构建输出（本次会话） |
| SR-001 | 根因机制确认 | 读取 `mango-ui/packages/common/utils/request.ts:47-49`、`mango-ui/node_modules/axios/lib/defaults/index.js:53-57`、`axios/lib/helpers/resolveConfig.js:79-85` | 确认 JSON 默认头使 axios 把 FormData 序列化为 JSON；显式 multipart 头使其返回 FormData 并由浏览器补 boundary | 源码行号 |

## 7. 例外与剩余风险

- 未执行真实后端与真实浏览器的端到端验收（M12/M13）：本记录只证明到请求契约层，即上传请求带 multipart 头且 body 为 FormData。真机小文件上传验收按后续安排进行，未据此关闭现场回归风险。
- `@mango/common/utils/request.ts` 的 axios 实例仍写死 `application/json;charset=UTF-8` 默认头；本次按 Issue 期望在上传调用点显式声明 multipart，不改全局默认头。若后续收敛全局头策略，属于独立任务。
- 业务仓 `uploadPdfForCompression.ts` 的显式 multipart 适配需在 `@mango/file` 发布新版本后再移除，本次不处理业务仓。
- 未 Push、未创建 PR：需用户明确授权后执行。
