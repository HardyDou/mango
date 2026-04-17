# Sprint 14 后端规则清单

## 1. 代码规则

| Rule ID | Source | Rule | Type | Tool | Risk | Action |
|---|---|---|---|---|---|---|
| BE-CODE-001 | `01-code.md` | 代码必须可读、可测、可维护 | 人工 | PR checklist | 中 | 保留 |
| BE-CODE-002 | `01-code.md` | 同一逻辑只保留一份实现 | 半自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-CODE-003 | `01-code.md` | 不写无意义包装层 | 人工 | PR checklist | 高 | 保留 |
| BE-CODE-004 | `01-code.md` | 不把临时方案当正式方案提交 | 人工 | PR checklist | 高 | 保留 |
| BE-CODE-005 | `01-code.md` | 方法保持简短 | 自动 | Checkstyle | 低 | 裁剪 |
| BE-CODE-006 | `01-code.md` | 类职责保持单一 | 人工 | PR checklist | 高 | 保留 |
| BE-CODE-007 | `01-code.md` | 一个类只解决一类问题 | 人工 | PR checklist | 高 | 保留 |
| BE-CODE-008 | `01-code.md` | 超过当前抽象层职责的逻辑必须下沉或拆分 | 人工 | PR checklist | 高 | 保留 |
| BE-CODE-009 | `01-code.md` | 只捕获明确异常 | 自动 | P3C / PMD | 低 | 保留 |
| BE-CODE-010 | `01-code.md` | 捕获后必须处理或继续抛出 | 自动 | P3C / PMD | 中 | 保留 |
| BE-CODE-011 | `01-code.md` | 禁止吞异常 | 自动 | P3C / PMD | 低 | 保留 |
| BE-CODE-012 | `01-code.md` | 禁止直接捕获 `Exception` 或 `Throwable` 作为常规写法 | 自动 | P3C / PMD | 中 | 裁剪 |
| BE-CODE-013 | `01-code.md` | 业务逻辑入口必须先做前置条件校验 | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-CODE-014 | `01-code.md` | 业务前置条件统一使用 `Require` | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-CODE-015 | `01-code.md` | 禁止在业务逻辑中直接写 `if (...) throw ...` 做前置条件校验 | 半自动 | `mango-maven-plugin` | 高 | 新增 |
| BE-CODE-016 | `01-code.md` | 禁止用 `if (...) return ...` 跳过非法参数处理 | 半自动 | `mango-maven-plugin` | 高 | 新增 |
| BE-CODE-017 | `01-code.md` | `Require` 失败统一抛业务异常 | 半自动 | `mango-maven-plugin` | 中 | 新增 |

## 2. 命名规则

| Rule ID | Source | Rule | Type | Tool | Risk | Action |
|---|---|---|---|---|---|---|
| BE-NAME-001 | `02-naming.md` | 模块名使用 `kebab-case` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-NAME-002 | `02-naming.md` | 包名使用小写 | 自动 | Checkstyle | 低 | 保留 |
| BE-NAME-003 | `02-naming.md` | 类名使用 `PascalCase` | 自动 | Checkstyle | 低 | 保留 |
| BE-NAME-004 | `02-naming.md` | 方法和变量使用 `camelCase` | 自动 | Checkstyle | 低 | 保留 |
| BE-NAME-005 | `02-naming.md` | 表名和字段名使用小写下划线 | 半自动 | migration scan | 中 | 保留 |
| BE-NAME-006 | `02-naming.md` | 持久化实体使用 `XxxEntity` | 自动 | PMD | 低 | 保留 |
| BE-NAME-007 | `02-naming.md` | 新增入参使用 `CreateXxxCommand` | 自动 | PMD | 低 | 保留 |
| BE-NAME-008 | `02-naming.md` | 更新入参使用 `UpdateXxxCommand` | 自动 | PMD | 低 | 保留 |
| BE-NAME-009 | `02-naming.md` | 查询入参使用 `XxxQuery` | 自动 | PMD | 低 | 保留 |
| BE-NAME-010 | `02-naming.md` | 分页查询使用 `XxxPageQuery` | 自动 | PMD | 低 | 保留 |
| BE-NAME-011 | `02-naming.md` | 返回对象使用 `XxxVO` | 自动 | PMD | 低 | 保留 |
| BE-NAME-012 | `02-naming.md` | 对外接口使用 `XxxApi` | 自动 | PMD | 低 | 保留 |
| BE-NAME-013 | `02-naming.md` | 内部服务使用 `IXxxService` | 自动 | PMD | 中 | 裁剪 |
| BE-NAME-014 | `02-naming.md` | 扩展接口使用 `I...Provider`、`I...Checker`、`I...Validator` | 自动 | PMD | 中 | 裁剪 |
| BE-NAME-015 | `02-naming.md` | 禁止 `PO`、`DO` | 自动 | PMD | 低 | 保留 |
| BE-NAME-016 | `02-naming.md` | 仓内业务 API 禁止使用 `DTO` 作为入参或返回命名 | 自动 | PMD | 低 | 保留 |
| BE-NAME-017 | `02-naming.md` | 不新增 `model`、`dto`、`po`、`util`、`helper` 杂项包 | 自动 | `mango-maven-plugin` | 中 | 裁剪 |

