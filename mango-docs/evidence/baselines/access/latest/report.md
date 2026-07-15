# Mango Access 历史债务治理验证基线

## 1. 对象与环境

- 验证日期：2026-07-15
- 基线分支：`origin/main`，起点 `e2de1e015`
- 任务分支：`refactor/access-debt`
- Java：21.0.10
- Maven：当前仓库 Maven reactor
- 数据库：不适用；Access 无数据库、Flyway、初始化数据、演示数据或菜单资源
- 测试数据：测试类内独立 token、策略、权限与租户数据；租户标识 `tenant-a`，无真实账号、密码、token 或密钥

## 2. 治理前基线与红灯

| 项目 | 治理前结果 |
|---|---|
| Access 自有自动化 | 8 条，仅 `AuthFilterTest`；API/Core/Gateway 为 0 |
| 架构债务 | 12 条：API 3、Core 6、Web 1、Gateway 2 |
| Servlet PUBLIC 身份注入 | 外部 tenant/app 上下文可残留，红灯复现 |
| 策略服务失败 | 降级为 LOGIN 并返回 401，红灯复现；预期 fail-closed 503 |
| 真实入口 | 无随机端口 Servlet/Gateway 下游链路测试 |

红灯命令：

```bash
mvn -pl :mango-access-web-starter,:mango-access-gateway-starter -am \
  -Dtest=AuthFilterTest,AuthGlobalFilterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 3. 长期用例基线

| 用例 ID | 优先级 | 层级 | 场景与稳定契约 | 入口 | 状态 |
|---|---|---|---|---|---|
| TC-ACCESS-001 | P0 | 单元 | PUBLIC/LOGIN/PERMISSION/INTERNAL 决策与 fail-closed | `AccessEvaluatorTest` | AUTOMATED |
| TC-ACCESS-002 | P0 | 单元 | token 类型、主体 claim、上下文校验、权限通配与依赖异常 | `AccessEvaluatorTest` | AUTOMATED |
| TC-ACCESS-003 | P1 | 单元 | IPv4/IPv6/CIDR、方法、路径和非法白名单配置 | `IpWhitelistMatcherTest` | AUTOMATED |
| TC-ACCESS-004 | P0 | 组件 | Servlet 清理外部身份、写入可信上下文、credential 优先级、401/403/503 与 JSON 编码 | `AuthFilterTest` | AUTOMATED |
| TC-ACCESS-005 | P0 | 组件 | Gateway 清理身份头、probe 对齐与 JSON 编码 | `AuthGlobalFilterTest` | AUTOMATED |
| TC-ACCESS-006 | P0 | 入口流程 | 随机端口 Tomcat 经过真实 Filter/自动配置/HTTP | `AccessWebFlowTest`，tag `flow,access` | AUTOMATED |
| TC-ACCESS-007 | P0 | 入口流程 | 随机端口 Netty Gateway 到真实 Reactor Netty 下游 | `AccessGatewayFlowTest`，tag `flow,access` | AUTOMATED |
| TC-ACCESS-008 | P0 | 静态/集成 | 当前 Access 生产者、System 扩展、Auth/Admin/Gateway App 同 Reactor 编译 | 8 模块 compile 命令 | AUTOMATED |
| TC-ACCESS-009 | P1 | 单元/静态 | 同域 web/gateway Starter 可依赖 Core，外域与任意限定词仍拒绝 | `MavenDependencyCheckerTest` + Access architecture | AUTOMATED |

## 4. 最终结果

### 4.1 Access 完整回归与入口流程

```bash
mvn -pl :mango-access-api,:mango-access-core,:mango-access-web-starter,:mango-access-gateway-starter -am test
```

- 结果：PASS
- Access 自有用例：31/31
- Core：10；Web：14（含真实 HTTP 3）；Gateway：7（含真实 Gateway→下游 4）
- 失败/错误/跳过：0/0/0
- 治理前 8 条原有用例全部保留并通过；新增用例覆盖已证明缺陷和真实入口。

### 4.2 架构门禁

```bash
mvn -pl :mango-architecture-rules -Dtest=MavenDependencyCheckerTest test
mvn -pl :mango-access-api,:mango-access-core,:mango-access-web-starter,:mango-access-gateway-starter \
  mango:architecture \
  -Dmango.architecture.requireFullReactor=false \
  -Dmango.architecture.mode=full \
  -Dmango.architecture.base=origin/main
```

- 结果：PASS
- 架构规则契约：17/17
- Access：dependency=0、archunit=0、pmd=0、blocking=0
- 相对治理前：12 → 0

### 4.3 当前生产者与真实消费者

```bash
mvn -pl :mango-access-api,:mango-access-core,:mango-access-web-starter,\
:mango-access-gateway-starter,:mango-auth-starter,:mango-system-core,\
:mango-admin-starter,:mango-gateway-app -DskipTests compile
```

- 结果：PASS，8/8 模块
- 生产者与 System、Auth、Admin、Gateway App 使用当前工作区源码同 Reactor 编译，未以旧本地 JAR 代替当前 API，覆盖 Issue #522 类风险。

## 5. 行为对比结论

- 保持：PUBLIC/LOGIN/PERMISSION/INTERNAL、Header→query→Cookie credential 优先级、外部 `/api` 前缀、IP 白名单、realtime probe、合法 token claim 与权限通配。
- 修复：外部安全身份注入、策略/授权依赖异常降级、Gateway event-loop 阻塞、Gateway probe 不一致、错误 JSON 未转义、Starter 运行时装配越界。
- 契约：三个历史 record 迁移为规范 VO；仓内全部引用已迁移，当前生产者和真实消费者同 Reactor 编译通过。
- 非阻断环境告警：Spring Cloud Gateway 旧 artifact 名弃用提示、macOS Netty DNS native fallback；均为既有依赖生态告警，不影响本次断言。
- 未完成项：无。
