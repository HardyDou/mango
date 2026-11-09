# 集成应用管理投产前真实验证

## 1. 验证环境

| 项 | 值 |
|---|---|
| 后端 | `http://127.0.0.1:18021`（`mango-monolith-app`，本地工作区） |
| 前端 | `http://127.0.0.1:30021`（`mango-admin`） |
| 数据库 | `mango_dev_mango_app_access_management_021`（MySQL） |
| 账号 | `admin` / 内部管理员 |
| 部署形态 | 单租户 |

## 2. 能力验证结果

| 能力 | 验证方式 | 实际结果 | 结论 |
|---|---|---|---|
| 健康检查 | `GET /actuator/health` | `UP` | PASS |
| 未登录访问 | `GET /integration/connectors/page` 无令牌 | `401` | PASS |
| 连接器定义资源声明同步 | 删除 `wecom-message` 后重启，读取列表 | 自动重建为 `MESSAGE` / 启用 | PASS |
| 未声明配置项拦截 | 实例提交 `unknownField` | `400 配置包含未声明的字段：unknownField` | PASS |
| 配置正则校验 | 实例提交 `ftp://bad` | `400 配置格式不符合要求：apiBaseUrl` | PASS |
| 凭证保存（密钥 + 证书） | 提交 `corpSecret` 与 `platformCert` | `200`，库内 `corpSecret` 为 `enc:` 密文，`platformCert` 为 PEM 明文 | PASS |
| 凭证列表脱敏 | `GET /integration/credentials` | `corpId=wx-verify,corpSecret=******,platformCert=******` | PASS |
| 凭证轮换 | `POST /integration/credentials/rotate` | `200`，版本 2 | PASS |
| 轮换记录 | `GET /integration/credentials/rotations` | `[(1, 'corpId,corpSecret,platformCert'), (2, 'corpSecret')]` | PASS |
| 实例创建与启用 | `POST /integration/instances` | `200`，状态 `ENABLED` | PASS |
| 回调路径生成 | `GET /integration/instances/detail` | `/integration/callback/wecom-message/verify-instance` | PASS |
| 连接器停用后启用实例 | 停用连接器后调用 `change-status` | `400 连接器已停用，不能启用实例` | PASS |
| 过期凭证状态 | 保存 `expiresAt` 为过去时间 | 列表返回 `EXPIRED` | PASS |
| 过期凭证启用拦截 | 用过期凭证创建启用实例 | `400 凭证已过期，请先轮换` | PASS |
| 连接测试（无认证实例） | 目标为 `http://127.0.0.1:18021/actuator/health` | `success=true`，`statusCode=200`，`elapsedMs=5` | PASS |
| 调用记录 | `GET /integration/instances/call-logs` | 返回 `GET /actuator/health 200 5ms 成功` | PASS |
| 凭证读取审计 | 连接测试触发 SPI 读取后查库 | `verify-credential / READ / corpId,corpSecret,platformCert / operator=1` | PASS |
| 页面主流程 | Playwright E2E | 应用定义查看、可视化新增连接器、动态表单创建实例 3 条通过 | PASS |
| 后端单元与场景测试 | `mvn -pl :mango-integration-core,:mango-integration-support test` | 21 条通过（模型 10、真实场景 3、认证工具 5、调用客户端 3） | PASS |
| 架构与质量门禁 | `mvn -f mango/pom.xml -DskipTests verify` | `blockingIssues = 0` | PASS |

## 3. 已知限制（不阻塞登记类投产）

| 限制 | 影响 | 处理 |
|---|---|---|
| 系统字典值仅前端限制，后端未按字典校验 | 直接调 API 可写入字典外的值 | 后续接入字典服务校验 |
| 业务域尚未接入声明 | 目前只有连接测试使用声明与凭证 | 由 notice/payment 按需接入 `IntegrationClient` |
| 无回调订阅与验签编排 | 企业微信/支付回调仍由业务域自行实现 | 后续独立设计 |
| 无删除能力 | 只能停用 | 需要归档需求 |
| 无实例级 IP 白名单 | 支付下游接入场景需要 | 后续补充 |

## 4. 结论

已完成的能力在真实数据库、真实接口、真实页面下可运行，可投产范围为：

```text
连接器定义（资源声明 + 页面维护）
凭证托管（多字段、加密、证书文本、版本、轮换、到期、读取审计）
连接实例（配置校验、能力授权、回调路径、启停、连接测试）
调用能力（声明 SPI、认证工具类、统一调用客户端、调用记录）
```

不可对外宣称：第三方回调接入、接口级授权、多租户运行时、开放 API 网关。
