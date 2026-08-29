# Mango Infra Crypto 历史债务修复验收证据

## 1. 验收范围

- 页面：支付中心 / 签约通道，验证真实新增、搜索、编辑、回显和删除。
- 接口：登录、文件上传、签约通道 POST/GET/PUT/DELETE、操作审计查询和文件清理。
- 权限：租户 `1` 的 `admin` 账号，经真实登录和既有支付权限访问。
- 数据：全新隔离数据库 `mango_dev_mango_infra_crypto_debt_182`，先验证正式资源，再启用 demo 资源满足支付 E2E 前置数据。
- 部署形态：单体后端 `127.0.0.1:18182`、管理端 `127.0.0.1:30182`、Chromium。
- Remote 适配器：不适用；Crypto 是本地 JVM 能力模块，不声明 `starter-remote`、HTTP API、Controller 或 Feign。已确认本次不涉及 POM-only remote、聚合 API、选择性 Feign 覆盖或 Service 充当 HTTP adapter。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30182`
- 后端地址：`http://127.0.0.1:18182`，Actuator health 为 `UP`。
- 数据库或租户：隔离 MySQL 数据库 `mango_dev_mango_infra_crypto_debt_182`，租户 `1`。
- 测试账号：`admin`；证据不记录密码、Token 或 SM4 密钥。
- 浏览器：Playwright Chromium，单 worker 串行执行。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | Crypto 单元测试 | SM2、SM3、SM4、HMAC-SM3 与公开兼容契约 | 官方/已知向量、非法密钥、CBC/ECB | 57 个测试全部通过；非法密钥完整异常链不泄露输入；公开构造器、接口签名与 `mango.crypto` 前缀不变 | 非 UI 用例；验证算法及公开契约 | 纯 JVM 单元测试，不产生浏览器 console/network；Surefire 无失败或错误 | Maven Surefire 摘要：57/57 PASS | PASS |
| TASK-002 | TC-002 | Payment 真实消费者 | Payment 敏感值通过真实 `Sm4CryptoService` 加解密 | 随机测试明文 | `enc:` 密文不含明文、可回解、重复 encode 幂等；既有日志与签约服务合同不变 | 非 UI 用例；验证真实服务消费 | Spring/H2 接口服务测试，不产生浏览器 console/network；Surefire 无失败或错误 | Maven Surefire 摘要：10/10 PASS | PASS |
| TASK-003 | TC-003 | `/api/payment/channel-contracts` 与签约通道页面 | 真实创建、数据库密文、详情脱敏、编辑保密、搜索回显、删除和审计 | 动态商户号、动态 AppId、动态 secret、真实上传证书文件 | 创建及编辑后数据库 `apiSecret` 均以 `enc:` 开头且不含原文；详情和编辑页只返回 `******`；编辑 AppId 后 secret 保持；删除后列表不可查且审计为 SUCCESS | 列表显示商户号和微信扫码/电脑网页能力；编辑弹窗回显脱敏值和费率；保存、关闭、删除提示均成功 | console error=0、pageerror=0、Payment requestfailed=0、HTTP 5xx=0 | 通过态截图（历史验收图片已清理（可从 Git 历史恢复））；Chromium 1 passed，测试体 4.9s | PASS |
| TASK-004 | TC-004 | 架构与静态门禁 | Crypto partial Reactor 全模式扫描 | `mango-infra-crypto` 及架构验证聚合器 | dependency=0、ArchUnit=0、PMD=0、blocking=0；Checkstyle/SpotBugs/PMD total/new/baseline/excluded/toolFailure 均为 0 | 非 UI 用例；检查源码、字节码、依赖和静态规则 | 非浏览器检查；Maven 工具失败数为 0 | 架构规则单测 124/124 PASS；scoped verify BUILD SUCCESS | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-infra-crypto` | 不适用 | 算法已知向量和密文格式兼容 | 配置绑定失败和非法密钥异常链 | 不适用 | Surefire 57/57 | PASS |
| `mango-payment` | 支付中心 / 签约通道 | 真实 Crypto 消费与数据库密文 | 脱敏编辑、页面回显、删除审计 | 标签、弹窗、输入值、成功提示均断言 | 历史验收图片已清理（可从 Git 历史恢复） | PASS |
| 架构规则 | 不适用 | `@LocalCapabilityContract` 仅放行 infra 本地能力 | 标记移出 infra 后仍 fail-closed | 不适用 | JUnit 124/124 | PASS |

## 5. 基线对比

| 指标 | 修复前 | 修复后 | 结论 |
|---|---:|---:|---|
| Crypto 单元测试 | 48 | 57 | 新增国密已知向量、安全异常链和公开兼容契约，全部通过 |
| Payment 消费者测试 | 9 | 10 | 新增真实 SM4 消费者测试，全部通过 |
| Crypto 架构阻断项 | 37（ArchUnit 12、PMD 25） | 0 | 全部清零 |
| Crypto 静态问题 | 20（Checkstyle 13、SpotBugs 7） | 0 | 全部清零 |
| Chromium 签约通道 E2E | 原用例无数据库密文断言 | 1 passed | 增加 DB 密文、编辑后密文及运行时错误断言 |

## 6. 验证命令

- Crypto：`mvn -f mango/pom.xml -pl mango-infra/mango-infra-crypto ... test`
- Payment 消费者：`mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -Dtest=PaymentChannelContractServiceIntegrationTest,PaymentSensitiveLogContractTest,PaymentSensitiveValueCodecCryptoIntegrationTest ... test`
- 架构规则：`mvn -f mango/pom.xml -pl mango-tools/mango-architecture-rules -Dtest=MangoArchUnitCheckerTest,MangoJavaArchitectureRuleTest ... test`
- Crypto scoped gate：`mvn -f mango/pom.xml -pl :mango-infra-crypto,:mango-architecture-verification -DskipTests -Dmango.architecture.requireFullReactor=false -Dmango.architecture.mode=full -Dmango.check.baseRef=origin/main ... verify`
- Chromium：`PAYMENT_E2E_ALLOW_SHARED_DB_MUTATION=true ... playwright test e2e/specs/payment-center.spec.ts --project=chromium --grep '签约通道按字段模板和签约能力真实保存、回显和删除审计可用'`
- 测试质量：`node mango-pmo/tools/test-quality-check.mjs --base origin/main`，11 个变更测试文件 PASS。
- 工作区：`node mango-pmo/tools/workspace-layout-check.mjs --root .`，PASS。

## 7. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 全仓 Reactor | 用户明确要求不重复全仓检查；本次使用官方 partial-reactor 参数只扫描直接改动模块 | 不对无关模块作全仓无债务声明 | 主干定时/手工完整 Reactor 继续承担全仓盘点 | 已明确 |
| 外部支付网关 | Crypto 改动只影响本地敏感值编解码；本用例使用 Mango Pay 内置通道，不请求第三方生产网关 | 不对第三方商户连通性作结论 | 真实商户联调由支付发布验收独立执行 | 不适用 |

## 8. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango/业务开发者 | Crypto 公开 String API、CBC `Base64(IV + ciphertext)` 格式和 `mango.crypto` 配置前缀保持不变；Payment 可继续使用既有敏感配置接口 | 本报告及模块测试 | 上述 Maven/Playwright 定向命令 | 使用隔离数据库和非生产密钥；不得复用证据环境凭据 | 密钥非法时只返回不含密钥材料的业务异常；新问题按 Mango Issue 流程登记 | PASS |

## 9. 能力说明影响

- 本次为兼容性保持的内部重构、安全加固和测试补充，公开 API、配置、密文格式、初始化方式与业务接入方式均未改变，因此模块 README 与能力地图无需修改。
- `LocalCapabilityContract` 的适用边界被澄清，长期规则只更新于 `mango-pmo/rules/backend/03-api.md`，并同步 `rules/index.json` 与业务 PMO 投影。
