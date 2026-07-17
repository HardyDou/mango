# mango-file-preview 历史债务治理验收证据

## 1. 基准与范围

- 基准 commit：`8de88b890ee7ae2038987fedfb0c67cd09fe2765`
- 目标模块：`mango-file-preview-api/core/engine/starter`
- 直接协作范围：`mango-file-starter` 权限资源、`mango-file-preview-app` Flow 回归、`mango-infra-web-starter` 参数校验异常映射、Mango 架构规则对本地 HTTP 适配器与上游 vendored 代码的边界；规则完整检查暴露的两个 main 阻断按根因收口到 web support 和 resource sync metadata。
- 不变契约：文件 ID 入口、`FilePreviewLinkVO` 结构、业务码 `180001~180003`、文件中心可见性和租户语义、短期 token 语义、kkFileView 预览行为保持不变。
- 明确缺陷修复：源文件 token 改为 query 参数以符合 Mango 无 PathVariable 契约；预览入口正式声明 `file:files:download` 权限；公开预览路径不再通过 `web.ignoring()` 绕过安全链。

## 2. 改前基线

| 项目 | 结果 |
|---|---|
| 直接模块单元测试 | 43/43 PASS：API 0、Core 2、Engine 33、Starter 8 |
| 旧微服务测试 | 1 PASS，但 Mock `FileApi`/内容 Provider，关闭 DB 和授权，不作为最终 E2E 结论 |
| 架构门禁 | 244 个问题：API 1、Core 13、vendored `cn.keking` 211、Starter 19 |
| 安全启动 | 产生 22 条 `WebSecurity.web.ignoring()` 警告 |

## 3. 修复后自动化结果

| 层级 | 命令/范围 | 结果 |
|---|---|---|
| 预览直接模块 | `mvn -f mango/pom.xml -pl :mango-file-preview-api,:mango-file-preview-core,:mango-file-preview-engine,:mango-file-preview-starter test` | 48/48 PASS：1 + 6 + 33 + 8 |
| 文件权限资源 | `mvn -f mango/pom.xml -pl :mango-file-starter test` | 6/6 PASS |
| 微服务 Flow | `MangoFilePreviewAppFlowTest` | 3/3 PASS：主链路、缺少 `fileId` 与空 token HTTP 400 |
| Web 异常边界 | `WebBoundaryIntegrationTest` | 4/4 PASS：方法校验 `ConstraintViolationException` 返回 HTTP 400 和稳定消息 |
| 完整规则阻断定向回归 | `InternalCallFilterTest`、Auth/Authorization 安全配置、Resource Sync 自动配置 | 23/23 PASS：10 + 5 + 4 + 4；共享常量移动后消费者正常，module metadata 与 Controller 根路径一致 |
| 架构规则 | `mvn -f mango/pom.xml -pl :mango-architecture-rules test` | 161/161 PASS；包含 vendored 边界、页面/流适配器和 JSON `ResponseEntity` 不得绕过 API 契约的反例 |
| 定向架构门禁 | file-preview 四个子模块 | dependency=0、ArchUnit=0、PMD=0、blocking=0 |

## 4. 全新数据库与单体 E2E

- 数据库：`mango_dev_mango_file_preview_debt_005`，从 0 表开始。
- 默认新库启动：PASS；Flyway 和正式 Resource Registry 正常。
- 权限缺陷复现：修复前上传成功但预览链接返回 HTTP 403；根因是 Controller 使用 `file:files:download`，文件菜单没有声明该 `apiCode`。
- 修复后资源：`file-common-menu.json` version 2，管理员权限集包含 `file:files:download`。
- 真实 Playwright：`file-preview-types-live.spec.ts` Chromium 1/1 PASS（17.2s）。
- 格式结果：TXT、PNG、PDF、ZIP（含包内 PDF）的上传、链接、页面和下载全部 PASS。XLSX 上传/下载 PASS，宿主未安装 LibreOffice，页面按既有语义显示不可转换，记录为环境例外而非业务失败。
- 测试数据：Playwright `finally` 中删除；手工双进程验证文件也已调用物理删除接口清理。

## 5. 微服务真实链路

