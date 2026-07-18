# Checkstyle 默认规则修复标准交付记录

## 1. 元数据

- 任务 ID：MANGO-CHECK-CHECKSTYLE-DEFAULT
- 交付模式：STANDARD
- 需求影响：L2 - 影响业务项目的静态检查结果和质量门禁可信度
- 方案风险：L2 - 改变 Mango Maven 插件委托 Checkstyle 时的配置选择
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：禁止 `mango:check` 在消费项目未配置 Checkstyle 时使用 Sun 默认规则。
- 成功条件：默认使用 Mango 规范；业务项目可以显式使用自己的规范；无效自定义配置由 Checkstyle 失败并进入现有静态工具失败策略。
- 处理范围：Mango Maven 插件的 Checkstyle 委托参数、配置解析测试、工具 README。
- 不处理范围：不调整 Mango 规则内容，不改变 PMD/SpotBugs，不修改业务代码。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | `mvn mango:check` | 项目没有 Checkstyle 配置 | 使用插件内置 Mango 规则，不出现 Sun 默认规则 | 内置规则不可读取时静态检查失败 | 委托命令显式包含 Mango 配置路径 |
| REQ-002 | 业务项目 | 提供自定义规则路径 | 自定义规则优先于 Mango 默认规则 | 路径无效时 Checkstyle 明确失败 | 两种配置属性均可覆盖默认规则 |
| REQ-003 | 业务项目模板 | 存在 `config/quality/checkstyle.xml` | 自动使用项目规则文件 | 文件不可用时不静默回退 Sun | 委托命令使用项目文件绝对路径 |
| REQ-004 | 检查报告使用者 | 执行聚合静态检查 | 日志和报告说明实际规则来源 | 无法解析来源时检查失败 | `gateMessages` 包含 Checkstyle 配置来源 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001, REQ-003 | 新增单一职责解析器，按显式 Mango 属性、标准 Checkstyle 属性、项目文件、内置规则的顺序解析 | `mango-maven-plugin` | 回滚解析器和命令参数注入 |
| DEC-002 | REQ-002 | 新增 `mango.check.checkstyleConfigLocation`，同时兼容 `checkstyle.config.location` | `CheckMojo` 参数契约 | 删除新增参数，恢复项目 POM 单点配置 |
| DEC-003 | REQ-001, REQ-004 | 内置规则复制到 `target/mango-check/checkstyle.xml` 后显式传给子 Maven，并记录来源 | 静态检查临时产物与 JSON 报告 | 恢复不注入配置的委托命令 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | DEC-001 | 1 | `mango-tools/mango-maven-plugin/src/main/java` | 配置解析器完成 |
| TASK-002 | DEC-002, DEC-003 | 2 | `CheckMojo.java` | Checkstyle goal 始终显式获得配置 |
| TASK-003 | DEC-001, DEC-002 | 3 | `mango-tools/mango-maven-plugin/src/test/java` | 默认和自定义回归测试通过 |
| TASK-004 | DEC-002 | 4 | `mango/mango-tools/README.md` | 默认行为和自定义用法清楚 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001 | M10 单元测试 | `mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckstyleConfigResolverTest,CheckMojoTest test` | 通过 | 默认规则解析与委托命令均显式使用 Mango 规则 |
| REQ-002 | M10 单元测试 | 同上 | 通过 | Mango 属性、标准属性优先级和无效路径不回退断言 |
| REQ-003 | M10 单元测试 | 同上 | 通过 | 项目规则文件绝对路径断言 |
| REQ-004 | M09 静态验证 | Maven 插件 `verify` 与 Mango Checkstyle 扫描 | 通过 | 217 项测试通过；插件包包含内置规则和新增参数描述；新增解析器无 Checkstyle 告警 |

## 7. 例外与剩余风险

- 自定义规则内容由业务项目负责；Mango 只保证选择和传递指定规则，不判断其规则质量。
