# 标准交付记录：Issue #1074 公开表单提交防刷

## 1. 元数据

- 任务 ID：Issue #1074
- 交付模式：STANDARD
- 需求影响：L2 - 公开提交接口的错误语义、限流和验证码行为发生变化，影响匿名用户与服务端接入方。
- 方案风险：L2 - 保护逻辑位于写库事务前，依赖共享 KV 与验证码服务，失败策略和重试行为需要兼容多实例部署。
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-issues-1074-1076`
- 评审选择：AGENT_ONLY；未指定外部同行评审，保留后续复核入口。

## 2. 目标与范围

- 目标：为 `POST /information/open/forms/submit` 增加风险触发式验证码和共享固定窗口限流。
- 成功条件：正常请求直接提交；风险请求要求一次性验证码；超过硬上限返回 429 和 `Retry-After`；所有拒绝请求不写入记录。
- 处理范围：Information starter、Information API 错误码、模块 README、技术设计和测试证据。
- 不处理范围：不新增验证码实现；不改变表单数据模型、幂等语义、Resource API 声明或 #1075/#1076 资源所有权逻辑。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1074-01 | 匿名浏览器/客户端提交公开表单 | 请求低于风险阈值 | 直接调用提交服务并返回成功 | 表单自身校验失败按原有业务错误返回 | 不强制验证码 |
| AC-1074-02 | 风险客户端 | 达到风险阈值且未携带验证码 | 不调用提交服务 | HTTP 428，返回验证码必需错误码 | 不写入采集记录 |
| AC-1074-03 | 风险客户端 | 携带现有验证码凭据重试 | 验证成功后继续提交 | 验证失败 HTTP 400 | 原请求体可重试，验证码一次性使用 |
| AC-1074-04 | 高频客户端 | 固定窗口超过硬上限 | 在提交事务前拒绝 | HTTP 429，返回 `Retry-After` | 不写入采集记录 |
| AC-1074-05 | 多实例部署 | 使用共享 KV 计数 | 各实例使用同一限流窗口 | KV 故障按 `fail-open` 配置处理 | IP 或 IP+表单维度可配置 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-1074-01 | AC-1074-01/05 | 复用 `IKvStore.increment`，默认按 IP+表单建立 SHA-256 业务键；窗口、风险阈值、硬上限和维度配置化 | `InformationSubmitProtection`、Information starter | 关闭 `mango.information.public-submit.protection.enabled` |
| DEC-1074-02 | AC-1074-02/03 | 复用 `CaptchaApi`；保持提交 JSON 不变，风险重试通过 `X-Captcha-Key`、`X-Captcha-Code` 等请求头传递 | `InformationSubmitProtection`、`InformationPublicController` | 关闭保护配置；不保留第二套提交路径 |
| DEC-1074-03 | AC-1074-04 | 429 携带 `Retry-After`，公开提交编排器在调用 `IInformationSubmissionService` 前拒绝请求；Controller 只返回成功结果，拒绝由专用异常适配器映射 | Controller、公开提交编排器、异常适配器、API 错误码和测试 | 回滚 starter 保护 bean 与错误码变更 |
| DEC-1074-04 | AC-1074-02/03 | `serverSecret` 仍只允许服务端使用；浏览器不得暴露密钥 | README 接入说明 | 仅通过服务端调用，不提供浏览器兼容兜底 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-1074-01 | DEC-1074-01 | 1 | `mango-information-starter` 配置与保护实现 | 编译通过，KV 窗口和配置生效 |
| TASK-1074-02 | DEC-1074-02/03 | 2 | Controller、公开提交编排器、异常适配器、错误码、Captcha 头处理 | 428/400/429/503 语义可验证，429 返回 Retry-After，架构门禁通过 |
| TASK-1074-03 | DEC-1074-04 | 3 | `mango-information/README.md` | 浏览器与服务端接入流程可按文档执行 |
| TASK-1074-04 | 全部 | 4 | 测试与 `mango-docs/designs/issue-1074-public-submit-protection` | 自动化验证和证据记录完成 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1074-01/02/03/05 | 单元测试 | `mvn -pl mango-platform/mango-information/mango-information-starter -am -Dtest='*Information*ProtectionTest' -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test` | PASS | `InformationSubmitProtectionTest` |
| AC-1074-04 | API/Controller 测试 | `mvn -pl mango-platform/mango-information/mango-information-starter -am -Dtest='*Information*' -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test` | PASS | `InformationPublicControllerTest` |
| 编译与架构质量 | Maven 验证 | `mvn -pl mango-platform/mango-information/mango-information-starter -am verify` | PASS | Maven reactor 输出 |
| KV/测试物料专项 | Mango 检查 | `mvn -f mango/pom.xml -pl :mango-architecture-verification io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check -Drule=test-fixture -DbaseDir="$PWD" -Dmango.check.changedOnly=true -Dmango.check.gate=no-new-violations -Dmango.check.baseRef=origin/main` | PASS | Mango test-fixture 输出 |
| CI 等价 Java 门禁 | Maven 架构与静态质量 | `mvn -f mango/pom.xml -pl :mango-architecture-verification,mango-platform/mango-information/mango-information-api,mango-platform/mango-information/mango-information-starter -DskipTests -Dmango.architecture.skip=false -Dmango.architecture.mode=changed -Dmango.architecture.requireFullReactor=false -Dmango.architecture.base=1ebacb14084ca0795123b5b0cbda00bc2fc0f49b -Dmango.check.changedOnly=true -Dmango.check.gate=no-new-violations -Dmango.check.baseRef=1ebacb14084ca0795123b5b0cbda00bc2fc0f49b verify` | PASS | `target/mango-architecture-report.json`、`target/mango-static-report.json` |
| 能力说明 | 文档检查 | `git diff --check`；PR 能力文档门禁 | 待最终 PR head 回读 | 模块 README、技术设计、验收证据 |

## 7. 例外与剩余风险

- 默认 `fail-open=true`：KV 故障时优先保持公开提交可用，安全强度降低；生产环境可配置为 `false` 返回 503。
- 未执行真实生产级 Redis、多实例和外部验证码服务联调；当前由共享 KV 接口、保护逻辑单测和 Controller API 测试证明契约，生产部署需按配置完成运行态验证。
- #1075/#1076 不属于本 PR，继续等待资源所有权方案确认。