- 文件能力进程：`mango-file-capability-app` / 18105。
- 预览能力进程：`mango-file-preview-capability-app` / 18205。
- 服务发现：Spring Simple Discovery 为 `mango-file-capability-app` 登记真实实例，Feign 经 Mango module service 路由调用。
- 业务结果：真实 multipart 上传 `README.md` 成功；预览服务远程读取元数据和二进制流；预览页返回 HTTP 200；解码后正文与源文件在引擎既有 HTML 转义/换行规范化后 SHA-256 一致。
- 安全和运行日志：新进程无 `web.ignoring()` 警告，无系统异常。

## 6. 调试中发现的构件问题

1. 工作区源码已删除过期 migration 和 Security Customizer，但普通增量安装后 `~/.m2` 仍可能被旧 JAR 污染。
2. 旧 `mango-infra-feign-starter` JAR 在模块服务名改写后丢失 `/file/files` 基础路径，双进程真实调用返回 404；源码中已有保留 base path 的修复和契约测试。
3. 对直接构件执行 `clean install`后，目标 JAR 和 `~/.m2` SHA-256 一致，JAR 清单不再包含删除类；重启双进程后链路通过。

结论：只检查源码或普通 `mvn test/install` 不能证明运行制品正确；删除类/migration/资源后必须 clean，并检查 JAR 清单与摘要。

## 7. 范围外环境事实

- 全量 demo 扫描会因 Authorization 正式 login-role 资源与 Identity demo 资源 ID `2026071609000000021` 冲突而失败，这是 Issue #522 所属的“资源 ID 必须全局唯一”问题，未在 file-preview 治理中隐藏或跨范围修改。
- 为验证真实角色权限，验收时只显式加载 `authorization-demo-role.yml`；正式默认新库启动仍以 demo 关闭通过。

## 8. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| FP-001 | TC-001 | `GET /file-preview/files/preview-link` | 预览链接与参数校验 | 隔离库文件 ID；缺失 `fileId` | 有效文件返回短期 token 链接；缺参 HTTP 400 | 接口用例不涉及 UI；页面结果由 TC-002 验证 | 无未解释 4xx/5xx | Surefire：API/Core/Flow 报告 | PASS |
| FP-002 | TC-002 | `/file-preview/files/preview-entry?token=...` | TXT、PNG、PDF、ZIP 在线预览 | Playwright 动态上传且 `finally` 删除 | 上传、预览链接、预览页、下载均成功；ZIP 内 PDF 可读取 | 960x720 下 ZIP 目录与预览区无重叠，非 PDF 内页无冗余 header | 无 console error、资源 404 或未解释接口失败 | `browser-results.json`、`zip-preview.png` | PASS |
| FP-003 | TC-003 | XLSX 预览入口 | Office 环境降级 | Playwright 动态 XLSX | 上传与下载成功；例外原因：宿主未安装 LibreOffice，页面显示明确不可转换提示 | 页面不是 404、500、空白或无限加载 | 无未解释系统异常 | `browser-results.json` | EXCEPTION |
| FP-004 | TC-004 | 单体资源与权限链 | 全新数据库启动、下载权限 | `mango_dev_mango_file_preview_debt_005`，tenant `1`，admin | Flyway/Resource Registry 成功；`file:files:download` 授权后预览可用 | 浏览器预览页面可见且可操作 | 修复后无 403、无 `web.ignoring()` 警告 | 启动日志摘要与 Playwright 结果 | PASS |
| FP-005 | TC-005 | 文件服务 18105 → 预览服务 18205 | 双进程 Feign 元数据和二进制读取 | 临时上传工作区 `README.md`，完成后物理删除 | 远程元数据、内容、预览页均成功；渲染正文 SHA-256 与源文件一致 | 预览正文可见 | 无 404、系统异常或 `web.ignoring()` 警告 | 双进程验收日志摘要见第 5 节 | PASS |

## 9. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 业务开发者 | 按文件 ID 获取预览链接；预览要求文件下载权限；源文件 token 使用 query 参数 | `mango/mango-platform/mango-file-preview/README.md` | 第 3 节四组定向命令及正式 Playwright 用例 | 使用隔离数据库、tenant `1`；测试文件由用例清理 | Office 转换不可用时检查 LibreOffice/插件配置；远程 404 时先核对 clean 后 JAR 与服务路由 | DONE |
