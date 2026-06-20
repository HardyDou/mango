# Issue #186 模块分层升级计划

## 1. 目标

- 将 Mango 后端模块统一到 `api` / `support` / `core` / `starter-*` 四层模型。
- 保留 #186 Resource Registry 运行时依赖专项守护。
- 先完成规范和 `mango:check` 自动化守护，再逐模块迁移历史依赖。

## 2. 已落地守护

- `api` 是接口契约，禁止依赖 `support`、`core`、`starter` 或 `starter-*`。
- `support` 是公共支撑能力，可被其它 `core` 依赖，禁止依赖 `core`、`starter` 或 `starter-*`。
- `support` 禁止包含持久化内容、`@AutoConfiguration` 和 `AutoConfiguration.imports`。
- `core` 可依赖其它模块 `api` 或 `support`，禁止依赖其它模块 `core`、`starter` 或 `starter-*`。
- `starter-remote` 在 `io.mango` 依赖中只允许本模块 `api`、本模块 `support` 和 `mango-infra-feign-starter`。

## 3. 全库检查基线

命令：

```bash
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

初始结果：32 个 dependency 边界问题。

当前进度：

- Task 0 规则与检查器已完成。
- Task 1 `mango-infra-context` 已完成。
- Task 2 `mango-infra-persistence` 已完成。
- Task 3 `mango-authorization-support` 已完成。
- Task 4 `mango-identity-core -> mango-authorization-core` 已完成。
- Task 5 `mango-org-core -> mango-identity-core / mango-infra-web-starter` 已完成。
- Task 6 `mango-system-core -> mango-infra-log-starter / mango-infra-web-starter / mango-access-core` 已完成。
- Task 7 `mango-domain-starter-remote -> spring-cloud-starter-openfeign` 已完成。
- 当前全库 dependency 检查结果：0 个 issue。

## 4. 升级分组

### P0 Infra 公共入口

1. `mango-infra-context`
   - 状态：DONE。
   - 问题：大量 `core` 依赖 `mango-infra-context-core` 或 `mango-infra-context-starter`。
   - 计划：新增或调整为 `mango-infra-context-api`，承载 `MangoContextHolder`、`MangoContextSnapshot`、`MangoContextHeaders` 等编译期上下文契约。
   - 计划：将 `TtlExecutorDecorator` 这类可复用支撑能力放到 `mango-infra-context-support`，starter 只做装配。
   - 影响模块：`mango-auth-core`、`mango-authorization-core`、`mango-file-core`、`mango-file-preview-core`、`mango-template-core`、`mango-workflow-core`、`mango-payment-core`、`mango-ai-core`、`mango-job-support`。

2. `mango-infra-persistence`
   - 状态：DONE。
   - 问题：多个 `core` 直接依赖 `mango-infra-persistence-starter`。
   - 计划：`mango-infra-persistence-api` 承载业务 core 编译期需要的实体基类、CRUD 抽象、分页/Wrapper 相关公共能力。
   - 计划：`mango-infra-persistence-starter` 只保留 MyBatis-Plus、审计、租户、DataSource、Flyway、schema validation 等运行时装配。
   - 影响模块：`mango-infra-kv-core`、`mango-authorization-core`、`mango-identity-core`、`mango-org-core`、`mango-system-core`、`mango-domain-core`、`mango-notice-core`、`mango-file-core`、`mango-workflow-core`、`mango-calendar-core`、`mango-grid-layout-core`、`mango-numgen-core`。

### P1 Infra 横切能力

3. `mango-infra-web`
   - 状态：DONE。
   - 问题：`mango-org-core`、`mango-system-core` 依赖 `mango-infra-web-starter`。
   - 计划：将 core 编译期使用的 Web 工具或轻量契约迁到 `mango-infra-web-api` 或 `mango-infra-web-support`。
   - 计划：`mango-infra-web-starter` 保留 Filter、Controller advice、Servlet 实现和自动配置。

4. `mango-infra-log`
   - 状态：DONE。
   - 问题：`mango-system-core` 依赖 `mango-infra-log-starter`。
   - 计划：新增 `mango-infra-log-api`，迁移 `io.mango.infra.log.annotation.Log` 等注解契约。
   - 计划：starter 依赖 api 并保留日志切面、配置和运行时装配。

### P1 Support 语义修正

5. `mango-authorization-support`
   - 状态：DONE。
   - 问题：support 内含 `AutoConfiguration.imports`、`SecurityAutoConfiguration`、`TokenAutoConfiguration`。
   - 计划：自动配置类和 imports 迁入 `mango-authorization-starter`。
   - 计划：`JjwtTokenServiceImpl` 等非自动配置默认实现可留在 support；如只由 starter 使用，也可迁入 starter。

6. `mango-job-support`
   - 状态：DONE。
   - 问题：support 依赖 `mango-infra-context-core`。
   - 计划：待 `mango-infra-context-api/support` 建立后替换依赖。
   - 计划：保留 Job handler 注册、worker 执行和 transport 支撑能力在 support。

### P2 业务 core 穿透

7. `mango-identity-core -> mango-authorization-core`
   - 状态：DONE。
   - 计划：梳理使用点，将跨模块编译期类型迁到 `mango-authorization-api` 或 `mango-authorization-support`。

8. `mango-org-core -> mango-identity-core`
   - 状态：DONE。
   - 计划：禁止直接使用 identity mapper/entity；跨域查询走 `mango-identity-api`，共享支撑类型放 `mango-identity-support`。

9. `mango-system-core -> mango-access-core`
   - 状态：DONE。
   - 计划：将 access 鉴权上下文、校验器、principal 等公共支撑类型迁到 `mango-access-api` 或 `mango-access-support`。

### P2 Remote starter

10. `mango-domain-starter-remote`
    - 状态：DONE。
    - 问题：直接依赖 `spring-cloud-starter-openfeign`。
    - 计划：改为依赖 `mango-infra-feign-starter`。

## 5. 建议执行顺序

1. 先迁移 `mango-infra-context`，因为它影响最多，且不是持久化能力。
2. 再迁移 `mango-infra-persistence`，解决所有 `core -> persistence-starter`。
3. 处理 `mango-infra-log`、`mango-infra-web`。
4. 修正 `authorization-support` 和 `job-support`。
5. 处理业务 core 穿透：identity/authorization、org/identity、system/access。
6. 最后处理 `mango-domain-starter-remote` 的 Feign 入口。

## 6. 每阶段验收

每个阶段至少执行：

```bash
mvn -f mango/pom.xml -pl <changed-modules> -am -DskipTests compile
mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckMojoTest test
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：对应分组的 dependency 问题归零，不新增 `api -> support/core/starter-*`、`support -> core/starter-*` 或 `core -> core/starter-*`。

