# 标准交付记录

## 1. 元数据

- 任务 ID：MANGO-ATTACHMENT-UPLOAD-GRID-20260921
- 交付模式：STANDARD
- 需求影响：L2 - 新增 `@mango/file` 稳定公共组件 API，供业务系统独立消费分类卡片上传能力。
- 方案风险：L2 - 涉及文件上传、预览、公开导出、组件样式、类型产物和跨项目消费契约；底层上传接口和分片策略保持复用。
- 最终风险：L2
- 工作区决策：CREATE - `D:\Project\mango-attachment-upload-grid`，分支 `codex/mango-attachment-upload-grid`
- 启用能力：M01、M08、M09、M10、M13

## 2. 目标与范围

- 目标：将保函系统现有 `GuaranteeAttachmentCardGrid` 等量迁移为 Mango 公共组件 `MangoAttachmentUploadGrid`。
- 成功条件：保留分类卡片、点击/拖拽上传、单文件替换、多文件限制、格式/大小校验、预览、删除、插槽、事件、`validate()` 和两种布局；组件通过 `@mango/file` 根入口导出并拥有类型、样式、文档和组件契约。
- 处理范围：`@mango/file` 组件、类型、导出、组件合同、README、业务接入指南、能力地图、Changeset 和组件测试。
- 不处理范围：不修改保函页面引用；不删除 `GuaranteeAttachmentCardGrid`；不新增上传进度、断点续传、失败重试或其它增强；不发布 npm 包。

## 3. 可观察系统要求

| ID    | 参与者或入口                 | 输入或前置条件                          | 预期行为                                                               | 失败语义                             | 验收标准                                  |
| ----- | ---------------------------- | --------------------------------------- | ---------------------------------------------------------------------- | ------------------------------------ | ----------------------------------------- |
| SR-01 | `@mango/file` 业务组件消费者 | 传入分类和附件值                        | 渲染每个分类卡片，默认使用 `grid`，支持 `stacked`                      | 无效分类被过滤；空分类显示空态       | 组件测试验证卡片数量、布局和状态          |
| SR-02 | 上传入口                     | 点击或拖拽文件                          | 按分类规则校验并调用 `fileApi.upload()`，回写带 `categoryKey` 的附件值 | 格式、大小、数量不符合时提示并不上传 | 组件测试验证上传成功回写和 `success` 事件 |
| SR-03 | 表单校验入口                 | 调用组件 `validate()`                   | 必填资料缺失返回 `false`，完整时返回 `true`                            | 缺失分类给出提示                     | 组件测试验证缺失与完整两种结果            |
| SR-04 | 只读/自定义消费方            | `disabled` 或 `file-status`/`file-note` | 禁用时不允许上传/删除；已有文件和插槽正常展示                          | 禁用状态不触发上传操作               | 组件测试和构建通过                        |
| SR-05 | npm 消费入口                 | 从 `@mango/file` 导入组件与类型         | 构建产物、类型声明、样式和组件合同一致                                 | 导出、类型或文档缺失时质量检查失败   | 包构建、组件合同检查和定向格式检查通过    |

## 4. 技术决定

| ID    | 对应要求       | 接口/数据/权限/兼容性决定                                                                                         | 影响路径                                                  | 回滚方式                   |
| ----- | -------------- | ----------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------- | -------------------------- |
| TD-01 | SR-01 至 SR-04 | 复制现有卡片上传公开契约并统一为 Mango 命名；不改变 props、emits、slots、`v-model`、`validate()` 和业务文件值结构 | `packages/file/src/components/MangoAttachmentUploadGrid*` | 删除新增组件、测试和导出   |
| TD-02 | SR-02          | 继续调用 `fileApi.upload()`，复用 Mango 已有普通上传、分片上传和秒传策略；不在组件内复制上传实现                  | `MangoAttachmentUploadGrid.vue`、`api/file.ts`            | 删除组件即可恢复原消费路径 |
| TD-03 | SR-02、SR-05   | 文件预览改用同包 `MangoFilePreviewDialog`；组件样式跟随 `@mango/file/style.css`                                   | `MangoAttachmentUploadGrid.vue`、file README              | 回滚新增样式和引用         |
| TD-04 | SR-05          | 从 `@mango/file` 根入口公开导出组件、类型并登记组件合同和 Changeset                                               | `src/index.ts`、`component-contracts.json`、`.changeset`  | 回滚导出、合同和 Changeset |

