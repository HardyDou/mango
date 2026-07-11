# Issue #431 Excel 导入能力实施计划

文档状态：实施中  
关联 Issue：<https://github.com/HardyDou/mango/issues/431>  
关联设计：`mango-docs/designs/2026-07-11-issue-431-excel-import-design.md`  
日期：2026-07-11

## 1. 目标

交付 Mango 官方 Excel 导入 Starter，使普通 Controller 和 `BaseCrudController` 能通过真实 multipart 入口完成 title/idx 映射、字典及自定义转换、分层校验、事务模式、失败工作簿和模板下载。

## 2. 实施阶段

### S1 公共契约与兼容模型

- 扩展 `ExcelImportContext`、`@ExcelImport`、`@RequestExcel`。
- 新增 `@ExcelColumn`、`UnknownColumnPolicy`、Converter、字典 Provider、失败文件 Store、单元格与字段元数据。
- 兼容扩展 `ImportError` 和 `ImportResult`。
- 完成契约编译与兼容性单元测试。

完成标准：title/idx、转换优先级、错误详情和失败文件 ID 均能由公共契约表达，既有三参数错误构造与访问方法保持可用。

### S2 默认 POI Adapter

- 新增 `mango-infra-excel-starter` 聚合模块和自动配置。
- 统一 Apache POI 依赖版本。
- 实现 Sheet 选择、标题规范化、idx 映射、原始文本、公式、日期、金额、合并单元格、空行与真实行号。
- 实现 classpath 原始模板复制和注解空模板生成。
- 用真实 `.xlsx` fixture 覆盖原序、乱序、说明行和边界输入。

完成标准：仅增加 Starter 依赖即可得到默认 `ExcelAdapter`，乱序 title 导入结果一致，idx 使用零基列号且不参与 title 回退。

### S3 转换与校验编排

- 实现 Converter Bean/无参实例解析。
- 实现 Converter > dictType > 内置转换顺序。
- 聚合结构、转换、Jakarta Validation 和业务批量错误。
- 统一普通 Controller 与 BaseCrudController 的导入编排。
- 落实 PARTIAL_SUCCESS 与 ALL_SUCCESS 事务语义。

完成标准：单行三个错误完整返回；PARTIAL_SUCCESS 只提交合法行；ALL_SUCCESS 任一错误不提交且运行时异常整体回滚。

### S4 System/File 桥接与失败工作簿

- `mango-system-starter` 提供当前租户字典 Provider。
- `mango-file-starter` 提供失败工作簿 Store。
- 从原工作簿派生只含失败行的文件并追加“失败原因”。
- 返回 Mango File `fileId`，下载继续经过文件权限和租户链路。

完成标准：字典 label 转 value 使用真实字典数据；失败工作簿保留原结构、原值和全部原因；跨租户不可读取失败文件。

### S5 API 验收、Mutation、文档和基线

- 从 HTTP multipart 入口执行普通 Controller 与 BaseCrudController API 测试。
- 对 title/idx 决策、转换优先级、PARTIAL/ALL 模式和失败行筛选执行定向 Mutation。
- 更新 Persistence README、能力地图和业务接入示例。
- 生成并提升 `excel-import` UNIT/API latest 基线。
- 输出正式交付报告、兼容性、发布与升级结论。

完成标准：受影响 Maven 模块 `verify`、Mango 静态门禁、proof-path、Mutation、文档审计和能力文档检查全部通过。

## 3. 验证入口

| 层级 | 入口 |
|---|---|
| UNIT | 新 Excel Starter 和 Persistence Web Starter 的模块测试 |
| API | 真实 Spring MVC multipart 集成测试，不绕过 Controller/参数解析器 |
| Proof path | HTTP multipart -> argument resolver/import orchestrator -> POI adapter -> converter/validator -> service transaction -> file store |
| Mutation | title/idx 互斥与匹配、转换优先级、失败行筛选、事务模式分支 |
| Static | 受影响 reactor `mvn verify` 与 `mvn mango:check -Drule=all` |
| Docs | module README、source facts、capability docs 检查 |

## 4. 风险与回滚

- 公共契约只做兼容扩展；用户自定义 `ExcelAdapter` 继续由条件装配优先。
- 新 Starter 可从业务依赖中移除以回滚默认实现，不影响原扩展接口。
- System/File 只增加条件桥接 Bean，不改变既有字典与文件 API 行为。
- 不修改数据库结构和初始化数据，无 migration 回滚项。
- 若 POI 内存验证超过设计上限，交付前以明确文件大小/行数限制阻断，不提交静默降级实现。

## 5. 不处理范围

- 不实现业务项目特有的名称到业务 code 规则。
- 不新增字段 Validator 注解。
- 不支持 `.xls`。
- 不新增前端导入页面。
- 不执行 Maven/NPM 正式发布；发布作为代码合并后的独立动作。

