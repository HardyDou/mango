# Issue #765 自动化与接口验证

## 环境

- 日期：2026-08-13，Asia/Shanghai
- Worktree：`mango-765-notice-inbound`
- 分支：`feat/765-notice-inbound`
- Workspace ID：`mango_038`
- 后端：`http://127.0.0.1:18038`
- 前端：`http://127.0.0.1:30038`
- 数据库：`mango_dev_mango_765_notice_inbound_038`
- File 并发专项数据库：`mango_dev_mango_765_notice_inbound_038_concurrency`
- 测试租户：`tenantId=1`、`tenantCode=default`；测试账号标识为初始化管理员，不记录密码或 Token。
- Node：`26.5.0`。项目要求 `>=22.23.1 <23`，构建与测试虽通过但存在非阻断 engine 警告。

工作区由项目内 CLI 读取并验证：

```bash
node mango-ui/packages/mango-cli/src/index.mjs workspace status
node mango-ui/packages/mango-cli/src/index.mjs dev doctor
```

结果：初始化来源明确为 `mango workspace init`，workspace JSON、env、端口和数据库一致，数据库存在。所有 Maven 验证均加载 `.mango/dev-workspace.env`；不把此前未加载 workspace env 的执行算作有效验证。

## Maven 验证

加载工作区环境后，对 Notice 受影响链及消费者执行 Reactor verify：

```bash
set -a
. ../.mango/dev-workspace.env
set +a

MANGO_DB_NAME="${MANGO_DB_NAME}_concurrency" mvn \
  -pl mango-platform/mango-notice/mango-notice-api,\
mango-platform/mango-notice/mango-notice-support,\
mango-platform/mango-notice/mango-notice-core,\
mango-platform/mango-notice/mango-notice-channel-email,\
mango-platform/mango-notice/mango-notice-channel-wecom,\
mango-platform/mango-notice/mango-notice-starter,\
mango-platform/mango-notice/mango-notice-starter-remote \
  -am verify
```

此前基线 Reactor 验证结果为 66 个模块全部 `SUCCESS`；本次修复后的新增定向验证为：

| 模块 | 测试 | 失败 | 错误 | 跳过 |
|---|---:|---:|---:|---:|
| mango-file-core | 67 | 0 | 0 | 0 |
| mango-notice-core 入站定向集成 | 5 | 0 | 0 | 0 |
| mango-notice-starter | 23 | 0 | 0 | 0 |
| mango-notice-starter-remote | 4 | 0 | 0 | 0 |
| mango-notice-channel-email | 13 | 0 | 0 | 0 |
| mango-notice-channel-wecom | 10 | 0 | 0 | 0 |

File 并发专项在专用数据库执行 3 项测试，0 失败；附件集成测试验证 File Service 落盘、租户和 PRIVATE 属性，以及广播只携带 File ID。

本次在当前 worktree 直接执行 `mvn -pl mango-platform/mango-notice/mango-notice-core verify` 时，编译通过，入站定向用例通过；模块既有渠道资源 H2 测试因测试建表未包含新增 `capability_mode` 列而有 7 个错误。该失败与本次失败字段清理无关，不能把本次模块 `verify` 宣称为全绿。

## 前端与质量检查

```bash
cd mango-ui
pnpm --filter @mango/notice build
pnpm --filter @mango/notice test
```

结果：构建成功；10 个测试文件、38 项测试通过。Node engine 警告见环境说明。

以下检查通过：

```bash
node mango-pmo/tools/test-quality-check.mjs --base origin/main
node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD
```

- 测试质量：15 个变更测试文件通过。
- 后端测试替身审计：block 0，warn 0。
- 模块 README、README 源事实、能力文档检查通过；能力检查报告 84 个变更文件。

## HTTP 与数据库实测

后端健康检查返回 `UP`，DB 组件为 `UP`。

完全不带 Cookie 和 Authorization 的请求结果：

| 入口 | HTTP | 业务结果 | 判定 |
|---|---:|---|---|
| GET `/notice/inbound-callbacks/public` | 200 | 不存在的测试 `channelConfigId` 返回业务 `code=400` | 匿名请求进入业务层，未被认证拦截 |
| POST `/notice/inbound-callbacks/public` | 200 | 不存在的测试 `channelConfigId` 返回业务 `code=400` | 匿名请求进入业务层，未被认证拦截 |
| POST `/notice/inbound-mail-callbacks/public` | 200 | 不存在的测试 `channelConfigId` 返回业务 `code=400` | 匿名请求进入业务层，未被认证拦截 |
| GET `/notice/inbound-messages` | 401 | Unauthorized | 管理入口保持认证保护 |

框架会把部分业务错误包装为 HTTP 200；以上公网请求只证明匿名可达和业务校验生效，不代表消息成功接收。

使用 Notice 正式 POP3 轮询和管理员登录态回读：

- `GET /notice/inbound-messages?pageNum=1&pageSize=10` 返回 HTTP 200、业务 `code=200`，列表包含三封真实管理员入站回复，状态均为 `BROADCASTED`。
- `GET /notice/inbound-messages/detail?id=2087866808973164545` 返回 `bodyText`、`bodyHtml`，均包含 `2222`；附件返回 `fileId=2087866809128353794`、文件名 `1.pdf`、状态 `SAVED`。
- 数据库现场：消息 `2087866808973164545` 为 `BROADCASTED`；附件表记录 `file_id=2087866809128353794`、大小 `153578`、状态 `SAVED`；File 记录为 `biz_type=NOTICE_INBOUND_MESSAGE`、`purpose=notice-inbound-attachment`、`access_level=PRIVATE`，业务表未保存文件 URL。
- 广播事件 `fc2b36fb-d9a7-3fc3-9a37-12a047c0a29a` 的 Outbox 状态为 `SUCCESS`，KV payload 长度 `877`，只含轻量消息元数据与 `fileIds`，不含正文、HTML、二进制、URL 或 Secret。
- 游标 `channelConfigId=2087860192278274050` 已推进至 POP3 UIDL，`last_failure_code`、`last_failure_reason` 均为空。

旧记录 `2087860225505550338` 曾因修复前 KV payload 超限留下失败摘要，后续已广播成功；本地验收库已清理该历史脏字段，当前状态为 `BROADCASTED` 且失败字段为空。
