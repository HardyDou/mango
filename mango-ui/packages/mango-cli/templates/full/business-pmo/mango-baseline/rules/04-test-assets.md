# 测试资产目录规范

## 1. 定位

- 本文件约束测试代码、回归脚本、验收证据和临时测试产物的放置位置。
- 后端测试细节遵循 `rules/backend/08-test.md`。
- 前端测试细节遵循 `rules/frontend/04-test.md`。
- 临时运行目录遵循 `rules/02-dev-environment.md`。

## 2. 目录归属

- 后端测试放到被测 Maven 模块的 `src/test/java` 或 `src/test/resources`。
- 后端跨模块能力链路放到约定的聚合测试模块或 app 模块。
- 前端应用 E2E 放到对应应用的 `e2e/specs`。
- 前端 E2E 公共 fixture、登录、接口和截图工具放到对应应用的 `e2e/support`。
- 前端包单测和组件测试放到对应包的 `src/__tests__`。
- CLI 可复用测试放到 CLI 包内的 `tests` 或 `scripts`，不得长期放在 evidence 目录。
- 发布物料、企业项目初始化和模板消费类可重复回归脚本，应放到对应包或应用的正式测试目录。

## 3. Evidence 边界

- `business-docs/evidence/` 只保存某次验收的最终证据。
- 允许保存截图、报告、trace 摘要和可复核脚本副本。
- 禁止把生成项目、依赖缓存、构建产物、运行日志和临时下载文件放入 evidence。
- 可复用脚本在 evidence 中只能作为当次执行副本；长期入口必须沉淀到正式测试目录。

## 4. Runtime 边界

- 测试生成的临时项目放到 `.runtime/projects/`。
- 测试包缓存放到 `.runtime/package-store/`。
- 测试日志、中间截图、下载文件、trace/video 原始产物放到 `.runtime/` 子目录。
- 任务结束后只把最终证据复制到 `business-docs/evidence/`。
- `.runtime/` 内容不得提交。

## 5. 命名规则

- 后端测试类使用 `XxxTest`、`XxxIntegrationTest` 或 `XxxE2ETest`。
- 前端测试文件使用 `*.spec.ts` 或 `*.test.ts`。
- 可复用脚本使用能表达目标的名称，例如 `main-framework-regression`、`enterprise-cli-runtime-regression`。
- Evidence 目录使用日期和任务名命名，例如 `2026-06-04-main-framework-regression`。

## 6. 统一入口

- 单模块测试可以使用模块原生命令。
- 跨模块回归必须提供明确命令入口。
- 主框架 E2E 入口应放在前端应用的 `e2e` 目录。
- 微前端 E2E 入口应放在壳应用的 `e2e` 目录。
- CLI 企业项目回归入口应放在 CLI 包测试目录或 CLI 包脚本中。
- 后端全仓测试入口使用 Maven 聚合命令。

## 7. 禁止事项

- 禁止把 evidence 目录当作长期测试脚本目录。
- 禁止把临时生成项目提交到仓库。
- 禁止把依赖缓存、构建产物或运行日志提交到仓库。
- 禁止只保留截图而没有可复核的验证步骤或报告。
- 禁止将同一个可复用回归脚本复制到多个长期目录。