## 7. 模块级验收方案

### 7.1 规则与检查器

改动对象：

- `mango-pmo/rules/backend/05-module.md`
- `mango-pmo/rules/index.json`
- `mango/mango-tools/README.md`
- `mango/mango-tools/mango-maven-plugin`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckMojoTest test
mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -DskipTests install
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `CheckMojoTest` 通过。
- `mango:check -Drule=dependency` 能识别 `api/support/core/starter-*` 边界。
- 全库检查仍允许用 `report` 模式列出历史问题，不允许检查器本身异常。
- #186 Resource Registry starter 例外必须显式传入模块和理由。

### 7.2 `mango-infra-context`

改动对象：

- `mango-infra-context-api`
- `mango-infra-context-support`
- `mango-infra-context-starter`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-context -am -DskipTests compile
mvn -f mango/pom.xml -pl mango-platform/mango-auth/mango-auth-core,mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-file/mango-file-core,mango-platform/mango-file-preview/mango-file-preview-core,mango-platform/mango-template/mango-template-core,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-payment/mango-payment-core,mango-platform/mango-job/mango-job-support,mango-extension/mango-ai/mango-ai-core -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `MangoContextHolder`、`MangoContextSnapshot`、`MangoContextHeaders` 等对外可用上下文能力由 `api` 提供。
- `TtlExecutorDecorator` 等非契约公共支撑能力由 `support` 提供。
- `starter` 只保留自动配置和运行时装配。
- 消除以下违规：
  - `mango-auth-core -> mango-infra-context-core`
  - `mango-authorization-core -> mango-infra-context-core`
  - `mango-system-core -> mango-infra-context-starter`
  - `mango-file-core -> mango-infra-context-starter`
  - `mango-file-preview-core -> mango-infra-context-core`
  - `mango-template-core -> mango-infra-context-core`
  - `mango-workflow-core -> mango-infra-context-starter`
  - `mango-job-support -> mango-infra-context-core`
  - `mango-payment-core -> mango-infra-context-core`
  - `mango-ai-core -> mango-infra-context-starter`

