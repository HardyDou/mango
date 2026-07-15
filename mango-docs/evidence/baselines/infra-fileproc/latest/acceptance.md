# Infra Fileproc 历史债务治理验收证据

## 1. 验收范围

- 页面：`/#/file/files` 文件管理。
- 接口：文件上传、`/file/files/merge-pdf`、分页查询、预览元数据、下载、删除清理。
- 权限：默认租户平台管理员登录态。
- 数据：用例运行时创建两张唯一命名 PNG 和一份合并 PDF，结束时通过文件删除接口清理。
- 部署形态：Mango 单体后端与 `mango-admin` 源码前端。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30182`。
- 后端地址：`http://127.0.0.1:18182`，`/actuator/health` 为 `UP`。
- 数据库或租户：`mango_dev_mango_infra_fileproc_debt_182`，默认租户 `1`。
- 测试账号：平台管理员 `admin`。
- 浏览器：Playwright Chromium（Chrome channel）。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| FILEPROC-DEBT-001 | TC-001 | `/#/file/files`；上传、merge-pdf、page、preview、download、delete | 从用户入口上传两张 PNG，生成并使用两页 PDF | `fileproc-source-a/b-<timestamp>-<worker>.png`、`fileproc-merged-<timestamp>-<worker>.pdf`；租户 1；finally 按本用例 ID 清理 | 上传返回真实文件记录；合并结果为 `application/pdf`；下载内容包含 `%PDF-`、`%%EOF` 和 2 个 Page 对象；清理返回 `true` | 列表仅命中一条目标 PDF 且显示正确类型；预览弹窗按文件名打开；下载按钮可用 | file API 5xx、`pageerror`、console error 均收集并断言为空 | `mango-ui/apps/mango-admin/e2e/specs/fileproc-merge-pdf-live.spec.ts`；串行 repeat 结果 `5 passed (15.4s)`；失败自动保留 screenshot/trace | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-file` 消费 `mango-infra-fileproc` | `/#/file/files` | PNG 转 PDF 与双文件合并 | PDF 预览元数据和二进制下载 | 文件名、内容类型、预览弹窗、下载动作与清理均有业务断言 | 同 TC-001 Playwright 用例与 list 报告 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 五个并发 merge 请求 | 消费者 `mango-file` 保存相同派生内容时出现 MySQL 死锁；后端证据为 `DeadlockLoserDataAccessException`，不属于本次 fileproc 转换或契约改动 | 高并发生成相同文件时单次请求可能返回 500；串行用户流程和 fileproc 处理均稳定 | 非本次改动引入，已纳入当前“全部模块治理”目标的 `mango-file` 后续批次，不作为最终遗留关闭；后续修复事务锁顺序并增加并发 merge 集成测试 | 未单独确认例外；最终总目标处理该问题前不声明全部完成 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 可复用真实上传、图片转 PDF、合并、预览、下载和清理回归 | `mango-ui/apps/mango-admin/e2e/specs/fileproc-merge-pdf-live.spec.ts` | `pnpm exec playwright test e2e/specs/fileproc-merge-pdf-live.spec.ts --project=chromium --workers=1` | 独立新库；默认租户 1；平台管理员；每次数据唯一并由 API 清理 | file API 5xx、浏览器错误或清理失败均阻断；并发死锁按 `mango-file` 消费者债务处理 | DONE |
