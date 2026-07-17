# Admin Starter 历史债务验收证据

## 1. 基线与环境

- 日期：2026-07-17（Asia/Shanghai）
- 分支：`refactor/admin-starter-debt`
- 起始源码：`2ae2af299492d2d3f5b87e1ef10f305190416815`
- 对比基线：`origin/main`（验证时 `1327ce531e7fef17b220bd43a46b5786183364d0`）
- Java：OpenJDK 21.0.10；Maven：3.9.13；Node.js：26.5.0；pnpm：11.10.0
- 工作区：slot 13；后端端口 `18013`；一次性数据库 `mango_dev_mango_admin_starter_debt_013`

## 2. 自动化与门禁

| 范围 | 命令/步骤 | 结果 |
|---|---|---|
| 架构规则与 Maven 插件 | `mvn -f mango/pom.xml -pl ':mango-architecture-rules,:mango-maven-plugin' test` | PASS；Architecture rules 165 条、plugin 211 条 |
| Admin Starter 契约 | `mvn -f mango/pom.xml -pl ':mango-admin-starter' test package` | PASS；5 条测试 |
| 零源码真实 Reactor | `mvn -f mango/pom.xml -pl ':mango-admin-starter,:mango-architecture-verification' -DskipTests -Dmango.architecture.skip=false -Dmango.architecture.mode=full -Dmango.architecture.requireFullReactor=false -Dmango.check.baseRef=origin/main verify` | PASS；dependency/ArchUnit/PMD/blocking 均为 0 |
| CLI 大输出 | `node mango-ui/packages/mango-cli/scripts/check-cli.mjs` | PASS；1280 KiB 前置安装输出后仍进入 Spring Boot 启动命令 |
| 完整源码安装 | `MAVEN_ARGS='-Dmango.architecture.base=origin/main -Dmango.check.baseRef=origin/main' node mango-ui/packages/mango-cli/src/index.mjs dev start backend` | PASS；完整 Reactor `BUILD SUCCESS`，安装和启动输出直接写入 54 MiB app 日志 |
| 完整 changed 门禁 | `mvn -f mango/pom.xml -DskipTests -Dmango.architecture.base=origin/main -Dmango.check.baseRef=origin/main verify` | PASS；211/211 Reactor，dependency 2、ArchUnit 58、PMD 245 均为已登记 baseline，blocking 0；静态报告覆盖 17 个变更文件，total/baseline 16956、new 0、tool failure 0；总耗时 7 分 12 秒 |

## 3. 构件边界

`mango-admin-starter` 的直接生产依赖全部是唯一的本地 `io.mango` `*-starter`，不包含 common、API、core、support、app 或 `starter-remote`。README 清单由契约测试按 POM 顺序锁定。

构建后的 JAR 只有以下有效内容：

```text
META-INF/MANIFEST.MF
META-INF/mango/module.properties
META-INF/maven/io.mango/mango-admin-starter/pom.properties
META-INF/maven/io.mango/mango-admin-starter/pom.xml
```

没有 Java class、application 配置、Flyway migration、正式资源或 demo seed。

## 4. Fresh DB 与真实入口

- Repo CLI 启动真实 `MangoMonolithApplication`，15.316 秒完成 Spring Boot 启动；进程 PID 41622 归属当前 worktree。
- `GET http://127.0.0.1:18013/actuator/health` 返回 HTTP 200、`status=UP`，数据库、磁盘、ping 和 SSL health 均为 UP；未配置 discovery client，因此 discovery 为 UNKNOWN，不影响整体健康。
- Fresh DB 完成 20 组 Flyway history，生成 220 张表；资源初始化后有 1586 条 registry 记录，覆盖 19 个资源模块。
- `GET http://127.0.0.1:18013/v3/api-docs` 返回 HTTP 200、OpenAPI 3.1.0、标题 `Mango API`、593 个 paths，证明聚合 starter 的 Web 装配入口可用。
- 启动完成后的日志未发现新的 `ERROR` 或未处理异常。

## 5. 不适用项与剩余风险

- 本次不修改页面、路由、菜单、权限、租户或业务 API 语义，浏览器 E2E 不适用。
- 完整 changed 报告保留仓库已登记历史存量；本任务不新增 blocking issue，也不修改 baseline 预算。
- 首次对已提交变更执行完整门禁时识别出 3 个新增 Checkstyle 问题；均通过拆分复杂逻辑和移除行内条件表达式消除，最终报告 new 0，未增加 suppression 或 baseline。
- AI 通用 `/ai/sse` 向 Realtime 的迁移不属于本任务，已登记 GitHub Issue #567。
