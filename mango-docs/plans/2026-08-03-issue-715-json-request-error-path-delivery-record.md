# Issue #715 JSON 请求体错误字段路径标准交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #715
- 交付模式：STANDARD
- 需求影响：L2 - 调整所有接入 Mango Web Starter 的 JSON 请求体反序列化失败文案。
- 方案风险：L2 - 复用 Jackson 异常引用链，不改变成功请求、HTTP 状态或统一响应结构。
- 最终风险：L2
- 工作区决策：REUSE - 与 Issue #705 共用 Maven `1.0.33-mango-018-SNAPSHOT` 业务验收批次。

## 2. 目标与范围

- 目标：让业务调用方在安全边界内直接定位 JSON 未知字段、类型错误和日期时间格式错误。
- 成功条件：响应包含完整嵌套路径与稳定错误类别；无路径的畸形 JSON 返回安全顶层错误；不回显用户字段值、请求体、异常堆栈或类全限定名。
- 处理范围：`mango-infra-web-starter` 全局请求体异常映射、真实 HTTP 入口回归、Web 能力文档。
- 不处理范围：不改变 `R` 响应结构、HTTP 400 语义、业务 DTO、宿主 Jackson 的未知字段开关和服务端异常日志策略。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | Spring MVC JSON 接口 | 严格未知字段检查开启，嵌套列表元素包含未知字段 | 返回完整对象属性与数组索引路径 | HTTP 400，`请求字段 <path> 不受支持` | 精确返回 `materials[0].materialCategoryCode` |
| R2 | Spring MVC JSON 接口 | 嵌套列表字段类型与 DTO 不匹配 | 返回完整路径和白名单目标类型类别 | HTTP 400，`类型不正确，期望 <category>` | 路径含数组索引且类别不包含 Java 类名 |
| R3 | Spring MVC JSON 接口 | 日期时间文本无法按 Mango 格式解析 | 返回日期字段路径 | HTTP 400，`日期时间格式不正确` | 精确返回日期字段且不回显输入值 |
| R4 | Spring MVC JSON 接口 | JSON 语法不完整且不存在可靠字段路径 | 不猜测字段 | HTTP 400，安全顶层格式错误 | 不返回请求体、堆栈或异常类名 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R1-R4 | 沿 cause 链识别 Jackson `UnrecognizedPropertyException`、`InvalidFormatException`、`MismatchedInputException` | `GlobalExceptionHandler` | 恢复原统一文案分支 |
| D2 | R1-R4 | 仅接受受限字段名、非负数组索引和最长 256 字符路径；目标类型映射为固定白名单类别 | `GlobalExceptionHandler` | 移除路径格式化逻辑并使用顶层错误 |
| D3 | R1 | 不修改宿主 `FAIL_ON_UNKNOWN_PROPERTIES`；入口测试显式开启严格未知字段检查以验证异常映射 | Web Starter 测试 | 移除测试属性，不影响运行时配置 |
| D4 | R1-R4 | 保持 `R<Void>`、HTTP 400、现有服务端日志策略和成功请求行为不变 | Web Starter | 恢复异常处理方法 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1-D2 | 1 | `GlobalExceptionHandler.java`、Starter `pom.xml` | 四类异常均映射到安全稳定文案 |
| I2 | D1-D4 | 2 | `WebBoundaryIntegrationTest.java` | 随机端口真实 HTTP 测试覆盖四类失败和敏感值不回显 |
| I3 | D3-D4 | 3 | Web README、能力地图 | 能力边界、配置责任和验证基线可发现 |
| I4 | D1-D4 | 4 | 本记录 | 验收命令、结果和剩余风险完整回填 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1-R4 | 安全格式化单元测试、随机端口 Tomcat 入口回归与模块验证 | `mvn -f mango/pom.xml -pl :mango-infra-web-starter verify -Drevision=1.0.33-mango-018-SNAPSHOT` | PASS，36 条测试全部通过，其中 `WebBoundaryIntegrationTest` 9 条、`GlobalExceptionHandlerTest` 1 条 | Surefire XML：errors=0、failures=0；Maven exit 0 |
| R1-R4 | 测试质量门禁 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | PASS | `Test quality PASS: 1 file(s)` |
| R1-R4 | Mock 审计 | `node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS | block=0、warn=0 |
| R1-R4 | 文档与差异门禁 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`、`git diff --check` | PASS | 三条命令 exit 0 |
| R1-R4 | 本地业务验收物料 | `mvn -f mango/pom.xml -pl :mango-infra-web-starter -am install -DskipTests -Drevision=1.0.33-mango-018-SNAPSHOT` | PASS | 本地 Starter JAR SHA-256 `526551143869f4e832b4f9da7f1fe7766c18ea3d155afac10dce28fc9f109fe3` |

## 7. 例外与剩余风险

- 未知字段是否触发异常仍由宿主 Jackson 严格模式决定；本次不扩大为全局兼容性变更。
- 非 ASCII、超过 64 字符的单段字段名或超过 256 字符的路径会安全回退到顶层格式错误，避免反射不可信字段名。
- 正式版本发布和 Issue #715 关闭等待业务对本地 SNAPSHOT 联合验收后执行。
