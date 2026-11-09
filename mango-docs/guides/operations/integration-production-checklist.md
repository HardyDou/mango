# 集成应用模块投产核对清单

适用版本：`mango-integration`（连接器定义、凭证、连接实例、声明读取、调用客户端）。

## 1. 配置核对

| 项 | 要求 | 检查方式 |
|---|---|---|
| Crypto 密钥 | `mango.crypto.sm4.secret-key` 必须由环境变量注入，禁止使用仓库默认值 | 检查部署配置与 `MANGO_CRYPTO_SM4_SECRET_KEY` |
| 连接测试网络边界 | 默认 `mango.integration.test.allow-private-network=false`；确需内网目标时才开启 | 检查配置；开启需安全评审 |
| 数据库账号 | 应用账号只具备所需表读写权限；外部只读账号不得访问 `integration_credential` | 检查 DB 授权 |
| 数据库备份 | 备份加密；证书以文本入库，备份即包含证书 | 检查备份策略 |
| Flyway | `mango.persistence.flyway.modules.integration.enabled=true` | 启动日志确认 integration 模块迁移成功 |
| 租户排除 | `integration_connector` 在 `excludedTables` 中 | 检查 `PersistenceProperties` 生效配置 |

## 2. 数据与声明核对

| 项 | 要求 |
|---|---|
| 连接器来源 | 生产连接器应为 `source=DECLARED`，由声明文件维护 |
| 声明文件 | `integration-connectors.json` 随代码版本发布，变更需走代码评审 |
| 页面维护连接器 | 仅允许 `source=MANUAL` 的连接器在页面编辑 |
| 凭证 | 生产凭证由运维/业务方录入，Secret 只写不读；证书文本可读但接口返回掩码 |
| 权限 | 写操作仅授予平台管理员角色：`integration:connector:edit`、`integration:credential:add/rotate`、`integration:instance:add/edit`、`integration:maintenance:cleanup` |

## 3. 发布前回归

```bash
mvn -f mango/pom.xml -pl :mango-integration-core,:mango-integration-support test
mvn -f mango/pom.xml -DskipTests verify
pnpm --filter mango-admin build
pnpm --filter mango-admin test:e2e -- e2e/specs/integration-application-management.spec.ts
```

预期：测试全绿、`blockingIssues = 0`、E2E 3 条通过。

## 4. 上线后巡检

| 频率 | 检查项 |
|---|---|
| 每日 | 连接器停用数量、实例 `connectorEnabled=false` 数量 |
| 每日 | 凭证 `expiresAt` 临近 7 天的清单（当前需人工查询，无主动告警） |
| 每周 | `integration_call_log` 失败率、`integration_credential_access_log` 异常读取 |
| 每月 | 按保留天数手动清理历史日志（页面「清理日志」或 `POST /integration/maintenance/cleanup`） |

## 5. 回滚

| 场景 | 处理 |
|---|---|
| 连接器声明错误 | 回退声明文件版本并重新发布，或先停用连接器 |
| 凭证录入错误 | 使用「轮换凭证」覆盖，旧版本保留在轮换记录 |
| 实例配置错误 | 停用实例后修正配置 |
| 数据异常 | 按备份恢复 `integration_*` 表；恢复前确认凭证版本 |

## 6. 已知限制

- 凭证到期无主动提醒，需人工巡检
- 证书以文本明文入库，依赖数据库访问控制与备份加密
- 系统字典值校验依赖 `DictApi`，字典服务不可用时放行
- 无删除能力，只能停用
- 无回调订阅与验签编排，回调仍由业务域实现
