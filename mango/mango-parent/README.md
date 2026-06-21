# Mango Parent

## 1. 概览
`mango-parent` 是 Mango 后端 Maven 父 POM，统一 Java 版本、Spring Boot/Spring Cloud BOM、基础依赖版本、构建插件版本和发布仓库配置。

主要使用者是 Mango 后端模块维护者、业务模块开发者和 CI 构建流程。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 新增后端模块时继承统一构建基线 | Maven 依赖 / HTTP API / Java API |
| 升级 Spring Boot、Spring Cloud、MyBatis-Plus、Jackson、JWT、BouncyCastle 等全仓依赖版本 | Maven 依赖 / HTTP API / Java API |
| 统一 Maven compiler、surefire、checkstyle、spotbugs、pmd 等插件版本 | Maven 依赖 / HTTP API / Java API |
| 发布 release 或 snapshot 到公司 Maven 仓库 | Maven 依赖 / HTTP API / Java API |


## 3. 能力边界
- 不提供运行时代码、Controller、Service、数据库 migration 或业务配置。
- 不负责单个模块是否引入某个依赖；这里只做版本和插件管理。
- 不替代 `mango-admin-starter` 的运行时装配。

## 4. 模块入口
`mango-parent/pom.xml` 管理：

- Java 17 编译基线。
- Maven 坐标版本变量 `revision`，默认 `1.0.0-SNAPSHOT`。
- Spring Boot `3.5.14`、Spring Cloud `2025.0.1`。
- Spring Cloud Alibaba Nacos discovery/config starter 版本。
- MyBatis-Plus、springdoc、Knife4j、Swagger annotations、Lombok、JUnit。
- 安全敏感依赖版本：JJWT、BouncyCastle、commons-compress、commons-fileupload、plexus-utils。
- Maven compiler、surefire、checkstyle、spotbugs、pmd 等插件版本。
- 发布仓库：`maven-releases`、`maven-snapshots`。

## 5. 接入方式
Mango 后端模块通过父 POM 或 reactor 继承版本管理：

```xml
<parent>
    <groupId>io.mango</groupId>
    <artifactId>mango-parent</artifactId>
    <version>${revision}</version>
</parent>
```

业务模块如果在 Mango reactor 内开发，通常不需要重复声明第三方依赖版本；只在 `mango-parent` 没管理且确实需要固定版本时补充。

## 6. 配置说明
无运行时配置。可被构建命令覆盖的 Maven 属性包括：

| 属性 | 默认值 | 含义 |
|------|--------|------|
| `revision` | `1.0.0-SNAPSHOT` | 当前构建版本。 |
| `java.version` | `17` | Java 基线。 |
| `spring-boot.version` | `3.5.14` | Spring Boot BOM。 |
| `spring-cloud.version` | `2025.0.1` | Spring Cloud BOM。 |
| `spring-cloud-alibaba.version` | `2023.0.3.3` | Spring Cloud Alibaba Nacos discovery/config starter 版本。 |
| `mybatis-plus.version` | `3.5.16` | MyBatis-Plus 版本。 |
| `junit.version` | `5.10.1` | JUnit 版本。 |

修改这些属性属于全仓影响，必须扩大验证范围。

## 7. API 与扩展
无 Java API。对外能力是 Maven `dependencyManagement`、`pluginManagement` 和 `distributionManagement`。

## 8. 数据与初始化
无数据库、migration 和初始化数据。

## 9. 管理入口
无菜单、权限和租户能力。

## 10. 快速开始
1. 新模块继承 `mango-parent`。
2. 只声明必要依赖，不重复写 parent 已管理的版本。
3. 运行模块 `mvn test`。
4. 如果新增第三方依赖版本，把版本统一登记到 parent，并说明为什么不是模块私有版本。

## 11. 问题排查
- 依赖版本冲突：先查 `mvn dependency:tree`，再判断是否应在 parent 统一管理。
- 修改 parent 后很多模块失败：这是预期风险，parent 是全仓构建基线。
- 发布失败：检查 Maven `settings.xml` 中 `maven-releases`、`maven-snapshots` 凭据。

## 12. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 13. 补充资料
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
