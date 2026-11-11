# Mango Parent

## 1. 概览
`mango-parent` 是 Mango 后端 Maven 父 POM，统一 Java 基线、构建插件和发布仓库配置，并导入同版本的 `mango-bom` 提供完整依赖版本目录。

主要使用者是 Mango 后端模块维护者、业务模块开发者和 CI 构建流程。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 新增后端模块时继承统一构建基线 | Maven 依赖 / HTTP API / Java API |
| 通过 `mango-bom` 统一 Spring Boot、Spring Cloud、Flyway、Mango 模块及第三方依赖版本 | Maven dependencyManagement |
| 统一 Maven compiler、surefire、checkstyle、spotbugs、pmd 等插件版本 | Maven 依赖 / HTTP API / Java API |
| 发布 release 或 snapshot 到公司 Maven 仓库 | Maven 依赖 / HTTP API / Java API |


## 3. 能力边界
- 不提供运行时代码、Controller、Service、数据库 migration 或业务配置。
- 不负责单个模块是否引入某个依赖；依赖版本目录由 `mango-bom` 管理，parent 负责导入。
- 不替代 `mango-admin-starter` 的运行时装配。

## 4. 模块入口
`mango-parent/pom.xml` 管理：

- Java 21 编译基线，使用 `--release 21` 生成与运行时一致的字节码。
- Maven 坐标版本变量 `revision`，默认 `1.0.0-SNAPSHOT`。
- 导入同版本 `io.mango:mango-bom`，依赖版本清单不在 parent 内重复维护。
- Maven compiler、surefire、checkstyle、spotbugs、pmd 等插件版本。
- 发布仓库：`maven-releases`、`maven-snapshots`。

## 5. 接入方式
完全接受 Mango Java 与插件基线的项目可以继承 parent：

```xml
<parent>
    <groupId>io.mango</groupId>
    <artifactId>mango-parent</artifactId>
    <version>${revision}</version>
</parent>
```

只需要依赖版本管理、需要保留自有 parent 的业务项目，应直接导入 [`mango-bom`](../mango-bom/README.md)。两种方式均不应为 BOM 已管理的依赖重复声明版本。

业务仓直接继承 `mango-parent` 时，仓库里不需要也没有 Mango 主仓的 `mango-tools/` 目录：

- Checkstyle、PMD、SpotBugs 的规则集由 `mango-maven-plugin` 从自身 jar 提取到业务仓 `target/mango-check/`，`mango-parent` 不再指向文件系统路径。
- 需要启用后端质量门禁时，复制 CLI 模板的 `backend/architecture-verification` 模块，声明 `mango.check.*` 和 `mango.architecture.*` 属性后执行 `mvn verify`。
- 需要自定义规则时使用 `mango.check.checkstyleConfigLocation`、`mango.check.pmdRulesets`、`mango.check.spotbugsIncludeFilterFile` 指向业务仓自己的文件，不要伪造 `mango-tools/...` 目录。

质量门禁可调整项：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `mango.check.rulesetDir` | 空 | 统一规则目录，读取 `checkstyle.xml`、`pmd-mango.xml`、`spotbugs-security.xml` |
| `mango.check.checkstyleConfigLocation` | 空 | 自定义 Checkstyle 规则文件 |
| `mango.check.pmdRulesets` | 空 | 自定义 PMD 规则文件；插件会写入 `mango.check.pmdRulesetsPath`，由 parent 的 PMD 插件配置读取 |
| `mango.check.spotbugsIncludeFilterFile` | 空 | 自定义 SpotBugs include filter |
| `mango.check.pmdEnabled` | `true` | 是否执行 PMD |
| `mango.check.pmdTypeResolution` | `true` | PMD 类型解析；PMD 7 支持 Java 8-21 class 文件 |
| `mango.check.checkstyleEnabled` | `true` | 是否执行 Checkstyle |
| `mango.check.spotbugsEnabled` | `true` | 是否执行 SpotBugs |

PMD 7（maven-pmd-plugin 3.28+）支持 Java 8-21，规则集为 `pmd-mango.xml`。PMD 报告出现处理错误时质量门禁按工具失败阻断。

`pmd-mango.xml` 只保留能定位真实缺陷、资源泄漏、并发正确性问题和基础安全问题的检查。命名风格、Javadoc 形式、NPath 复杂度、重复字面量、微性能和笼统异常捕获不进入门禁，因为这些检查误报高、修复收益低，或与 Mango 架构规则冲突。方法圈复杂度保留 `15`/`80` 的高阈值。存量代码继续通过门禁应使用 `-Dmango.check.gate=no-new-violations` 和 baseline，不得关闭门禁。

规则文件解析顺序：显式 `mango.check.*` 参数 → `rulesetDir/<规范文件名>` → `config/quality/` 下的项目文件（`checkstyle.xml`、`pmd-mango.xml` 或 `pmd.xml`、`spotbugs-security.xml` 或 `spotbugs.xml`）→ 插件内置规则。

## 6. 配置说明
无运行时配置。可被构建命令覆盖的 Maven 属性包括：

| 属性 | 默认值 | 含义 |
|------|--------|------|
| `revision` | `1.0.0-SNAPSHOT` | 当前构建版本。 |
| `java.version` | `21` | Java 基线。 |
| `spring-boot.version` | `3.5.14` | Spring Boot Maven 插件版本，需与 BOM 基线保持一致。 |

修改这些属性属于全仓影响，必须扩大验证范围。

## 7. API 与扩展
无 Java API。对外能力是 Maven `dependencyManagement`、`pluginManagement` 和 `distributionManagement`。

## 8. 数据与初始化
无数据库、migration 和初始化数据。

## 9. 管理入口
无菜单、权限和租户能力。

## 10. 快速开始
1. 新模块继承 `mango-parent`。
2. 只声明必要依赖，不重复写 BOM 已管理的版本。
3. 运行模块 `mvn test`。
4. 如果新增或升级依赖，把版本统一登记到 `mango-bom`，并通过 BOM 覆盖测试。

## 11. 问题排查
- 依赖版本冲突：先查 `mvn dependency:tree`，再判断是否应在 `mango-bom` 统一管理。
- 修改 parent 后很多模块失败：这是预期风险，parent 是全仓构建基线。
- 发布失败：检查 Maven `settings.xml` 中 `maven-releases`、`maven-snapshots` 凭据。

## 12. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 13. 补充资料
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
