# 2026-05-26 文件预览 E2E 验证报告

## 1. 目标

在新工作区使用新数据库启动 Mango 后端和管理前端，通过 E2E 验证多类型文件上传、预览入口和下载链路，并登记存在问题。

## 2. 范围

- 新工作区：`/Users/hardy/Work/mango-file-preview-e2e-20260526`
- 后端：`mango-monolith-app`
- 前端：`mango-ui/apps/mango-admin`
- 文件类型：`txt`、`png`、`pdf`、`zip`、`xlsx`
- 验证链路：登录、上传、生成预览入口、浏览器打开预览页、下载接口返回文件内容

## 3. 不做什么

- 不扩展文件预览新格式能力。
- 不验证 MinIO、OSS、COS、Kodo 等远端存储。
- 不验证全量后台菜单和无关业务流程。

## 4. 设计输入

- 用户要求：新工作区、自选端口、新数据库、启动前后端、E2E 测试各种类型文件预览并登记问题。
- PMO 规范：`mango-pmo` QA 验证流程、后端测试规范、前端测试规范、交付契约规则。

## 5. 设计说明

### 5.1 影响模块

- `mango/mango-app/monolith/mango-monolith-app`
- `mango/mango-platform/mango-file`
- `mango/mango-platform/mango-file-preview`
- `mango-ui/apps/mango-admin`
- `mango-docs/plans`

### 5.2 接口变化

本次不改接口，仅验证以下接口：

- `POST /auth/login`
- `POST /file/files`
- `GET /file-preview/files/preview-link`
- `GET /file-preview/files/preview-entry`
- `GET /file/files/download`
- `GET /file/local-objects/{bucket}/**`

### 5.3 数据变化

- 新建 MySQL 数据库：`mango_preview_e2e_20260526`
- Flyway 初始化后，验证环境将 `file_storage_config` 中 `LOCAL` 配置设为 active，将 `MINIO` 配置设为 inactive。
- E2E 上传的文件记录在用例结束后调用归档接口清理。

### 5.4 菜单/页面/权限变化

本次不修改菜单、页面和权限数据。E2E 使用 `admin/admin123` 登录并通过真实权限访问文件接口。

### 5.5 测试范围

- 正常流：`txt`、`png`、`pdf` 上传、预览入口、下载链路。
- 异常/限制流：`zip` 预览页目录读取失败；`xlsx` 在 Office 插件禁用环境下显示不可预览提示。
- 环境流：新数据库默认活动存储为 MinIO 时上传失败，切换 LOCAL 后验证继续。

## 6. 环境与启动命令

- 后端端口：`7781`
- 前端端口：`5581`
- 数据库：`mango_preview_e2e_20260526`
- 存储目录：`/Users/hardy/Work/mango-file-preview-e2e-20260526/.runtime/files`
- 预览缓存：`/Users/hardy/Work/mango-file-preview-e2e-20260526/.runtime/preview-cache`
- Office 插件：`office.plugin.enabled=false`

```bash
mysql -uroot -h127.0.0.1 -e "DROP DATABASE IF EXISTS mango_preview_e2e_20260526; CREATE DATABASE mango_preview_e2e_20260526 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

```bash
SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/mango_preview_e2e_20260526?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD='' \
KK_OFFICE_PLUGIN_ENABLED=false \
mvn -pl :mango-monolith-app -am spring-boot:run \
  -Dspring-boot.run.arguments='--server.port=7781 --mango.file.storage-type=LOCAL --mango.file.local.root-path=/Users/hardy/Work/mango-file-preview-e2e-20260526/.runtime/files --mango.file-preview.source-token-expire-seconds=86400 --mango.file-preview.cache-path=/Users/hardy/Work/mango-file-preview-e2e-20260526/.runtime/preview-cache --office.plugin.enabled=false'
