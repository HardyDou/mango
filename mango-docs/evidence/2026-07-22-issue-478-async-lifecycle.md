# 标准交付记录

## 1. 元数据

- 任务 ID：#478
- 交付模式：STANDARD
- 需求影响：L2 - SSE 公共服务入口的超时与断开失败语义、日志可观测性受影响
- 方案风险：L2 - 共享 Web 异常解析边界改变，需保持普通 JSON 异常契约兼容
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-478`

## 2. 目标与范围

- 目标：让 SSE/异步响应的正常超时、响应不可用和客户端断开回到 Spring MVC 生命周期语义，避免通用 JSON 500 与二次写响应异常。
- 成功条件：已提交 SSE 超时不再写 JSON 或改写状态；未提交异步超时保持 503；客户端断开不记录为系统 500；普通业务异常继续返回 `R`。
- 处理范围：`mango-infra-web-starter` 异步生命周期异常解析器、自动配置和 Web 边界集成测试。
- 不处理范围：不修改 Realtime/AI 客户端重连策略，不改变 SSE 连接时长配置，不改变普通业务异常返回协议。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-478-001 | 已建立并已提交的 SSE 连接 | `AsyncRequestTimeoutException` | 连接结束，不写 `R` 或 JSON，不改写已提交响应 | 不产生 `HttpMessageNotWritableException` | 真实 HTTP SSE 超时响应保持原流语义，日志无通用系统异常 |
| AC-478-002 | 尚未提交的异步响应 | `AsyncRequestTimeoutException` | 返回 Spring 默认 503 | 不返回 Mango `R` JSON | 解析器单测断言 503 |
| AC-478-003 | 客户端断开/响应不可用 | `AsyncRequestNotUsableException` 或断开异常 | 静默完成异常解析 | 不记录为系统 500 | 单测覆盖典型断开异常 |
| AC-478-004 | 普通同步业务入口 | 任意未识别业务异常 | 保持现有 `R.fail(500, "系统异常")` | 500 JSON | 既有 Web 边界测试继续通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-478-001 | AC-478-001/002 | 在全局 JSON advice 之前注册高优先级 `HandlerExceptionResolver`，仅处理 Spring 异步生命周期/客户端断开异常；超时未提交时设 503，已提交时返回空处理结果。 | Web MVC 异常解析链 | 删除解析器 bean 与测试，恢复原 advice 优先级 |
| TD-478-002 | AC-478-003 | 使用 Spring `DisconnectedClientHelper` 识别 broken pipe、connection reset、容器 ClientAbort 等断开异常；不吞掉被排除的远端调用异常。 | Web MVC 异常解析链 | 删除断开分支 |
| TD-478-003 | AC-478-004 | 对未命中的异常返回 `null`，继续交给既有 `GlobalExceptionHandler` 和后续解析器。 | 全局异常兼容性 | 删除 resolver 不影响业务 handler |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-478-001 | TD-478-001/002 | 1 | Web starter 异步生命周期 resolver | 编译通过，覆盖 timeout/not-usable/disconnect 分支 |
| TASK-478-002 | TD-478-001 | 2 | Web 自动配置 | resolver 在 advice 前生效且可被覆盖/禁用不破坏启动 |
| TASK-478-003 | AC-478-001/004 | 3 | Web 边界集成测试 | 真实 SSE 超时无二次 converter 异常，普通异常契约不变 |
| TASK-478-004 | 全部 | 4 | 本记录 | 写入真实命令、结果和剩余风险 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-478-001 | M11/M12 | `mvn -Dtest=WebBoundaryIntegrationTest test`；`mvn verify` | PASS，真实 Tomcat SSE 返回 200/`text-event-stream`，无二次 converter 异常 | `target/surefire-reports` |
| AC-478-002 | M10 | `mvn -Dtest=AsyncLifecycleExceptionResolverTest test` | PASS，超时未提交响应为 503 | `target/surefire-reports` |
| AC-478-003 | M10 | `mvn -Dtest=AsyncLifecycleExceptionResolverTest test` | PASS，异步响应不可用和 broken pipe 均不写 JSON | `target/surefire-reports` |
| AC-478-004 | M11/M12 | `mvn verify` 中 `WebBoundaryIntegrationTest` | PASS，原有业务、SQL、参数异常契约保持不变 | `target/surefire-reports` |

最终根聚合定向复核：

- `mvn -f mango/pom.xml -pl :mango-infra-web-starter -Dtest=AsyncLifecycleExceptionResolverTest,WebBoundaryIntegrationTest test`
- 结果：PASS，9 tests，0 failures，0 errors，0 skipped。
- 模块完整验证：`mvn verify`，PASS，30 tests。
- 静态边界验证：根聚合 Checkstyle PASS；`mvn mango:architecture` PASS（dependency=0、archunit=0、pmd=0、blocking=0）；SpotBugs 0 bugs。

## 7. 例外与剩余风险

- 当前环境没有 `mango` CLI，无法执行 `mango workspace init`；本任务不启动本地业务服务，使用 Maven 模块定向测试替代。
- 直接在模块目录运行 `mvn pmd:check` / `mvn checkstyle:check` 会因仓库相对路径配置找不到根目录规则文件；从根聚合按 `-pl :mango-infra-web-starter` 执行时 Checkstyle 通过，`mango:architecture` 通过。`mango:check` 受基线中已有 288 条历史违规阻断，未发现本次新增 resolver 的架构违规。
- 不改变 5 分钟 SSE 生命周期和客户端重连策略；连接轮换仍是预期行为。
