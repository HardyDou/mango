# Sprint 14 自动检查映射

## 1. 第一批自动规则

| Rule ID | Tool | Check Target | Fail Condition |
|---|---|---|---|
| BE-API-002 | PMD | `*Api`、Controller 方法签名 | 参数或返回出现 `Entity`、`PO` |
| BE-API-005 | PMD | API 返回类型 | 业务对象返回类型不是 `XxxVO` |
| BE-API-010 | PMD | GET 方法 | GET 方法使用 `@RequestBody` |
| BE-API-012 | PMD | `Command`、`Query` 字段 | 字段缺少 `jakarta.validation` 约束注解 |
| BE-API-014 | PMD | Controller、`XxxApi` | 参数校验入口缺少 `@Validated` 或等效机制 |
| BE-API-017 | PMD | Controller | 直接注入或调用 `Mapper` |
| BE-API-020 | PMD | `*-api` 源码 | 出现 `@FeignClient` |
| BE-API-022 | PMD | `*-api` 源码 | 出现服务发现名硬编码 |
| BE-NAME-006 | PMD | 实体类 | 持久化实体不是 `XxxEntity` |
| BE-NAME-007 | PMD | 创建入参 | 创建入参不是 `CreateXxxCommand` |
| BE-NAME-008 | PMD | 更新入参 | 更新入参不是 `UpdateXxxCommand` |
| BE-NAME-009 | PMD | 查询入参 | 查询入参不是 `XxxQuery` |
| BE-NAME-010 | PMD | 分页查询入参 | 分页查询入参不是 `XxxPageQuery` |
| BE-NAME-011 | PMD | 返回对象 | 返回对象不是 `XxxVO` |
| BE-NAME-015 | PMD | 类名、包名、API 签名 | 出现 `PO`、`DO` |
| BE-NAME-016 | PMD | 仓内业务 API | 入参或返回使用 `DTO` |
| BE-NAME-017 | `mango-maven-plugin` | KV 注解 key | 手写 `mango:infra:kv` 前缀，或使用 `user:#userId` / `user:@bean` 非标准写法 |
| BE-TEST-005 | `mango-maven-plugin` | `src/test/java/**/*Test.java` | `Redis*Test`、`Jdbc*Test`、`Memory*Test` 使用不同实现类型的 KV 测试物料 |
| BE-MOD-001 | `mango-maven-plugin` | `*-api` 目录 | 出现非契约包或实现类 |
| BE-MOD-006 | `mango-maven-plugin` | `*-core/pom.xml` | 依赖非 API 的跨域模块 |
| BE-MOD-007 | `mango-maven-plugin` | `*-starter-remote/pom.xml` | 依赖非本域 API 模块 |
| BE-MOD-009 | PMD | `*-api` 源码 | 出现 `@FeignClient` |
| BE-MOD-010 | `mango-maven-plugin` | `*-core` 目录 | 出现 Controller |
| BE-MOD-012 | PMD | `starter-remote` 源码 | 服务名常量或 `@FeignClient(name = "...")` 硬编码 |
| BE-MOD-015 | `mango-maven-plugin` | `*-starter` 目录 | 缺少 `META-INF/mango/module.properties` 或 `module-name` 非法 |
| BE-SEC-001 | PMD | Java / YAML / properties | 出现疑似明文密钥、口令、token |
| BE-SEC-006 | PMD | 日志语句 | 日志输出 token、password、secret、credential |

## 2. 第一批半自动规则

| Rule ID | Tool | Candidate Signal | Manual Decision |
|---|---|---|---|
| BE-CODE-013 | `mango-maven-plugin` | 业务入口方法开头无 `Require` 调用 | 是否属于业务入口 |
| BE-CODE-014 | `mango-maven-plugin` | 参数校验使用散写 `if/throw` | 是否为前置条件 |
| BE-CODE-015 | `mango-maven-plugin` | 方法前半段出现 `if (...) throw ...` | 是否可改为 `Require` |
| BE-CODE-016 | `mango-maven-plugin` | 方法前半段出现 `if (...) return ...` | 是否为非法参数吞掉 |
| BE-API-011 | `mango-maven-plugin` | API 方法入参无校验注解 | 是否需要校验 |
| BE-API-013 | `mango-maven-plugin` | `@PathVariable`、`@RequestParam` 无约束 | 是否属于外部输入 |
| BE-MOD-003 | `mango-maven-plugin` | `starter` 暴露 `XxxApi` 但无模块信息声明 | 是否为本地 starter |
| BE-MOD-004 | `mango-maven-plugin` | `starter-remote` 直接声明 Feign 调用 | 是否已通过 Remote Adapter |
| BE-MOD-013 | `mango-maven-plugin` | 远程调用未读取模块信息 | 是否为本地直连 |
| BE-SEC-005 | `mango-maven-plugin` | 受保护 Controller 无权限注解 | 是否为公开接口 |

