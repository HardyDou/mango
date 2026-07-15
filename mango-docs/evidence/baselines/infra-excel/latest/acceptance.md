# Infra Excel 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-excel-starter`。
- 当前源码协作者：`mango-infra-persistence-web-starter` 的 Excel 公共契约、Spring MVC、Apache POI。
- 行为：`.xlsx` 导入映射、转换、失败工作簿、导出字段筛选、表头、单元格类型、下载响应和自动配置。
- 边界：本模块无数据库、Flyway、初始化/demo 数据、菜单或浏览器页面；仓内没有其它 Maven 模块直接依赖该 Starter。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | 12/12 通过；11 条 Adapter 测试和 1 条 MockMvc 导入测试 |
| 真实服务入口 | 无随机端口 HTTP 上传或下载，MockMvc 未证明 Servlet multipart 与下载响应 |
| 导出公共配置 | `include`、`exclude`、`headGenerator` 被静默忽略，全部字段按字符串写出 |
| 映射冲突 | 同一 DTO 的重复固定列号不会失败，后续字段会复用/覆盖同一列 |
| 失败工作簿 | “失败原因”列只按头部行宽度定位，可能覆盖更宽数据行中的原值 |
| 静态债务 | Checkstyle 1 条 `SeparatorWrap`；SpotBugs 0 |

## 3. 缺陷红灯

在修复生产代码前，新增测试形成 4 个稳定缺陷红灯；真实 HTTP 导入基线同时保持通过：

| 用例 | 治理前失败事实 |
|---|---|
| 导出字段与类型 | 字段筛选和多级表头被忽略，金额被写成字符串 |
| 真实 HTTP 导出 | 下载到的工作簿包含未请求字段，金额不是原生数字单元格 |
| 失败工作簿 | 标题只有一列、数据有三列时，“失败原因”覆盖第二列原始扩展值 |
| 重复固定列 | 两个字段都声明 `idx=0` 时未拒绝冲突 |

## 4. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| 导出配置失效 | `include`/`exclude` 使用 Java 字段名筛选；未知字段和空结果明确失败 | 空筛选仍导出全部字段，文件名和 Sheet 默认值不变 |
| 表头与类型丢失 | 独立 `PoiExcelExporter` 支持单/多级表头；数字、布尔、日期、时间使用原生单元格 | 字符串和枚举文本语义不变；超过 Excel 15 位安全精度的数值按文本保护 |
| 固定列兼容 | `idx` 导出仍使用原固定列；标题字段自动避开固定列 | 不再允许两个字段占用同一列 |
| 导入碰撞 | 注解级重复 idx/跨字段标题别名立即失败；运行时 idx/title 同列返回批错误 | 正常 title/alias/idx 映射及原错误码不变 |
| 失败文件覆盖 | “失败原因”定位到整个 Sheet 的最右已用列之后 | `FAILED_ONLY`、逐行多原因拼接与原工作簿保留语义不变 |
| 模板配置 | 默认 POI 导出对未定义数据锚点的 `templateKey/templateLocation` 明确失败 | 不再静默生成与配置不符的文件；带样式模板继续由自定义 `ExcelAdapter` 扩展点承担 |
| 单体实现 | 字段绑定和导出职责从 887 行 Adapter 中拆出 | `PoiExcelAdapter` 公共构造方法和 `ExcelAdapter` 接口不变 |