### 7.3 `mango-infra-persistence`

改动对象：

- `mango-infra-persistence-api`
- `mango-infra-persistence-starter`
- `mango-infra-persistence-web-starter`
- 所有依赖持久化能力的业务 `core`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-persistence -am -DskipTests compile
mvn -f mango/pom.xml -pl mango-infra/mango-infra-kv/mango-infra-kv-core,mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-identity/mango-identity-core,mango-platform/mango-org/mango-org-core,mango-platform/mango-system/mango-system-core,mango-platform/mango-domain/mango-domain-core,mango-platform/mango-notice/mango-notice-core,mango-platform/mango-file/mango-file-core,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-calendar/mango-calendar-core,mango-platform/mango-grid-layout/mango-grid-layout-core,mango-platform/mango-numgen/mango-numgen-core -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- 业务 `core` 编译期只依赖 `mango-infra-persistence-api`。
- `MangoCrudService`、实体基类、分页、查询注解、Wrapper 相关编译期能力在 `api` 中可用。
- `starter` 保留 MyBatis-Plus、Flyway、DataSource、审计、租户、schema validation 等运行时装配。
- 消除所有 `*-core -> mango-infra-persistence-starter` 违规。
- 不把 mapper、entity 扫描、数据源装配、自动配置迁入 `api`。

### 7.4 `mango-infra-web`

改动对象：

- `mango-infra-web-api`
- 必要时新增 `mango-infra-web-support`
- `mango-infra-web-starter`
- `mango-org-core`
- `mango-system-core`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-web,mango-platform/mango-org/mango-org-core,mango-platform/mango-system/mango-system-core -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `core` 需要的 Web 注解、接口或轻量工具来自 `web-api` 或 `web-support`。
- `starter` 保留 Filter、Controller advice、Servlet 相关实现和自动配置。
- 消除以下违规：
  - `mango-org-core -> mango-infra-web-starter`
  - `mango-system-core -> mango-infra-web-starter`

### 7.5 `mango-infra-log`

改动对象：

- `mango-infra-log-api`
- `mango-infra-log-starter`
- `mango-system-core`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-log,mango-platform/mango-system/mango-system-core -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `@Log`、`LogType` 等注解契约在 `mango-infra-log-api`。
- 日志切面、属性、自动配置仍在 `starter`。
- 消除 `mango-system-core -> mango-infra-log-starter`。

### 7.6 `mango-authorization-support`

改动对象：

- `mango-authorization-support`
- `mango-authorization-starter`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-support,mango-platform/mango-authorization/mango-authorization-starter -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `support` 不存在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- `support` 不包含 `@AutoConfiguration` 类。
- 自动配置和 imports 迁入 `starter`。
- `support` 可保留被其它模块复用的非自动配置默认实现。
- 消除 `mango-authorization-support` 的 3 个 support 内容违规。

### 7.7 `mango-job-support`

改动对象：

- `mango-job-support`
- `mango-job-core`
- `mango-job-starter`
- `mango-job-starter-remote`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-job -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `job-support` 不依赖任何 `core` 或 `starter-*`。
- `job-support` 继续承载 Job handler 注册、worker 执行、transport 支撑能力。
- 消除 `mango-job-support -> mango-infra-context-core`。

### 7.8 业务 core 穿透

改动对象：

- `mango-authorization-api/support`
- `mango-identity-api/support/core`
- `mango-org-api/support/core`
- `mango-access-api/support/core`
- `mango-system-api/support/core`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-authorization,mango-platform/mango-identity,mango-platform/mango-org,mango-platform/mango-access,mango-platform/mango-system -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- 消除以下违规：
  - `mango-identity-core -> mango-authorization-core`
  - `mango-org-core -> mango-identity-core`
  - `mango-system-core -> mango-access-core`
