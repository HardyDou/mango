# Infra Doc 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-doc-starter`。
- 运行时协作者：`mango-infra-module-api/core/starter`、SpringDoc、Spring Boot Web。
- 真实消费者：`mango-file-preview-app`、`mango-business-app`；聚合依赖入口为 `mango-admin-starter`、`mango-app-platform-capability`。
- 行为：OpenAPI 基础信息、Authorization scheme、接口访问范围、模块分组、配置路径覆盖、路径归一和真实 `/v3/api-docs/{group}` HTTP 入口。
- 边界：本模块不执行生产鉴权，不拥有数据库、Flyway、初始化数据、菜单或业务页面。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | 3/3 通过；只覆盖常规 operation scope/security，没有 Spring 装配、模块 registry 或 HTTP 文档入口 |
| 模块分组事实 | 直接重复扫描 classpath `module.properties`，忽略 `mango.module.module-service` 的运行时配置覆盖 |
| 配置安全 | `pathsToMatch`、`contact`、`moduleGrouping` 直接暴露内部可变对象 |
| Checkstyle | 3 条 `AvoidInlineConditionals` |
| SpotBugs | 6 条 `EI_EXPOSE_REP/EI_EXPOSE_REP2` |
| 消费入口 | 没有真实 `/v3/api-docs/{group}` 请求，也没有当前 Doc 与真实 app 消费者同 reactor 编译证据 |

## 3. 缺陷红灯

新增边界测试在旧实现上形成 3 个稳定失败：

| 用例 | 治理前失败事实 |
|---|---|
| 运行时路径覆盖 | classpath 路径为 `/stale-doc-flow/`，显式配置已改为 `/doc-flow/`，生成分组仍不包含 `/doc-flow/ping` |
| 路径数组隔离 | 调用方修改 setter 入参数组后，`DocProperties.pathsToMatch` 同步被篡改 |
| 嵌套配置隔离 | 调用方修改已传入或 getter 返回的 `Contact` 后，内部联系人配置同步被篡改 |

同时新增的 API interface 方法/类型注解优先级测试在旧实现上通过，证明该行为无需修改；它作为兼容回归基线保留。

## 4. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| 模块事实分叉 | 分组 Bean 实例化时读取 `ModuleInfoRegistry` 的最终路径；registry 不可用才回退 classpath | `mango.doc`、`module.properties` 和分组名称不变 |
| 配置覆盖失效 | 显式 `mango.module.module-service` 路径完整覆盖同名 classpath 旧路径，并支持配置新增模块名 | 与 Module、Feign、authorization 共用同一部署映射结果 |
| 路径不稳定 | 复用 `ModuleInfo` 归一语义，移除尾斜杠，根路径使用 `/**`，多路径去重 | 非根模块仍同时匹配精确路径和子路径 |
| Bean 名冲突 | 模块名使用无损 Base64 URL 编码形成 Bean 名，不再把 `-`、`.`、`_` 压成同一个标识 | OpenAPI group 仍使用原始 module name |
| 配置可变状态 | 数组和嵌套对象在 setter/getter 边界防御性复制 | Spring Boot 嵌套配置绑定、默认值和读取方式不变 |
| 静态债务 | 清理 3 条 Checkstyle 和 6 条 SpotBugs | scope、安全 scheme、默认分组和 UI 依赖不变 |

## 5. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 数据/清理 | 执行入口 | 状态 |
|---|---|---|---|---|---|---|
| TC-DOC-001 | P0 | 单元 | PUBLIC/LOGIN/PERMISSION/INTERNAL 的 extension、tag、security 语义 | 无持久数据 | `MangoApiScopeOperationCustomizerTest` | AUTOMATED |
| TC-DOC-002 | P1 | 单元 | API interface 方法注解优先级保持兼容 | 无持久数据 | `MangoApiScopeOperationCustomizerTest` | AUTOMATED |
| TC-DOC-003 | P1 | 单元 | 配置数组与嵌套对象不能被调用方反向修改 | 无持久数据 | `DocPropertiesTest` | AUTOMATED |
| TC-DOC-004 | P1 | 集成 | 配置新增模块及原 sanitize 会冲突的模块名均注册独立分组 Bean | 独立 BeanDefinition registry | `ModuleGroupedOpenApiRegistrarTest` | AUTOMATED |
| TC-DOC-005 | P0 | 入口流程 | 配置覆盖后的两个归一模块路径均出现在真实 OpenAPI 分组 | 随机端口、独立 Spring context，结束自动关闭 | `DocOpenApiFlowTest` | AUTOMATED |
| TC-DOC-006 | P0 | 入口流程 | HTTP 文档包含 title/contact、Authorization scheme、公开空 security 和内部 scope/tag/security | 同上 | `DocOpenApiFlowTest` | AUTOMATED |
| TC-DOC-007 | P1 | 消费契约 | 当前 Doc/Module 与微服务消费者同 reactor 编译 | 无持久数据 | `mango-file-preview-app`、`mango-business-app` | AUTOMATED |

## 6. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | Doc 模块测试 | 3/3，fail/error/skip 0 | PASS |
| 缺陷红灯 | 新增测试运行于旧实现 | 8 条中 3 条稳定失败 | DEFECT CONFIRMED |
| 治理后回归 | 四个定向测试类 | 9/9，fail/error/skip 0 | PASS |
| 入口流程 | `DocOpenApiFlowTest`，标签 `flow` + `infra-doc` | 随机 Tomcat 端口真实 HTTP 1/1 | PASS |
| 当前源码消费者 | Doc 与 File Preview App 44 项 reactor；Doc 与 Business App 56 项 reactor | 两个 app 生产源码编译成功 | PASS |
| 聚合入口 | `mango-admin-starter`、`mango-app-platform-capability` 定向 Maven validate | 2/2 成功 | PASS |
| 直接静态 | Doc Checkstyle、SpotBugs | 0/0 | PASS |
| 正式架构 | Doc + `mango-architecture-verification` partial reactor | dependency、ArchUnit、PMD 7、blocking、聚合静态和工具失败均为 0 | PASS |
| 测试质量 | `test-quality-check`、Mockito changed-only audit | 5 个测试资产 PASS；block=0、warn=0 | PASS |

## 7. Issue #522 防回归

入口流程把当前 Doc、当前 Module API/Core/Starter、SpringDoc 和测试宿主放在同一 reactor；两个真实微服务消费者也分别与当前 Doc/Module 同 reactor 编译。结论不依赖公共 Maven 缓存中的旧 JAR，不通过清缓存掩盖生产者与消费者版本漂移。

## 8. 数据与未验证项

| 项目 | 结论 |
|---|---|
| 数据库/Flyway/init/demo | N/A；Doc 只维护应用内 OpenAPI 模型和分组 Bean |
| 生产鉴权 | N/A；Doc 只描述 security，authorization/access 承担真实访问控制 |
| 浏览器 UI | N/A；本次公共契约是 OpenAPI JSON/HTTP 入口，Knife4j UI 资源和依赖保持不变 |
| 全仓测试 | 未执行；按要求只验证 Doc、Module 协作、两个真实 app 消费者和聚合依赖入口 |

## 9. 风险分级

- 需求影响：L2。错误分组和 security 描述影响多个部署入口的公共开发者契约，但不改变生产鉴权结果或持久数据。
- 方案风险：L2。方案改变共享 starter 的分组实例化和 Module 集成，公开配置与 endpoint 不变，可通过单提交回退。
- 最终风险：L2。由旧实现红灯、同一回归集、真实 HTTP、当前源码消费者和正式架构门禁共同覆盖。