## 5. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 数据/清理 | 执行入口 | 状态 |
|---|---|---|---|---|---|---|
| TC-EXCEL-001 | P0 | 单元 | title/alias/idx、字典、Converter、内置类型及错误定位 | 内存工作簿自动关闭 | `PoiExcelAdapterTest` | AUTOMATED |
| TC-EXCEL-002 | P0 | 单元 | 公式、日期、金额、合并单元格、空行和模板原样下载 | 测试 classpath 文件 finally 删除 | `PoiExcelAdapterTest` | AUTOMATED |
| TC-EXCEL-003 | P0 | 单元 | 失败原因位于全部原始数据列之后并保留逐行多原因 | 内存工作簿自动关闭 | `PoiExcelAdapterTest` | AUTOMATED |
| TC-EXCEL-004 | P0 | 单元 | 重复 idx、标题/别名和运行时同列映射不能静默覆盖 | 无持久数据 | `PoiExcelAdapterTest` | AUTOMATED |
| TC-EXCEL-005 | P0 | 单元 | include/exclude、多级表头、原生类型、固定列和精度保护 | 内存响应/工作簿自动关闭 | `PoiExcelAdapterTest` | AUTOMATED |
| TC-EXCEL-006 | P1 | 单元 | 未知字段及默认实现不支持的导出模板配置明确失败 | 无持久数据 | `PoiExcelAdapterTest` | AUTOMATED |
| TC-EXCEL-007 | P0 | 入口流程 | 真实 Tomcat 接收 multipart `.xlsx` 并由 `@RequestExcel` 解析 | 随机端口、Spring context 自动关闭 | `PoiExcelMvcIntegrationTest` | AUTOMATED |
| TC-EXCEL-008 | P0 | 入口流程 | 真实 HTTP 下载响应可由 POI 回读，字段、表头与数字类型正确 | 同上 | `PoiExcelMvcIntegrationTest` | AUTOMATED |

## 6. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | Excel 模块测试 | 12/12，fail/error/skip 0 | PASS |
| 缺陷红灯 | 新增测试运行于旧实现 | 4 个稳定失败，原 HTTP 导入行为通过 | DEFECT CONFIRMED |
| 治理后回归 | Excel 模块定向套件 | 20/20，fail/error/skip 0 | PASS |
| 入口流程 | `PoiExcelMvcIntegrationTest`，标签 `flow` + `infra-excel` | 随机 Tomcat 端口真实 HTTP 导入/导出 2/2 | PASS |
| 当前源码契约 | Persistence API/Starter/Web 与 Excel 四项目同 reactor 编译并执行 Excel 套件 | 20/20 通过 | PASS |
| 直接静态 | Checkstyle、SpotBugs | 0/0 | PASS |
| 正式架构 | Excel + `mango-architecture-verification` partial reactor | dependency、ArchUnit、PMD 7、blocking、聚合静态和工具失败均为 0 | PASS |
| 测试质量 | `test-quality-check`、Mockito changed-only audit | 2 个测试资产 PASS；block=0、warn=0 | PASS |

## 7. Issue #522 防回归

最终验证把当前 Persistence Web Excel 契约与当前 Excel Starter 放在同一 Maven reactor，不以公共 Maven 缓存中的旧 SNAPSHOT 代替生产者。仓内没有其它模块直接声明该 Starter，因此真实随机端口测试应用作为当前实现的产品入口宿主，不伪造不存在的业务消费者。

## 8. 数据与未验证项

| 项目 | 结论 |
|---|---|
| 数据库/Flyway/init/demo | N/A；所有工作簿只在请求、响应或调用方文件存储边界内存在 |
| 浏览器 UI | N/A；公共产品边界是 multipart HTTP 与 `.xlsx` 下载内容 |
| 导出模板 | 默认 POI Adapter 明确不支持无数据锚点的模板配置；使用该能力的宿主必须提供自定义 `ExcelAdapter` |
| 全仓测试 | 未执行；按要求只验证 Excel、当前 Persistence Web 契约和真实服务入口 |

## 9. 风险分级

- 需求影响：L2。错误导出会导致字段泄露、金额类型错误或长编号精度损失，失败文件覆盖原值会妨碍业务纠错，但不涉及平台持久数据。
- 方案风险：L2。修改共享 Starter 的导出和映射校验；公开接口、注解和正常导入语义保持不变，可单提交回退。
- 最终风险：L2。由旧实现红灯、同一回归集、真实 HTTP 上传下载、当前源码契约和架构门禁共同覆盖。
