# 文件存储访问入口回归报告

## 验收对象

- 分支：`feature/file-storage-access-entry`
- Worktree：`.mango/worktrees/file-storage-access-entry`
- 后端：`http://127.0.0.1:18577`
- 前端：`http://127.0.0.1:8267`
- 数据库：`127.0.0.1:3306/mango_dev_a84b18`
- 验收时间：`2026-06-09 09:53:15 CST`

## 业务断言

- `LOCAL` 存储有 `publicEndpoint` 时，公开访问 URL 使用 local 存储配置入口。
- `LOCAL` 存储无 `publicEndpoint` 时，Java 层通过 `/file/local-objects/{bucket}/...` 注册本地对象访问入口，并可按 `mango.file.public-base-url` 或 `X-Forwarded-*` 外部化。
- `MINIO/S3` 公开访问 URL 优先使用 `publicEndpoint`，无 `publicEndpoint` 时使用该存储配置的 `endpoint`，并按 `sslEnabled` 补协议。
- `MINIO/S3` path-style 配置会把 bucket 放入浏览器访问路径。
- 非 `LOCAL` 的直连 URL 不会被 Java 服务改写成 `mango.file.public-base-url` 或反向代理地址。
- 上传、详情、预览、批量上传响应中的运行时访问地址只用于响应展示，不写入业务数据。

## 执行命令

```bash
mvn -q -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -am test -Dcheckstyle.skip=true -Dtest=FileAccessUrlAssemblerTest,LocalFileStorageTest,S3CompatibleFileStorageTest -Dsurefire.failIfNoSpecifiedTests=false
```

结果：`PASS`

```bash
scripts/dev-workspace.sh start
```

结果：`PASS`，后端和前端均已启动。

```bash
curl -sS -o /tmp/mango-health-file-storage.txt -w '%{http_code}' http://127.0.0.1:18577/actuator/health
```

结果：`200`

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:8267 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18577 pnpm -F mango-admin exec playwright test e2e/specs/file-upload-url-contract.spec.ts --config playwright.config.ts --project chromium
```

结果：`PASS`，`1 passed`。

## 真实场景覆盖

- 使用真实后端服务和真实本地数据库登录 `admin/admin123`。
- 通过真实 `/file/files` 上传文本文件。
- 通过真实 `/file/files/detail` 查询上传记录。
- 通过真实 `/file/files/preview` 获取预览响应。
- 通过真实 `/file/files/batch` 执行批量上传。
- 请求头带 `X-Forwarded-Proto/Host/Port/Prefix`，验证 Java 代理/local 入口外部化。

## UI 与截图

本次验证为 API contract E2E，不涉及页面视觉变更。没有新增 UI 截图；Playwright 失败产物未作为最终证据提交。

## 风险

- 未连接真实 MinIO/S3 服务执行上传下载，相关对象存储入口以单元测试覆盖配置生成规则；真实对象存储连通性仍依赖环境中的 `file_storage_config` 和对象存储服务可达性。
- `ALIYUN_OSS`、`TENCENT_COS`、`QINIU_KODO` 沿用统一公开入口生成方法，未在本轮连接真实云服务做 SDK 级验收。