- 跨模块编译期类型迁到对应 `api` 或 `support`。
- 跨域业务调用走 API，不直接访问对方 mapper/entity/service impl。
- `support` 不引入持久化内容和自动配置。

### 7.9 `mango-domain-starter-remote`

改动对象：

- `mango-domain-starter-remote`

验收命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-domain/mango-domain-starter-remote -am -DskipTests compile
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

通过标准：

- `mango-domain-starter-remote` 不直接依赖 `spring-cloud-starter-openfeign`。
- Feign 入口统一依赖 `mango-infra-feign-starter`。
- 消除 `mango-domain-starter-remote -> spring-cloud-starter-openfeign`。

## 8. 当前验收记录

### 8.1 已执行命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-authorization/mango-authorization-starter-remote,mango-platform/mango-identity/mango-identity-core -am -DskipTests compile

mvn -f mango/pom.xml -pl mango-platform/mango-identity/mango-identity-api,mango-platform/mango-identity/mango-identity-core,mango-platform/mango-org/mango-org-core -am -DskipTests compile

mvn -f mango/pom.xml -pl mango-infra/mango-infra-log-api,mango-infra/mango-infra-log,mango-infra/mango-infra-web/mango-infra-web-api,mango-infra/mango-infra-web/mango-infra-web-support,mango-infra/mango-infra-web/mango-infra-web-starter,mango-platform/mango-access/mango-access-api,mango-platform/mango-access/mango-access-core,mango-platform/mango-access/mango-access-web-starter,mango-platform/mango-access/mango-access-gateway-starter,mango-platform/mango-system/mango-system-core,mango-platform/mango-domain/mango-domain-starter-remote -am -DskipTests compile

mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckMojoTest test

mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check \
  -Drule=dependency \
  -Dmango.check.staticFailurePolicy=report \
  -Dmango.check.resourceStarterDependencyExceptions=mango-monolith-app=confirmed-deployment-entry-needs-local-resource-registry,mango-platform-app=confirmed-deployment-entry-needs-platform-resource-registry
