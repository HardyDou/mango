# Issue #1270 证据：mango:check 的 globalEntityManifest 路径绑定

- 任务：MANGO-1270
- 分支 / worktree：`fix/1270-check-mojo-path-binding`，`/data/Mango/worktrees/mango-1270-check-mojo-path-binding`（slot 6）
- 环境：JDK 21.0.13、Apache Maven 3.9.12、离线本地仓库 `mango-006` 版本限定符
- 结论：修复前 Maven 3 无法把 POM 字符串绑定到 `Path` 参数，修复后同一配置正常进入 `mango:check` 规则执行

## 1. 缺陷复现（修复前）

复现工程：一次性消费项目 `.runtime/projects/check-manifest-binding/pom.xml`（未提交），配置与 Issue 一致：

```xml
<plugin>
  <groupId>io.mango.tools.maven.plugin</groupId>
  <artifactId>mango-maven-plugin</artifactId>
  <version>mango-006</version>
  <configuration>
    <globalEntityManifest>${maven.multiModuleProjectDirectory}/business-pmo/global-entity-exceptions.json</globalEntityManifest>
  </configuration>
</plugin>
```

命令：

```bash
mvn -B -ntp -o -Dmaven.repo.local=/data/Mango/mango/.mango/m2/repository \
  io.mango.tools.maven.plugin:mango-maven-plugin:mango-006:check
```

结果 `BUILD FAILURE`：

```text
Unable to parse configuration of mojo io.mango.tools.maven.plugin:mango-maven-plugin:mango-006:check
for parameter globalEntityManifest: Cannot create instance of interface java.nio.file.Path:
java.nio.file.Path.<init>()
```

同一时刻生成的插件描述符（`target/classes/META-INF/maven/plugin.xml`）中 `check` mojo：

```xml
<parameter>
  <name>globalEntityManifest</name>
  <type>java.nio.file.Path</type>
```

## 2. 回归用例反证（修复前）

```bash
mvn -f mango/pom.xml -o -pl :mango-maven-plugin -Drevision=mango-006 \
  -Dmaven.repo.local=/data/Mango/mango/.mango/m2/repository \
  -Dtest=PluginDescriptorPathParameterTest,CheckMojoBindingTest test
```

结果 `Tests run: 2, Failures: 1, Errors: 1`：

```text
PluginDescriptorPathParameterTest.everyDescriptorParameterUsesPlexusBindableType:
parameters must be java.io.File, not java.nio.file.Path: [check:globalEntityManifest]
CheckMojoBindingTest.mavenConfiguratorBindsGlobalEntityManifestAsFile:
java.lang.ClassCastException: class sun.nio.fs.UnixPath cannot be cast to class java.io.File
```

## 3. 修复后验证

同一复现工程、同一命令：

```text
[INFO] Status: PASS
[INFO] Gate: all
[INFO] Issues: 0
[INFO] Check completed. 0 issue(s) found.
[INFO] BUILD SUCCESS
```

描述符中 `check` mojo 变为 `<type>java.io.File</type>`；`architecture` mojo 保持 `java.io.File`。

模块回归：

```bash
mvn -f mango/pom.xml -o -pl :mango-maven-plugin -Drevision=mango-006 \
  -Dmaven.repo.local=/data/Mango/mango/.mango/m2/repository test
# Tests run: 277, Failures: 0, Errors: 0, Skipped: 12
mvn -f mango/pom.xml -o -pl :mango-maven-plugin -Drevision=mango-006 -DskipTests verify
# BUILD SUCCESS
```

## 4. 未覆盖

- 未在真实业务仓 `mango-baohan-risk` 上执行 `mvn -f backend/pom.xml verify`；业务侧需要升级到包含本修复的 Maven 插件版本后重跑，命令与期望结果见交付记录。
- 未发布 Maven 物料：升级与 `CHANGELOG` 由发布流程处理。