## 3. API 规则

| Rule ID | Source | Rule | Type | Tool | Risk | Action |
|---|---|---|---|---|---|---|
| BE-API-001 | `03-api.md` | API 只暴露协议模型 | 半自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-API-002 | `03-api.md` | API 不暴露 `Entity`、`PO` | 自动 | PMD | 低 | 保留 |
| BE-API-003 | `03-api.md` | 写操作使用 `Command` | 自动 | PMD | 低 | 保留 |
| BE-API-004 | `03-api.md` | 查询使用 `Query` | 自动 | PMD | 低 | 保留 |
| BE-API-005 | `03-api.md` | 返回统一使用 `VO` | 自动 | PMD | 低 | 保留 |
| BE-API-006 | `03-api.md` | 创建使用 `CreateXxxCommand` | 自动 | PMD | 低 | 保留 |
| BE-API-007 | `03-api.md` | 更新使用 `UpdateXxxCommand` | 自动 | PMD | 低 | 保留 |
| BE-API-008 | `03-api.md` | 查询使用 `XxxQuery` 或 `XxxPageQuery` | 自动 | PMD | 低 | 保留 |
| BE-API-009 | `03-api.md` | 简单路径参数和查询参数直接放方法签名 | 人工 | PR checklist | 高 | 保留 |
| BE-API-010 | `03-api.md` | `GET` 默认不用 `@RequestBody` | 自动 | PMD | 低 | 保留 |
| BE-API-011 | `03-api.md` | API 参数必须使用 Bean Validation 校验 | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-API-012 | `03-api.md` | `Command`、`Query` 字段必须声明 `jakarta.validation` 约束注解 | 自动 | PMD | 中 | 新增 |
| BE-API-013 | `03-api.md` | 路径参数和查询参数必须声明校验约束 | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-API-014 | `03-api.md` | Controller 或 `Api` 必须使用 `@Validated` 或等效机制开启参数校验 | 自动 | PMD | 中 | 新增 |
| BE-API-015 | `03-api.md` | `api` 只放 `XxxApi`、`command`、`query`、`vo`、`enums` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-API-016 | `03-api.md` | Controller 只做协议适配 | 人工 | PR checklist | 高 | 保留 |
| BE-API-017 | `03-api.md` | Controller 不直接操作 `Mapper` | 自动 | PMD | 中 | 保留 |
| BE-API-018 | `03-api.md` | Controller 不直接返回持久化对象 | 自动 | PMD | 低 | 保留 |
| BE-API-019 | `03-api.md` | `XxxApi` 只定义能力契约 | 人工 | PR checklist | 中 | 保留 |
| BE-API-020 | `03-api.md` | `XxxApi` 禁止声明 `@FeignClient` | 自动 | PMD | 低 | 新增 |
| BE-API-021 | `03-api.md` | 仓内业务 API 禁止使用 `DTO` 作为默认入参或返回命名 | 自动 | PMD | 低 | 保留 |
| BE-API-022 | `03-api.md` | API 契约禁止硬编码服务发现名 | 自动 | PMD | 低 | 新增 |

## 4. 数据库与持久化规则

| Rule ID | Source | Rule | Type | Tool | Risk | Action |
|---|---|---|---|---|---|---|
| BE-DB-001 | `04-db.md` | 表名使用小写下划线 | 半自动 | migration scan | 中 | 保留 |
| BE-DB-002 | `04-db.md` | 表名必须体现模块边界 | 人工 | PR checklist | 高 | 保留 |
| BE-DB-003 | `04-db.md` | 不使用外键作为默认方案 | 半自动 | migration scan | 中 | 保留 |
| BE-DB-004 | `04-db.md` | 一个表只服务一个领域 | 人工 | PR checklist | 高 | 保留 |
| BE-DB-005 | `04-db.md` | 字段名使用小写下划线 | 半自动 | migration scan | 中 | 保留 |
| BE-DB-006 | `04-db.md` | 公共审计字段按项目基线统一 | 半自动 | migration scan | 中 | 保留 |
| BE-DB-007 | `04-db.md` | 多租户表按需要增加 `tenant_id` | 人工 | PR checklist | 高 | 保留 |
| BE-DB-008 | `04-db.md` | 必须使用参数化查询 | 自动 | P3C / PMD | 中 | 保留 |
| BE-DB-009 | `04-db.md` | 禁止字符串拼接 SQL | 自动 | P3C / PMD | 中 | 保留 |
| BE-DB-010 | `04-db.md` | Mapper 只访问本域表 | 半自动 | `mango-maven-plugin` | 高 | 保留 |
| BE-DB-011 | `04-db.md` | DDL 变更必须使用 Flyway migration | 半自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-DB-012 | `04-db.md` | 只新增 migration，不修改历史 migration | 人工 | PR checklist | 高 | 保留 |
| BE-PER-001 | `07-persistence.md` | 写操作必须放在明确事务边界内 | 半自动 | `mango-maven-plugin` | 高 | 保留 |
| BE-PER-002 | `07-persistence.md` | 同一业务动作只定义一个主事务边界 | 人工 | PR checklist | 高 | 保留 |
| BE-PER-003 | `07-persistence.md` | 不在内部私有调用上重复定义事务 | 自动 | PMD | 中 | 保留 |
| BE-PER-004 | `07-persistence.md` | 跨域数据不通过跨表 join 解决 | 半自动 | `mango-maven-plugin` | 高 | 保留 |