```

### 8.2 结果

- 授权/身份定向编译：PASS。
- 身份/组织定向编译：PASS。
- log/web/access/system/domain 定向编译：PASS。
- `CheckMojoTest`：PASS，77 tests，0 failures，0 errors。
- 全库 `mango:check -Drule=dependency`：PASS，0 issue。

### 8.3 模块完成判定

- `mango-infra-context`：DONE，context 契约进入 api，TTL 支撑能力进入 support，starter 保留装配。
- `mango-infra-persistence`：DONE，业务 core 改依赖 persistence-api，starter 保留 MyBatis/Flyway/审计/租户等装配。
- `mango-authorization-support`：DONE，自动配置和 imports 迁出 support。
- `mango-identity-core`：DONE，通过 `mango-authorization-api` 新增角色绑定契约替代 direct core dependency。
- `mango-org-core`：DONE，通过 `mango-identity-api` 的成员组织关系契约替代 identity core mapper/entity 访问。
- `mango-system-core`：DONE，改依赖 `mango-infra-log-api`、`mango-infra-web-support`、`mango-access-api`。
- `mango-domain-starter-remote`：DONE，OpenFeign 入口改为 `mango-infra-feign-starter`。


## 9. 启动测试基线

### 9.1 范围

本次在 #186 同一任务中补齐后端部署入口的可重复启动基线，覆盖三类应用：

- 单体：`mango-monolith-app`。
- 微服务：`mango-gateway-app`、`mango-platform-app`、`mango-business-app`、`mango-file-preview-app`。
- 平台能力独立启动 app：`auth`、`authorization`、`identity`、`org`、`system`、`resource`、`captcha`、`file`、`file-preview`、`domain`、`notice`、`workflow`、`job`、`calendar`、`grid-layout`、`numgen`、`template`、`payment`。

不在本次声明完成的内容：带真实账号、真实权限、真实业务数据的全接口业务断言。启动基线只覆盖编译、进程启动、`/actuator/health`、`/v3/api-docs` 暴露情况。

### 9.2 新增入口

- `mango/mango-app/platform-capability`：平台能力独立启动 app 聚合。
- `scripts/mango-app-baseline.sh`：统一 app 编译、启动、停止、健康检查、OpenAPI 探测入口。
- `scripts/docker/nacos-compose.yml`：本地 Nacos standalone，用于微服务注册/发现基线。

### 9.3 验收命令

```bash
scripts/mango-app-baseline.sh list
scripts/mango-app-baseline.sh compile
scripts/mango-app-baseline.sh package
scripts/mango-app-baseline.sh verify <app>
scripts/mango-app-baseline.sh verify-all
scripts/mango-app-baseline.sh nacos-up
MANGO_BASELINE_PROFILE=nacos scripts/mango-app-baseline.sh verify gateway
scripts/mango-app-baseline.sh nacos-down
```

### 9.4 模块级验收方案

| app | 类型 | 编译验收 | 启动验收 | 接口基线 |
| --- | --- | --- | --- | --- |
| `monolith` | 单体 | `compile monolith` | `verify monolith` | health + OpenAPI |
| `gateway` | 微服务 | `compile gateway` | `verify gateway`；Nacos profile 下重复验证 | health |
| `platform` | 微服务 | `compile platform` | `verify platform` | health + OpenAPI |
| `business` | 微服务 | `compile business` | `verify business` | health + OpenAPI |
| `file-preview-service` | 微服务 | `compile file-preview-service` | `verify file-preview-service` | health + OpenAPI |
| `auth` | 平台能力 | `compile auth` | `verify auth` | health + OpenAPI |
| `authorization` | 平台能力 | `compile authorization` | `verify authorization` | health + OpenAPI |
| `identity` | 平台能力 | `compile identity` | `verify identity` | health + OpenAPI |
| `org` | 平台能力 | `compile org` | `verify org` | health + OpenAPI |
| `system` | 平台能力 | `compile system` | `verify system` | health + OpenAPI |
| `resource` | 平台能力 | `compile resource` | `verify resource` | health + OpenAPI |
| `captcha` | 平台能力 | `compile captcha` | `verify captcha` | health + OpenAPI |
| `file` | 平台能力 | `compile file` | `verify file` | health + OpenAPI |
| `file-preview` | 平台能力 | `compile file-preview` | `verify file-preview` | health + OpenAPI |
| `domain` | 平台能力 | `compile domain` | `verify domain` | health + OpenAPI |
| `notice` | 平台能力 | `compile notice` | `verify notice` | health + OpenAPI |
| `workflow` | 平台能力 | `compile workflow` | `verify workflow` | health + OpenAPI |
| `job` | 平台能力 | `compile job` | `verify job` | health + OpenAPI |
| `calendar` | 平台能力 | `compile calendar` | `verify calendar` | health + OpenAPI |
| `grid-layout` | 平台能力 | `compile grid-layout` | `verify grid-layout` | health + OpenAPI |
| `numgen` | 平台能力 | `compile numgen` | `verify numgen` | health + OpenAPI |
| `template` | 平台能力 | `compile template` | `verify template` | health + OpenAPI |
| `payment` | 平台能力 | `compile payment` | `verify payment` | health + OpenAPI |

### 9.5 已发现并修复的基线问题

- `mango-identity-starter` 等多个 starter 直接使用 `@MapperScan`，但未声明 `mybatis-spring` 编译依赖。启动 app 全量编译暴露该问题后，已在直接使用 `@MapperScan` 的 starter POM 中补齐显式依赖。
- 部分平台能力父 POM 未继承根聚合 `dependencyManagement`，因此 `mybatis-spring` 依赖显式声明版本 `3.0.5`，避免父链差异导致 Maven 解析失败。

### 9.6 风险与例外

- `verify-all` 启动基线默认使用 H2、关闭 Flyway/schema validation、关闭外部 KV/Event/Realtime/Workflow/Job 调度能力，目标是验证部署入口和最小 Web 运行态，不替代真实数据库集成验收。
- 微服务 Nacos 验证需要 Docker 可用；Docker/Nacos 不可用时只能标记 `BLOCKED`，不能声明微服务注册验证完成。
- 真实接口业务验收需要账号、租户、权限、初始化数据和接口断言，本次只建立基线入口，不声明所有业务接口已正常。

## 10. 破坏性升级契约与重建 Runbook

### 10.1 升级契约

#186 是 Resource Registry 发布后的模块边界和菜单初始化重构，按 Mango `1.0` 前置 rebase 处理。当前交付不承诺兼容既有开发库或测试库中的 Flyway 菜单数据、历史菜单运行时配置、菜单包授权和默认角色菜单授权。

本次升级允许重整未正式发布的 Flyway 初始化数据，菜单、按钮权限、菜单运行时配置、菜单套餐授权和默认角色授权统一改为 Resource Registry `AUTH_MENU` 资源注入。升级验收必须以干净库重建结果为准，不能以手工修补后的存量库作为通过依据。

### 10.2 影响数据

升级时必须关注以下数据归属变化：

- `authorization_menu`：菜单和按钮权限由 `AUTH_MENU` 注入。
- `authorization_role_menu`：默认角色菜单授权由 `AUTH_MENU.roleCodes` 注入。
- `authorization_menu_package_item`：菜单套餐授权由 `AUTH_MENU.packageCodes` 注入。
- `frontend_menu_runtime_config`：前端页面 key、图标、隐藏状态等菜单运行时配置由 `AUTH_MENU` 注入。
- Resource Registry 相关表：记录资源声明、同步状态、审计字段和幂等键，是菜单注入的事实来源。

不再允许通过 Flyway DML 新增、修改或删除菜单、按钮权限、菜单运行时配置、菜单套餐授权和默认角色授权。

### 10.3 重建步骤

开发库和测试库升级必须按以下步骤执行：

1. 停止后端、前端和所有平台能力 app。
2. 备份目标数据库，记录备份文件、Git commit 和数据库名。
3. 删除或重建目标数据库。开发环境只允许处理当前 worktree 的 `MANGO_DB_NAME`。
4. 使用本分支最新代码启动后端，让 Flyway 从 V1 重新执行 DDL 和基础初始化。
5. 确认 `mango-resource` 相关 migration 已执行，Resource Registry 可写入资源声明和同步审计字段。
6. 启动包含 `mango-authorization-starter` 与各平台能力 starter 的部署入口，触发 `AUTH_MENU` 资源注入。
7. 启动前端，用真实登录态进入菜单树，逐项验证菜单、按钮权限和页面 key。

### 10.4 验收门禁

破坏性升级完成必须同时满足：

- Flyway 在干净库上执行成功。
- Resource Registry 资源同步成功，`AUTH_MENU` 声明已被 `mango-authorization` handler 消费。
- `authorization_menu` 不存在孤儿菜单，父级缺失必须失败。
- `authorization_menu`、`authorization_role_menu`、`authorization_menu_package_item`、`frontend_menu_runtime_config` 的运行态结果与菜单基线一致。
- `MenuBaselineTest` 通过。
- 菜单资源静态检查通过，禁止出现 `@/views`、旧套餐 code 或 `ROLE_ADMIN` 兜底授权。
- 菜单 E2E 通过，右上角通知入口、我的消息、通知中心、发送任务、业务域菜单和工作流菜单可进入。
- 工作流和通知权限 E2E 通过，不能出现通过手工授权或临时 SQL 消除的 403。

### 10.5 回滚方式

如果重建或验收失败，必须按以下方式回滚：

1. 停止当前分支服务。
2. 恢复升级前数据库备份。
3. 切回升级前 tag 或 commit。
4. 使用升级前启动方式验证登录、菜单和核心接口。

禁止用以下方式作为回滚或修复：

- 手工向菜单、权限、菜单包或角色菜单表补 SQL。
- 放宽 `mango:check` 规则绕过模块边界或菜单边界。
- 给角色追加通配权限、`ROLE_ADMIN` 或其它兜底授权。
- 跳过 Resource Registry 注入，只让页面在当前库中临时可见。
