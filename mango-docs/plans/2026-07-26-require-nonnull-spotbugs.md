# 标准交付记录

> 主题：Require 非空收窄与 SpotBugs 兼容

## 1. 元数据

- 任务 ID：`require-nonnull-spotbugs`
- 交付模式：STANDARD
- 需求影响：L2 - 业务开发规范与静态检查冲突，影响公共前置条件写法和 Maven 公共契约。
- 方案风险：L2 - 向 `mango-common` 增加公共 API 与 provided 注解依赖，需要验证源码、字节码和消费者静态分析兼容性。
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：统一解决 `Require.isTrue(value != null, ...)` 无法被 SpotBugs 推导为非空而触发 `NP_NULL_PARAM_DEREF` 的规范冲突。
- 成功条件：业务失败继续抛 `BizException`；调用方可以获得静态标注为非空的原对象；规范不要求改写成 `if + Require.fail`；定向质量门禁通过。
- 处理范围：`Require` 公共 API、Common 单测和依赖、后端代码规范、Common README、能力地图。
- 不处理范围：不修改现有 `notNull/isTrue` 签名，不批量迁移历史调用点，不全局抑制 SpotBugs，不改变任何业务模块行为。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | Java 业务调用方 | 向 `Require.nonNull` 传入非空对象 | 返回同一对象，并暴露 SpotBugs 可识别的非空返回契约 | 不失败 | 四类重载均返回对象原引用 |
| SR-002 | Java 业务调用方 | 向 `Require.nonNull` 传入 null | 使用指定错误码和消息抛出 `BizException` | 不允许退化为 NPE | 单测验证异常类型、消息和错误码 |
| SR-003 | 业务开发者 | 复合条件需要 `Require.isTrue` 且 SpotBugs 无法收窄 | 保留业务断言，仅局部增加静态非空收窄 | 禁止改成 `if + Require.fail` 或全局抑制 | 规范、README 和能力索引口径一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002 | 新增四个 `Require.nonNull` 泛型静态重载，使用 `@NonNull` 标注返回值；现有方法签名不变，保持二进制兼容 | `mango-common` | 删除新增方法与 provided 注解依赖 |
| TD-002 | SR-002 | 实现先复用 `notNull` 产生业务异常，再由 `Objects.requireNonNull` 完成工具内部的静态收窄；后者在合法控制流中不可触发 | `Require.java` | 回滚新增方法 |
| TD-003 | SR-003 | 规则按纯对象判空与复合条件分流，禁止用检查工具改变业务校验风格 | PMO、README、能力地图 | 回滚对应说明并恢复原规则索引摘要 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-001 | TD-001, TD-002 | 1 | `mango/mango-common/**` | API、依赖和正反例单测完成 |
| TASK-002 | TD-003 | 2 | `mango-pmo/rules/backend/01-code.md`、`mango-pmo/rules/index.json` | 唯一规范源与路由摘要同步 |
| TASK-003 | TD-003 | 3 | Common README、能力地图 | 业务开发者可定位新入口和排障口径 |
| TASK-004 | SR-001, SR-002, SR-003 | 4 | 定向验证 | 单测、SpotBugs、模块 verify 和文档门禁全部通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001, SR-002 | M10 单元测试 | `mvn -f mango/pom.xml -pl :mango-common test` | PASS：22 个测试通过，0 失败、0 错误、0 跳过 | Maven Surefire 输出；`RequireTest` 10 个测试通过 |
| SR-001 | M09 静态验证 | Common 字节码契约检查与 SpotBugs 消费 fixture | PASS：四个重载均保留 `@NonNull` 字节码注解；基线写法触发 NP，`Require.nonNull` 消费写法不触发 | `javap -v` 与 SpotBugs 4.9.3.0 XML 输出 |
| SR-001, SR-002 | M09 静态验证 | `mvn -f mango/pom.xml -pl :mango-common verify` | PASS：Checkstyle 0、SpotBugs 0、PMD 0，22 个测试通过 | Maven verify 输出 |
| SR-003 | M09 静态验证 | `test-quality-check`、`workspace-layout-check`、README 结构与源码事实检查 | PASS | 四项检查输出 |
| SR-001, SR-002, SR-003 | M15 外部状态回读 | PR required checks | 计划执行 | GitHub PR check runs |

## 7. 例外与剩余风险

- 不批量迁移现有调用点；业务模块在真实出现静态误报或自然修改相关代码时采用新 API，避免无关 diff。
