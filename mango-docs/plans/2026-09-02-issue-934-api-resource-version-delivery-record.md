# 标准交付记录

任务：Issue #934 API Resource 版本一致性修复

## 1. 元数据

- 任务 ID：GitHub Issue #934
- 交付模式：STANDARD
- 需求影响：L2 - 已发布公共 API Resource 的静态声明与运行时声明不一致，导致 eventual reconciliation 持续失败并阻断同批声明收敛。
- 方案风险：L2 - 修改共享 API 资源声明与转换链路，需保持未显式声明版本的现有 Controller 行为不变。
- 最终风险：L2
- 工作区决策：CREATE - `fix/issue-934-api-resource-version`

## 2. 目标与范围

- 目标：让 API Resource 静态发布清单与运行时 MVC 扫描对同一资源产生一致版本，停止版本回退重试。
- 成功条件：`AdminBrandingController.publicConfig()` 运行时声明版本 2；未声明版本的 API 仍为版本 1；Resource Declaration 转换保留该版本。
- 处理范围：`ApiAccess` 注解契约、组合注解、MVC API 资源发现与 Resource Declaration 转换、系统公共配置接口声明、定向测试和授权能力说明。
- 不处理范围：不修改 Resource Registry 降版保护、数据库结构、静态清单版本 2、API 路径/权限模式或 eventual 调度策略。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R-01 | MVC API 资源扫描 | Controller 使用 `@ApiAccess` 或组合注解 | 生成的注册命令携带声明版本 | 未声明版本使用兼容默认值 1 | 公共配置接口扫描版本为 2，普通接口为 1 |
| R-02 | Resource Declaration 转换 | 注册命令包含版本 | 输出声明保留命令版本 | 缺失版本按 1 处理 | 转换结果版本与命令一致 |
| R-03 | 业务消费者 | Mango 1.0.48 静态声明版本 2 | 运行时与静态声明幂等收敛 | 不再发送版本 1 降版声明 | 回归测试证明扫描链路版本一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | R-01 | `ApiAccess.version` 默认 1；组合注解使用 AliasFor 共享版本语义 | authorization-api 注解、MVC discoverer | 删除版本属性读取与注解显式版本 |
| D-02 | R-02 | `ApiResourceRegisterCommand.version` 默认 1；converter 透传版本 | authorization-api command、resource-sync-starter converter | 恢复 converter 固定版本 1 |
| D-03 | R-03 | `AdminBrandingController.publicConfig()` 显式 `@PublicAccess(version = 2)`，不改变路径和访问模式 | mango-system-starter | 删除该注解版本参数并回退静态声明需同步的行为 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-01 | D-01 | 1 | `mango-authorization-api` 注解与组合注解 | 版本属性可声明且旧注解默认值保持 1 |
| I-02 | D-02 | 2 | `ApiAccessResourceDiscoverer`、`ApiResourceDeclarationConverter`、command | 版本从注解到声明完整透传 |
| I-03 | D-03 | 3 | `AdminBrandingController`、定向测试、授权 README | 公共配置运行时版本与静态版本均为 2 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R-01 至 R-03 | M10/M11 定向测试 | `MAVEN_OPTS='-Dmaven.repo.local=.mango/m2/repository' mvn -B -ntp -f mango/pom.xml -pl :mango-authorization-api,:mango-authorization-resource-sync-starter,:mango-system-starter test` | PASS | 28 tests passed；覆盖默认版本 1、显式版本 2、注解扫描和 Declaration 转换 |
| R-01 至 R-03 | M09 模块质量检查 | `MAVEN_OPTS='-Dmaven.repo.local=.mango/m2/repository' mvn -B -ntp -f mango/pom.xml -pl :mango-authorization-api,:mango-authorization-resource-sync-starter,:mango-system-starter verify` | PASS | 三个直接修改 Maven 模块 verify 成功 |
| R-01 至 R-03 | M09 测试质量检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS | test quality 2 files；mock audit block=0、warn=0 |
| R-01 至 R-03 | M08 能力说明审计 | `node mango-pmo/tools/check-standard-delivery-record.mjs <record>`、`node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS | STANDARD 记录、模块 README 和源码事实审计通过 |

## 7. 例外与剩余风险

- 不执行业务数据库冷启动作为本次本地模块修复的必需验证；完整业务消费项目仍需使用包含该修复的后续 Maven 版本进行冷启动回归。
