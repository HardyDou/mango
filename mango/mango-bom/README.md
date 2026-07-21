# Mango BOM

## 1. 概览

`mango-bom` 是业务项目使用 Mango 后端能力时的统一 Maven 版本入口。它管理同一发布批次的全部可消费 Mango JAR，并锁定 Mango 已验证的 Spring Boot、Spring Cloud、Flyway、文件预览、Maven 工具和测试依赖版本。

## 2. 业务项目接入

业务项目保留自己的 parent，在根 POM 中导入 BOM：

```xml
<properties>
    <mango.version>1.0.25</mango.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.mango</groupId>
            <artifactId>mango-bom</artifactId>
            <version>${mango.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

导入后，Mango 依赖不再声明版本：

```xml
<dependency>
    <groupId>io.mango.platform.notice</groupId>
    <artifactId>mango-notice-starter</artifactId>
</dependency>
```

升级后端平台时只修改 `mango.version`，然后重新构建并重启业务 JVM。示例中的 `1.0.25` 是首个计划包含 `mango-bom` 的版本，发布前不可在业务项目使用。不要同时导入另一份 Spring Boot 或 Spring Cloud BOM 后再局部覆盖 Mango 已锁定的兼容版本；确需覆盖时，应通过业务依赖树和启动回归证明兼容性。

## 3. Parent 接入

完全接受 Mango 构建插件和 Java 基线的项目可以继承同版本 `mango-parent`。`mango-parent` 已导入 `mango-bom`，业务依赖同样不写版本。

## 4. 验证

```bash
mvn -f mango/mango-bom/pom.xml help:effective-pom
mvn -f mango/mango-parent/pom.xml help:effective-pom
mvn -f mango/pom.xml verify
```

使用 `mvn dependency:tree` 确认业务项目没有混入不同 Mango 批次，并确认 Flyway、Jackson、Netty、Tomcat 等版本与 BOM 一致。

## 5. 边界

- BOM 只管理依赖版本，不注入运行时代码、插件执行或业务配置。
- Maven 插件版本由 `mango-parent` 的 `pluginManagement` 管理。
- `mango-app/**` 是部署入口，不属于默认业务依赖清单。
- npm 包和 `@mango/cli` 使用各自发布锁，不由 Maven BOM 管理。

## 6. 相关文档

- [Mango Parent](../mango-parent/README.md)
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