```

```bash
mysql -uroot -h127.0.0.1 mango_preview_e2e_20260526 -e "UPDATE file_storage_config SET active = CASE WHEN storage_type = 'LOCAL' THEN 1 ELSE 0 END, updated_time = NOW(), updated_at = NOW() WHERE tenant_id = 1;"
```

```bash
VITE_ADMIN_PROXY_PATH=http://127.0.0.1:7781 VITE_PORT=5581 VITE_OPEN=false pnpm run dev -- --host 127.0.0.1
```

```bash
PLAYWRIGHT_BASE_URL=http://127.0.0.1:5581 \
PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:7781 \
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
pnpm exec playwright test e2e/specs/file-preview-types-live.spec.ts --project=chromium --reporter=list
```

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 新工作区启动验证 | 使用 `/Users/hardy/Work/mango-file-preview-e2e-20260526` 独立 worktree | 新 worktree 与专用分支 | `git worktree list`、`git status` | DONE | 本文档第 2 节 |
| TASK-002 | 用户要求 | 使用新数据库 | 创建 `mango_preview_e2e_20260526` 并完成 Flyway 初始化 | 新 MySQL 数据库 | 后端启动日志、`actuator/health` | DONE | 本文档第 6 节 |
| TASK-003 | 用户要求 | 自选端口启动后端 | 后端使用 `7781`，本地文件存储目录使用 `.runtime/files` | 运行中的 Java 服务 | `curl http://127.0.0.1:7781/actuator/health` | DONE | 本文档第 6 节 |
| TASK-004 | 用户要求 | 自选端口启动前端 | 前端使用 `5581`，代理到 `7781` | 运行中的 Vite 服务 | Vite 输出 `http://127.0.0.1:5581/` | DONE | 本文档第 6 节 |
| TASK-005 | 用户要求 | E2E 验证各种类型文件预览 | 编写聚焦文件预览的 Playwright 用例覆盖 `txt/png/pdf/zip/xlsx` | `mango-ui/apps/mango-admin/e2e/specs/file-preview-types-live.spec.ts` | `pnpm exec playwright test ... --project=chromium` | DONE | 本文档第 8 节 |
| TASK-006 | 用户要求 | 登记存在问题 | 按 P1-P4 记录缺陷、环境阻塞和构建警告 | 本验证报告 | 人工复核 E2E 输出与日志 | DONE | 本文档第 9 节 |
| TASK-007 | 验证限制 | Office 类型预览 | 当前机器未安装可用 LibreOffice，禁用 Office 插件启动，验证提示和下载链路 | xlsx 结果登记为环境阻塞 | E2E `xlsx.status=BLOCKED` | EXCEPTION | 本文档第 9 节 P2 |
| TASK-008 | 用户要求 | 优化文件管理预览弹窗 | 弹窗标题仅显示文件名，右上角统一放置新窗口预览、下载、关闭按钮，移除底部基础信息 | `FilePreviewPanel.vue`、`files/index.vue`、`file-management.spec.ts` | `pnpm -F mango-admin build`、文件管理 E2E | DONE | 本文档第 11 节 |
| TASK-009 | 用户要求 | 优化 zip 预览页面文案和右侧 header 展示 | 左侧标题从“压缩包目录”改为“目录”；右侧预览区仅 PDF 保留 `FileView + 文件名` header，非 PDF 内容隐藏 header 以减少重复标题 | `compress.ftl`、`file-preview-types-live.spec.ts` | 多类型文件预览 E2E 中校验 zip 布局、包内 PDF 和包内 txt 预览 | DONE | 本文档第 11 节 |

## 8. E2E 结果

| 类型 | 上传 | 预览入口 | 浏览器预览页 | 下载 | 结果 | 证据 |
|---|---|---|---|---|---|---|
| txt | 通过 | 通过 | 通过 | 通过 | PASS | 页面标题：普通文本预览；下载 `text/plain;charset=UTF-8` |
| png | 通过 | 通过 | 通过 | 通过 | PASS | 页面标题：图片预览；下载 `image/png;charset=UTF-8` |
| pdf | 通过 | 通过 | 通过 | 通过 | PASS | 页面标题：PDF预览；下载 `application/pdf;charset=UTF-8` |
| zip | 通过 | 通过 | 通过 | 通过 | PASS | 页面标题：压缩包预览；目录树已就绪；包内 `sample.pdf` 读取 `application/pdf;charset=UTF-8`；下载 `application/zip;charset=UTF-8` |
| xlsx | 通过 | 通过 | 显示不可预览提示 | 通过 | BLOCKED | 环境禁用 Office 插件，页面提示未启用 Office 转换能力 |