## 5. 实施清单

| ID    | 对应决定     | 顺序 | 改动路径                                                                                                                       | 完成条件                                             |
| ----- | ------------ | ---: | ------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------- |
| IM-01 | TD-01、TD-02 |    1 | `packages/file/src/components/MangoAttachmentUploadGrid.vue`、`*.types.ts`                                                     | 等量迁移实现完成，公共包内部无保函命名或相对宿主依赖 |
| IM-02 | TD-03、TD-04 |    2 | `packages/file/src/index.ts`、`component-contracts.json`、README、`mango-docs/guides/business-integration/file-upload-form.md` | 导出、样式、合同和使用说明完成                       |
| IM-03 | 全部         |    3 | `src/components/__tests__/MangoAttachmentUploadGrid.spec.ts`                                                                   | 组件测试覆盖渲染、校验、禁用、上传回写和插槽         |
| IM-04 | 全部         |    4 | `mango-docs/capabilities/README.md`、`.changeset`                                                                              | 能力地图和发布声明完成                               |

## 6. 验收映射与结果

| 要求 ID        | 验证方式                | 命令或步骤                                                                                     | 结果                                                 | 证据                                                                                                                            |
| -------------- | ----------------------- | ---------------------------------------------------------------------------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| SR-01 至 SR-04 | M10 组件测试            | `pnpm --filter @mango/file test -- src/components/__tests__/MangoAttachmentUploadGrid.spec.ts` | PASS，5 tests passed                                 | Vitest 输出                                                                                                                     |
| SR-05          | M09 包构建              | `pnpm --filter @mango/file build`                                                              | PASS                                                 | Vite 构建及类型生成输出                                                                                                         |
| SR-05          | M09 组件合同            | `pnpm run component-contracts:check`                                                           | PASS                                                 | `component contracts PASS`                                                                                                      |
| SR-01 至 SR-05 | M09 格式与 lint         | `pnpm exec prettier --check ...`、`pnpm exec eslint ...`                                       | PASS；ESLint 仅保留测试文件已有的 2 个多组件 warning | 命令输出                                                                                                                        |
| SR-01 至 SR-05 | M09 PR 完整门禁         | `pnpm check:pr`                                                                                | PASS                                                 | 全仓构建、ratchet typecheck、受影响包测试和 package exports 均通过；`@mango/file` 共 10 个测试文件、52 个用例通过               |
| SR-05          | M09 全仓导出检查        | `pnpm run package-exports:check`                                                               | PASS                                                 | `Package export check passed`                                                                                                   |
| SR-01 至 SR-04 | M13 保函隔离环境 UI/E2E | `node baohan-ui/e2e/scripts/mango-attachment-upload-grid-browser-e2e.mjs`                      | PASS                                                 | `D:\Project\baohan-system-dev-20260921\.runtime\evidence\mango-attachment-upload-grid\result.json`、`trace.zip`、`screenshots/` |

## 7. 例外与剩余风险

- 保函系统仅在隔离验证分支和专用验证页加载候选组件，没有替换、删除或提交现有 `GuaranteeAttachmentCardGrid` 业务调用；生产业务页面仍保持原实现。
- 保函隔离环境已覆盖点击上传、拖拽替换、多文件、预览、删除、必填/格式/大小校验、`grid`/`stacked` 布局和 390x844 响应式；4 次上传均为 HTTP 200，`consoleErrors=[]`、`failedRequests=[]`，验证文件已清理。
- 本 PR 不发布 npm 包；合并后仍需通过独立发布流程生成可供外部消费者安装的版本。