## 3. `mango-maven-plugin` 新增检查项

`mango:check` 是项目统一验收入口。对内部分两层：

- `mango-check`：只承载 Mango 项目特有规则
- `static`：聚合 P3C/PMD、Checkstyle、SpotBugs

因此 `mango:check` 负责统一规则分流、结果聚合、项目约束检查和最终失败语义。

| Rule Group | Command | Scope |
|---|---|---|
| Naming | `mvn mango:check -Drule=naming` | P3C/Checkstyle 覆盖不到的 Mango 模块命名，例如 Maven `artifactId` kebab-case |
| API contract | `mvn mango:check -Drule=api-contract` | API 签名、包结构、`@FeignClient`、DTO/Entity 暴露 |
| Validation | `mvn mango:check -Drule=validation` | Bean Validation、`@Validated`、路径参数校验 |
| Require | `mvn mango:check -Drule=require` | 业务入口前置条件、散写 `if/throw` 候选 |
| Module boundary | `mvn mango:check -Drule=module-boundary` | 模块依赖、目录职责、跨域依赖；按 `common/infra/platform/app` 一级分层，再按 `api/core/starter/starter-remote` 二级职责检查 |
| Module info | `mvn mango:check -Drule=module-info` | 本地 starter 模块信息资源文件 |
| Remote adapter | `mvn mango:check -Drule=remote-adapter` | 服务名硬编码、Feign 直接使用、能力解析 |
| Security | `mvn mango:check -Drule=security` | 敏感信息、日志敏感字段、SQL 拼接候选 |
| KV key | `mvn mango:check -Drule=kv-key` | KV 注解 key 前缀和 SpEL 写法 |
| Test fixture | `mvn mango:check -Drule=test-fixture` | 测试类名和核心测试物料一致性 |

## 4. 通用代码质量检查

| Rule Group | Command | Scope |
|---|---|---|
| P3C / PMD | `mvn pmd:check` | 命名、异常、集合、并发、OOP、ORM、控制语句、契约型注释等通用 Java 规则 |
| Checkstyle | `mvn checkstyle:check` | 格式、复杂度、长度、命名等通用 Java 规则；不检查形式化 Javadoc |
| SpotBugs | `mvn spotbugs:check` | 字节码级 bug 风险、安全类缺陷和高概率误用 |

对团队日常验收，统一入口优先使用：

```bash
mvn mango:check -Drule=all
```

其中：

- `mvn mango:check -Drule=static` = 只聚合 PMD / Checkstyle / SpotBugs
- `mvn mango:check -Drule=all` = `static` + Mango 项目特有规则

## 4.1 报告来源

统一报告必须带来源字段，避免把 Mango 自定义规则和通用静态分析结果混在一起：

| Source | Meaning |
|---|---|
| `mango-check` | Mango 项目特有规则 |
| `pmd` | PMD / P3C 通用规则 |
| `checkstyle` | Checkstyle 通用规则 |
| `spotbugs` | SpotBugs 通用规则 |

## 5. 执行模式

| Mode | Purpose | Behavior |
|---|---|---|
| `migration` | 存量迁移 | 自动规则失败输出 warning，新增代码失败阻断 |
| `strict` | 稳定模块 | 自动规则失败阻断构建 |
| `report` | 盘点 | 只生成报告，不阻断构建 |

## 6. 不做自动硬检查的规则

| Rule | Reason |
|---|---|
| 类职责单一 | 依赖设计判断 |
| Controller 只做协议适配 | 依赖业务复杂度判断 |
| 一个表只服务一个领域 | 依赖领域模型判断 |
| 测试覆盖范围完整 | 依赖需求影响面判断 |
| 只改本次需求相关代码 | 依赖任务上下文判断 |
