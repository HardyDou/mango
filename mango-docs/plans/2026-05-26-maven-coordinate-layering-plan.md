# 2026-05-26 Maven 坐标分层调整计划

## 1. 目标

统一调整 Mango 后端 Maven 模块坐标，使模块发布到 Maven 仓库后按顶层领域和能力模块分层展示。

## 2. 范围

- `mango/pom.xml`
- `mango/mango-parent/pom.xml`
- `mango/mango-common/pom.xml`
- `mango/mango-infra/**/pom.xml`
- `mango/mango-platform/**/pom.xml`
- `mango/mango-extension/**/pom.xml`
- `mango/mango-app/**/pom.xml`
- `mango/mango-tools/**/pom.xml`
- `mango/mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/check/CheckMojo.java`

## 3. 不做什么

- 不调整源码目录结构。
- 不调整 Java package 命名。
- 不修改业务接口、数据库、菜单、页面或权限。
- 不改 Maven 仓库服务端路径规则。

## 4. 设计输入

- 用户要求：`mango-infra-kv` 可作为一层，下面才是 `api/core`。
- PMO 模块分层规范：`mango-app`、`mango-platform`、`mango-infra`、`mango-tools` 分层。
- Maven 仓库路径规则：发布路径由 `groupId/artifactId/version` 决定。

## 5. 设计说明

### 5.1 影响模块

所有后端 Maven POM 中的 Mango 内部坐标和 Mango Maven Plugin 边界检查逻辑。

### 5.2 坐标策略

- 根聚合和基础父 POM 保持 `io.mango`。
- 顶层聚合模块使用顶层 groupId，例如 `io.mango.infra`、`io.mango.platform`。
- 能力模块使用能力 groupId，例如 `io.mango.infra.kv`、`io.mango.platform.identity`。
- 子层 `api/core/support/starter/starter-remote` 继承能力 groupId。
- `artifactId` 保持完整模块名，例如 `mango-infra-kv-api`，不改为 `api`。

### 5.3 接口变化

无 HTTP API、Java API 行为变化。Maven 坐标变化会影响外部消费者依赖声明。

### 5.4 数据变化

无数据库变化，无 migration。

### 5.5 菜单/页面/权限变化

无。

### 5.6 测试范围

- Maven reactor `validate`，确认 POM 坐标、父子模块和依赖管理可解析。
- 定向模块测试，确认改动不会破坏基础编译链路。
- PMO 交付台账检查。

## 6. 风险与限制

- 外部项目如果依赖旧坐标 `io.mango:*`，需要迁移到新的分层 groupId。
- Maven 本地仓库和远端仓库会出现旧坐标与新坐标并存，发布版本策略需要后续明确。
- 本次只调整 Maven 坐标，不改变 Java package，避免扩大源码迁移范围。