## 9. 问题登记

### P1 zip 压缩包目录读取失败

- 现象：zip 文件上传成功，预览入口生成成功，浏览器打开压缩包预览页成功，但页面显示“目录加载失败”“目录读取失败，请重试”“暂时无法读取压缩包目录结构，请稍后重试”。
- 影响：zip 无法完成在线预览目录浏览；用户只能下载文件查看。
- 证据：`mango-ui/apps/mango-admin/e2e/.tmp/file-preview-types-live-results.json` 中 `zip.status=FAIL`。
- 根因：压缩包预览页通过 `/directory` 读取目录树，但该接口未加入 Mango 文件预览公开资源与鉴权白名单，无登录态 iframe 请求被鉴权拦截。
- 处理：已将 `/directory` 加入 `FilePreviewSecurityCustomizer`、`FilePreviewPermitPathBeanPostProcessor`、`FilePreviewEngineResourceRegistrar`，并补充注册回归测试。
- 状态：DONE，E2E 已回归通过，页面目录树可渲染。

### P1 zip 压缩包内 PDF 文件无法预览

- 现象：zip 预览页可列出 `sample.pdf` 和 `readme.txt`，其中 txt 可预览，但点击包内 PDF 后浏览器没有完成 PDF 二进制读取。
- 影响：用户无法在压缩包预览页内直接查看 PDF，只能下载压缩包或单独下载文件。
- 证据：E2E 初始失败记录为 `zipInnerPdfOk=false`，诊断日志显示 PDF.js 内层 iframe 先被跨源 `X-Frame-Options: sameorigin` 拦截；改同源后又发现 `/pdfjs/build/pdf.mjs` 返回 404。
- 根因：
  - `pdf.ftl` 在前端 `/api` 网关访问场景下仍使用后端 `baseUrl` 生成 PDF.js iframe，导致 `5581` 页面嵌套 `7781` PDF.js，被浏览器同源 frame 策略拒绝。
  - PDF.js `web/viewer.html` 引用 `../build/pdf.mjs`、`../build/pdf.worker.mjs`、`../build/pdf.sandbox.mjs`，但仓内只提交了 `static/pdfjs/web`，缺少同版本 `build` 运行时资源。
- 处理：
  - `pdf.ftl` 增加运行时 `gatewayBaseUrl()`，当前页面经 `/api` 访问时使用 `window.location.origin + "/api/"` 生成 PDF.js 和文件读取 URL，保持预览 iframe 同源。
  - 补齐 PDF.js `5.4.530` 的 `static/pdfjs/build/*.mjs` 运行时资源，并在 `.gitignore` 中为该目录增加精确例外。
  - 新增 `PdfTemplateTest` 覆盖 `/api` 网关场景和 PDF.js build 资源存在性；E2E 增加 zip 内 PDF 点击与 `application/pdf` 响应断言。
- 状态：DONE，E2E 已回归通过，`zipInnerPdfOk=true`，包内 PDF 读取响应 `status=200, content-type=application/pdf;charset=UTF-8`。

### P2 新数据库默认启用 MinIO，MinIO 不可用时所有上传失败

- 现象：新库初始化后 `file_storage_config` 默认 active 配置为 `MINIO`，MinIO 未启动时所有文件上传返回 `3501 文件存储失败`。
- 影响：用户即使通过启动参数指定 `--mango.file.storage-type=LOCAL`，仍可能被数据库活动配置覆盖，导致新环境不可用。
- 证据：第一次 E2E 所有类型上传失败；数据库查询显示 `MinIO 本地联调 active=1`，切换 `LOCAL active=1` 后上传恢复。
- 处理：已新增 `mango-file-core` migration `V5__default_local_storage_active.sql`，新数据库初始化后默认激活 `LOCAL`，`MINIO` 保留为本地联调示例但不默认启用。
- 状态：DONE，`flyway_schema_history_file` 已记录 V5 成功执行，当前 `LOCAL active=1`、`MINIO active=0`。

