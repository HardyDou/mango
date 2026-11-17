# 后端测试执行

本文说明后端测试在本地和 CI 中的执行入口、依赖真实外部服务的测试怎么隔离，以及「测试报告显示成功但一个用例都没跑」这类假绿现象的识别方法。规范口径见 [后端测试规范](../../../mango-pmo/rules/backend/08-test.md)。

## 执行入口

| 场景 | 命令 | 说明 |
|---|---|---|
| 本地跑单个模块 | `cd mango/mango-platform/<group>/<module> && mvn test` | 全仓统一绑定 Surefire 3.2.5，用例会被真正执行 |
| 本地跑指定类 | `mvn -pl <module> -Dtest=<ClassName> -Dsurefire.failIfNoSpecifiedTests=false test` | 从构建根 `mango/pom.xml` 执行时用 `-pl` 指定模块 |
| CI 跑改动模块 | `Changed module tests` job | 由 preflight 的 `maven_projects` 决定范围；为空时按改动路径回退定位，只跑本次改动的模块 |
| 发版与制品安装 | `mvn ... -DskipTests install` | 打包、安装、发布链路仍然跳过测试，测试只在「人改了代码」时执行 |

Surefire 版本与默认排除组写在构建根 `mango/pom.xml` 的 `pluginManagement`：

```xml
<maven-surefire-plugin.version>3.2.5</maven-surefire-plugin.version>
...
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>${maven-surefire-plugin.version}</version>
  <configuration>
    <excludedGroups>real</excludedGroups>
  </configuration>
</plugin>
```

不要在任何模块里自行声明更低版本的 Surefire：2.17 及更早版本没有 JUnit Platform provider，`mvn test` 会显示 `Tests run: 0` 并给出 `BUILD SUCCESS`。

## 依赖真实外部服务的测试

默认执行依赖 H2、内嵌容器和进程内组件。需要本机 Redis、MySQL 或其他外部服务的测试打 `@Tag("real")`，由 `excludedGroups` 默认排除：

```java
@Tag("real")
class RedisRealIntegrationTest {
}
```

需要特定环境变量的测试，用 `@EnabledIfEnvironmentVariable` 表达前置条件，不要靠注释说明：

```java
@EnabledIfEnvironmentVariable(named = MANGO_DB_NAME, matches = "mango_dev_.*")
```

本地要执行这类测试时显式打开分组：`mvn test -Dgroups=real -DexcludedGroups=`。

## 假绿排查

`mvn test` 通过但测试报告为空，或报告里 `tests="0"`，说明测试根本没跑。CI 用 `mango-pmo/tools/check-surefire-execution.mjs` 把这件事变成硬失败：

| 检查结果 | 含义 |
|---|---|
| `SUREFIRE_REPORT_MISSING` | 模块有可执行测试类，但没有 `target/surefire-reports/TEST-*.xml` |
| `SUREFIRE_ZERO_TESTS` | 报告存在但 `tests` 求和为 0，通常是 Surefire 版本过低 |
| `没有模块包含可执行测试` | 本次改动没有落在带测试的模块上，只告警不失败 |

本地用同一份规则自查：

```bash
node mango-pmo/tools/check-surefire-execution.mjs --modules mango/mango-platform/mango-workflow/mango-workflow-core
node mango-pmo/tools/check-surefire-execution.mjs --changed-since origin/main --root mango
```

## 已知存量红测

开启实际执行后暴露出的存量失败测试记录在 [Issue #1301](https://github.com/HardyDou/mango/issues/1301)。这些模块被改动时 `Changed module tests` 会失败，属于真实信号，不要用 `@Disabled`、`-DskipTests` 或删除测试的方式绕过；修复口径见该 issue。
