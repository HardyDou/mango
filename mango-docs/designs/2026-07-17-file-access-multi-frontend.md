# 多前端文件访问标准交付记录

## 1. 元数据

- 任务 ID：FILE-ACCESS-MULTI-FRONTEND-20260717
- 交付模式：STANDARD
- 需求影响：L2 - 文件预览和下载是多个前端共享的可观察访问契约。
- 方案风险：L2 - 同时涉及反向代理、运行时文件配置和对象存储直连兼容。
- 最终风险：L2
- 工作区决策：CREATE（创建后持续复用 `fix/file-access-multi-frontend`）

## 2. 目标与范围

- 目标：三套独立前端均通过自身同源 `/api` 访问 Java 文件服务，同时保留 MinIO/S3 直连模式。
- 成功条件：PROXY 地址包含当前前端 origin 和 `/api`；DIRECT 地址保持对象存储签名的 host、port、path 和 query；三套前端之间无需互相跨域访问。
- 处理范围：文件访问默认配置、代理地址组装验证、MinIO/S3 签名验证、Nginx 部署示例和文件模块说明。
- 不处理范围：不代理或重签 MinIO 的 DIRECT URL；不修改业务表中的文件 ID 模型；不替部署环境写入具体域名、密钥或桶策略。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 8081/8082/8083 独立前端 | 浏览器请求各自 `/api/file/**` | Nginx 去掉 `/api` 后转发后端，并传递当前 origin 与前缀 | 返回缺少 `/api` 或指向另一前端时不可预览/下载 | 三个入口分别返回自身 origin 下的 `/api/file/**` 地址 |
| AC-002 | PROXY 文件访问 | `accessMode=PROXY` 且 `public-base-url` 为空 | Java 服务流式读取存储对象，浏览器只访问同源 API | 浏览器直连后端或对象存储会产生跨域问题 | 预览和下载均不需要浏览器 CORS |
| AC-003 | MinIO/S3 DIRECT 文件访问 | `accessMode=DIRECT` 且配置稳定 `publicEndpoint` | 返回由公开 endpoint 参与签名的原始 URL | 修改 host、port、path 或 query 会使签名失效 | 返回 URL 使用 `publicEndpoint` 且包含原签名参数 |
| AC-004 | 新平台部署默认配置 | 首次同步 `FILE_SETTINGS` | 运行时配置初始化为 PROXY，DIRECT 仍可显式选择 | 默认 DIRECT 会把跨域与 CORS 责任暴露给浏览器 | 默认资源和数据库列默认值均为 PROXY，升级不覆盖已有选择 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001, AC-002 | 每个前端 Nginx 均提供 `/api/`，使用带尾斜杠的 `proxy_pass` 去掉前缀，并覆盖传入 `X-Forwarded-*` | `mango-ui/deploy/nginx/**` | 恢复 Nginx 配置并重新加载 |
| TD-002 | AC-001 | `mango.file.public-base-url` 在多前端 PROXY 场景保持为空，由请求转发头动态组装 origin | 文件模块配置说明 | 显式配置单一外部基准地址 |
| TD-003 | AC-003 | DIRECT URL 不添加 `/api`，S3 presigner 继续使用 `publicEndpoint`；浏览器跨域由 MinIO bucket CORS 管理 | 文件存储实现测试与 README | 切回 PROXY |
| TD-004 | AC-004 | 新平台部署由默认资源和数据库列默认值使用 PROXY；默认资源使用 `INIT_ONLY`，升级时保留现有租户及 YAML 的 PROXY/DIRECT 选择 | 文件资源、migration | 显式设置 `accessMode=DIRECT` |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-004 | 1 | `mango-file` 资源、migration | 新平台运行时默认值和说明一致，资源测试通过 |
| TASK-002 | TD-001, TD-002 | 2 | Nginx 示例、URL 组装测试 | 三个前端 origin 均生成同源 `/api` 地址 |
| TASK-003 | TD-003 | 3 | S3/MinIO 签名测试、README | DIRECT URL 保持公开 endpoint 和签名 |
| TASK-004 | 全部 | 4 | 定向构建、测试、API/浏览器验证 | 验收映射有真实结果和证据 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001 | M10 单元测试、M13 浏览器验证 | `mvn test`；Playwright 分别访问 8081、8082、8083 的 `/api/file/files/preview-content` | PASS | 三个响应均去掉 `/api` 转发，并分别携带对应 Host、Port 和 `X-Forwarded-Prefix=/api`；截图位于 `.runtime/file-access-browser/8081.png`、`8082.png`、`8083.png` |
| AC-002 | M12 API 验证 | 通过真实前端 `/api/file/files/preview-content` 和 `/download` 验证文件内容 | BLOCKED | Mango CLI 安装被主干既有 `MANGO-ARCH-CTRL-008` 阻断，真实后端未启动 |
| AC-003 | M11 集成测试 | `mvn test` 中的 `S3CompatibleFileStorageTest` | PASS | 签名 URL 使用 `publicEndpoint` 的 host/port/path，并保留 `X-Amz-Signature` |
| AC-004 | M09 静态验证、M11 资源与 migration 验证 | 文件核心模块 `mvn test`；真实 MySQL 顺序执行 V1、V2 后查询列默认值 | PASS | 45 项测试全部通过；`file_settings.access_mode` 默认值为 `PROXY`；starter package 成功 |

## 7. 例外与剩余风险

- 真实 Mango 后端启动受最新主干既有架构门禁阻断，未完成带鉴权、真实文件和真实 MinIO 的 API/浏览器现场验收。已完成三端口 Nginx 浏览器代理契约、文件模块全量测试、真实 MySQL migration 和 MinIO 签名生成验证。
- 临时浏览器回显环境没有静态站点根目录，因此控制台有一条 `favicon.ico 500`；目标文件 API 请求均成功，正式部署需使用真实前端构建目录。