### P2 Office 文档预览受本机 LibreOffice 环境阻塞

- 现象：本机没有可执行的 macOS LibreOffice；仓内 `LibreOfficePortable` 只有 Windows 可执行文件。启用 Office 插件时后端启动失败，禁用后 `xlsx` 显示“暂未启用 Office 转换能力”。
- 影响：Office 文件无法在当前机器完成在线转换预览，只能验证提示和下载链路。
- 证据：启动时启用 Office 插件报“找不到office组件”；E2E 中 `xlsx.status=BLOCKED`。
- 建议：为 macOS 开发验证补齐 LibreOffice 安装说明或容器化 Office 转换服务。

### P3 新工作区缺少 JAI system-scope jar 会阻断 Maven 构建

- 现象：新 worktree 初次 Maven 启动缺少 `jai_core-1.1.3.jar`、`jai_codec-1.1.3.jar`，需要从主工作区复制到 `mango-file-preview-engine/lib`。
- 影响：新工作区不可从 Git 内容直接复现启动。
- 证据：Maven 依赖解析失败后，手工补齐 jar 才能继续构建。
- 处理：已移除 `mango-file-preview-engine` 对 `lib/jai_*.jar` 的 `systemPath` 依赖，改为按 Maven 坐标解析 `javax.media:jai_core:1.1.3`、`javax.media:jai_codec:1.1.3`。
- 状态：DONE，目标 Maven 测试链路已通过，不再依赖 worktree 内 `lib/jai_*.jar`。

### P4 构建与运行警告

- Maven 警告：`org.redisson:redisson` dependencyManagement 重复声明，位置 `mango/pom.xml:463`。已删除重复声明。
- Maven 插件警告：`mango-maven-plugin` 若干 Maven 插件依赖 scope 建议为 `provided`。
- 前端警告：`build.terserOptions` 存在但未显式配置 `build.minify=terser`。
- pnpm 警告：安装时忽略若干依赖 build scripts。

## 10. 结论

本次验证完成了新工作区、新数据库、前后端启动和多类型文件预览 E2E。`txt`、`png`、`pdf`、`zip` 预览和下载通过；`xlsx` 因当前 Office 转换环境不可用，仅验证到清晰提示和下载链路。

## 11. 缺陷修复回归记录

- `/directory` 已加入文件预览公开资源与鉴权白名单，zip 压缩包目录读取不再被鉴权拦截。
- 新库文件存储默认状态已通过 `file` 模块 V5 migration 调整为本地存储启用、MinIO 禁用。
- JAI 依赖已移除 worktree 内 `systemPath` jar 约束，改为 Maven 坐标解析。
- `org.redisson:redisson` 重复 dependencyManagement 声明已删除。
- 独立 kkFileView 首页入口仍被拒绝：`/`、`/index` 返回 `404` JSON；预览入口和 `/directory` 保持可用。
- zip 压缩包内 PDF 预览已修复：`pdf.ftl` 在 `/api` 代理场景下保持 PDF.js 同源加载，`static/pdfjs/build` 资源已补齐。
- zip 压缩包预览页可视标题已从 `kkFileView` 调整为 `FileView`；后端重启后多类型 E2E 回归通过。
- zip 压缩包左侧标题已从“压缩包目录”调整为“目录”；包内非 PDF 文件预览时右侧外层 header 隐藏，PDF 文件仍保留 `FileView + 文件名` header。
- 文件管理预览弹窗已调整为顶部仅显示文件名，右上角统一显示新窗口预览、下载、关闭按钮；底部文件名、文件大小、内容类型、扩展名基础信息已移除。
- 文件管理 E2E 已补充弹窗按钮与基础信息移除断言；当前本地存储模式下 PNG 预览断言已改为校验当前存储访问地址可用，不再固定要求 MinIO 直链。