## 5. 模块规则

| Rule ID | Source | Rule | Type | Tool | Risk | Action |
|---|---|---|---|---|---|---|
| BE-MOD-001 | `05-module.md` | `api` 只放契约模型和 `XxxApi` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-MOD-002 | `05-module.md` | `core` 只放业务实现、实体、Mapper、转换 | 自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-MOD-003 | `05-module.md` | `starter` 负责实现 `XxxApi`、自动装配、能力自动注册 | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-MOD-004 | `05-module.md` | `starter-remote` 负责远程调用适配和能力解析 | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-MOD-005 | `05-module.md` | `app` 依赖 `starter` 或 `starter-remote` | 自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-MOD-006 | `05-module.md` | `core` 只依赖本域 `api` 和其他域 `api` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-MOD-007 | `05-module.md` | `starter-remote` 只依赖本域 `api` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-MOD-008 | `05-module.md` | `api` 不放 `Entity`、`Mapper`、`Controller` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-MOD-009 | `05-module.md` | `api` 不放 `@FeignClient` | 自动 | PMD | 低 | 新增 |
| BE-MOD-010 | `05-module.md` | `core` 不放 `Controller` | 自动 | `mango-maven-plugin` | 低 | 保留 |
| BE-MOD-011 | `05-module.md` | 跨域调用必须走 `XxxApi` | 半自动 | `mango-maven-plugin` | 高 | 保留 |
| BE-MOD-012 | `05-module.md` | 远程目标服务不得写死在业务代码中 | 自动 | PMD | 中 | 新增 |
| BE-MOD-013 | `05-module.md` | 远程目标必须通过能力注册表或配置解析 | 半自动 | `mango-maven-plugin` | 中 | 新增 |
| BE-MOD-014 | `05-module.md` | 服务能力维护属于 `mango-infra` | 人工 | PR checklist | 中 | 保留 |
| BE-MOD-015 | `05-module.md` | 每个 `starter` 必须注册自己提供的能力 | 半自动 | `mango-maven-plugin` | 中 | 新增 |

## 6. 安全、测试、流程规则

| Rule ID | Source | Rule | Type | Tool | Risk | Action |
|---|---|---|---|---|---|---|
| BE-SEC-001 | `06-security.md` | 禁止硬编码敏感信息 | 自动 | PMD | 中 | 保留 |
| BE-SEC-002 | `06-security.md` | 所有外部输入必须校验 | 半自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-SEC-003 | `06-security.md` | 字符串输入必须限制长度和格式 | 半自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-SEC-004 | `06-security.md` | 所有 SQL 必须参数化 | 自动 | P3C / PMD | 中 | 保留 |
| BE-SEC-005 | `06-security.md` | 受保护接口必须做权限校验 | 半自动 | `mango-maven-plugin` | 高 | 保留 |
| BE-SEC-006 | `06-security.md` | 日志禁止打印敏感内容 | 自动 | PMD | 中 | 保留 |
| BE-TEST-001 | `08-test.md` | 有代码改动，必须有对应验证 | 人工 | PR checklist | 中 | 保留 |
| BE-TEST-002 | `08-test.md` | 新增业务逻辑必须补测试 | 人工 | PR checklist | 中 | 保留 |
| BE-TEST-003 | `08-test.md` | 修复缺陷必须补回归测试 | 人工 | PR checklist | 中 | 保留 |
| BE-TEST-004 | `08-test.md` | 测试类名使用 `XxxTest` | 自动 | Checkstyle | 低 | 保留 |
| BE-FLOW-001 | `10-dev-flow.md` | 只改本次需求相关代码 | 人工 | PR checklist | 中 | 保留 |
| BE-FLOW-002 | `10-dev-flow.md` | DDL 变更必须使用 Flyway migration | 半自动 | `mango-maven-plugin` | 中 | 保留 |
| BE-FLOW-003 | `10-dev-flow.md` | 提交前执行对应检查 | 人工 | PR checklist | 中 | 保留 |

