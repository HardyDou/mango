# Issue 105 回归测试报告

- Issue: #105 治理：统一 infra API/Starter 边界与 core 依赖规则
- 日期: 2026-06-08
- 分支: `feature/issue-105-infra-api-starter-boundary`
- 结论: 通过

## 改动摘要

- 新增 `mango-infra-persistence-api`，承载持久化轻量契约、实体基类、CRUD 契约、数据源上下文和 Resolver 接口。
- `mango-infra-persistence-starter` 改为依赖 API 模块，继续承载自动配置、Flyway、MyBatis-Plus、数据源运行时实现。
- `mango-job-core` 从依赖 `mango-infra-persistence-starter` 调整为依赖 `mango-infra-persistence-api`，并显式声明自身使用的 MyBatis/Jackson 依赖。
- `mango:check dependency` 在 Maven reactor session 存在时只分析本次 reactor 项目的 POM，避免局部模块检查被无关历史债务阻塞。
- 更新 PMO 模块规范和 persistence README，固化 infra `*-api` / `*-starter` 边界。

## 验证命令

```bash
mvn -q -f mango/pom.xml -pl mango-tools/mango-maven-plugin -am install -DskipTests -Dcheckstyle.skip=true
```

```bash
mvn -q -f mango/pom.xml -pl mango-infra/mango-infra-persistence/mango-infra-persistence-api,mango-infra/mango-infra-persistence/mango-infra-persistence-starter,mango-infra/mango-infra-persistence/mango-infra-persistence-web-starter,mango-platform/mango-job/mango-job-core,mango-tools/mango-maven-plugin -am test -Dcheckstyle.skip=true -Dsurefire.failIfNoSpecifiedTests=false
```

```bash
mvn -q -f mango/pom.xml -pl mango-platform/mango-job/mango-job-core -am mango:check -Drule=dependency -DskipTests -Dcheckstyle.skip=true -Doutput=json -DreportFile=/tmp/issue105-dependency-scoped.json
```

```bash
mvn -q -f mango/pom.xml -pl mango-app/monolith/mango-monolith-app -am test -DskipTests -Dcheckstyle.skip=true
```

```bash
git diff --check
```

## 结果

| 验证项 | 结果 |
|---|---|
| Maven plugin 安装 | 通过 |
| persistence API/starter/web-starter + job-core + maven-plugin 测试 | 通过 |
| job-core scoped dependency check | 通过，报告 `passed=true` |
| monolith 编译回归 | 通过 |
| diff whitespace 检查 | 通过 |
| UI/证据截图 | 通过，见 `regression-report.png` |

## 回归范围

- 持久化基础契约包路径：`io.mango.infra.persistence.api.*`
- 多数据源上下文与事务内切换保护
- Job core 编译与依赖边界
- Maven dependency checker 局部 reactor 行为
- monolith 应用装配编译

## 未验证项和风险

- 未执行全仓 `mango:check dependency`，仓库仍存在多个历史 `core -> *-starter` 债务；本次按 #105 范围验证 Job scoped check 和 checker 作用域。
- 未启动完整后端服务做业务页面交互，因为本 Issue 是架构边界治理；UI 证据采用静态回归报告截图。
