# Issue #431 Excel 导入能力详细设计

## 1. 设计目标与范围

本设计落实 GitHub Issue #431，为 Mango 提供可直接装配的 Excel 导入实现，并保持现有 `ExcelAdapter`、`@ExcelImport`、`@RequestExcel`、`ImportableService` 公共入口兼容。

本次覆盖：

- 官方 Excel Starter 与默认 `ExcelAdapter`。
- 按规范化标题或固定列序号映射字段。
- 字典 `label -> value` 转换、自定义 Converter 和内置类型转换。
- Workbook、Sheet、表头、原始单元格文本和真实行号处理。
- 工作簿结构、转换、Jakarta Validation、业务批量规则和入库异常校验。
- `PARTIAL_SUCCESS`、`ALL_SUCCESS` 两种事务语义。
- 失败工作簿生成、通过 Mango File 保存并返回 `fileId`。
- classpath 原始模板下载及注解生成模板。
- 单元测试、真实 HTTP 入口测试、真实 xlsx 夹具、能力文档和测试基线。

本次不覆盖：

- 业务项目特有的渠道、银行、支行名称到业务 code 的规则；业务通过 Converter 或 `ImportableService` 实现。
- 新增字段级 Validator 注解；字段约束使用 Jakarta Validation，跨行和数据库规则使用业务 Service。
- 绕过 Mango File 返回或持久化临时下载 URL。
- 旧 `.xls` 格式；首期只接受 OOXML `.xlsx`。
- 前端导入页面和交互改造。

## 2. 设计输入与确认

| 输入 | 来源 | 采用结论 |
|---|---|---|
| 完整能力和验收标准 | GitHub Issue #431 | 全部纳入范围 |
| 字典与 Converter | 用户确认，2026-07-11 | 二者可配置；Converter 优先，`dictType` 作为元数据传入 Converter |
| 失败工作簿 | 用户确认，2026-07-11 | 通过 Mango File 保存，结果返回 `fileId` |
| 校验边界 | Issue #431 与用户确认，2026-07-11 | Jakarta Validation + Service 批量业务校验，不新增 Validator 注解 |
| 字段映射 | 用户确认，2026-07-11 | `title` 或零基 `idx` 二选一，禁止 title 失败后按 idx 兜底 |

## 3. 影响模块与依赖方向

| 模块 | 改动 | 依赖方向 |
|---|---|---|
| `mango-infra-persistence-web-starter` | 扩展兼容契约、注解、上下文、结果和 Controller 编排 | 不新增对平台模块的依赖 |
| `mango-infra-excel-starter` | 新模块；基于 Apache POI 提供默认 Adapter、解析、转换和失败工作簿生成 | Excel Starter -> Persistence Web Starter、POI |
| `mango-system-starter` | 提供 `ExcelDictionaryProvider` 桥接 Bean | System Starter -> Excel 契约；Excel 不依赖 System |
| `mango-file-starter` | 提供 `ExcelFailureFileStore` 桥接 Bean | File Starter -> Excel 契约；Excel 不依赖 File |
| Maven parent/聚合 POM | 统一 POI 版本并聚合新模块 | 构建层依赖管理 |
| Persistence README/能力地图 | 更新依赖、注解、转换、校验、失败文件、模板与排障说明 | 文档入口 |

平台桥接 Bean 使用条件装配。没有字典 Provider 时，普通字段仍可导入；字段声明 `dictType` 时返回明确配置错误。没有失败文件 Store 时，导入仍返回结构化错误，但失败工作簿状态标记为不可生成，不伪造 `fileId`。

## 4. 公共契约

### 4.1 字段注解

```java
@ExcelColumn(
        title = "状态",
        idx = -1,
        required = true,
        aliases = {"业务状态"},
        dictType = "tender_status",
        converter = ExcelColumnConverter.None.class)
private String status;
```

规则：

- `title` 与 `idx` 必须且只能配置一个；`idx` 为从 `0` 开始的物理列序号。
- `title` 和 `aliases` 在 trim、全半角空白归一化、连续空白折叠后精确匹配，不做模糊匹配。
- title 模式下缺失必填标题、重复标题直接报告结构错误，不按 `idx` 兜底。
- idx 模式不读取标题语义，列越界按字段错误处理。
- 未知列由 `UnknownColumnPolicy` 控制，默认 `IGNORE`，可配置 `ERROR`。
- `@ExcelLine` 只注入真实 Excel 行号，不参与列映射。

### 4.2 Converter

```java
public interface ExcelColumnConverter<T> {
    T convert(ExcelCellValue value, ExcelColumnMetadata metadata,
              ExcelImportContext context);
}
```

`ExcelCellValue` 至少包含原始展示文本、公式文本与计算值、单元格类型、真实行号和零基列号。`ExcelColumnMetadata` 至少包含字段、title、别名、`dictType`、目标 Java 类型和注解。

转换顺序固定为：

1. 配置了自定义 `converter`：只执行自定义 Converter；`dictType` 作为元数据提供。
2. 未配置 Converter 且配置 `dictType`：调用 `ExcelDictionaryProvider` 完成 `label -> value`。
3. 两者均未配置：执行内置字符串、数字、布尔、枚举、日期和时间类型转换。

Converter 由 Spring 容器优先提供；未注册为 Bean 时允许无参构造。构造失败或存在多个不可判定 Bean 时返回明确配置错误。

### 4.3 Workbook 配置

`@ExcelImport` 与 `@RequestExcel` 增加兼容默认值：

- `sheetName`：非空时按名称选择。
- `sheetIndex`：`sheetName` 为空时按零基序号选择，默认 `0`。
- `headRowNumber`：表头占用的总行数；最后一行是 title，之后第一行为数据。
- `ignoreEmptyRow`：默认 `true`。
- `unknownColumnPolicy`：默认 `IGNORE`。
- `templateLocation`：可选 classpath 原始模板。
- `failureRowPolicy`：默认失败文件只保留失败数据行。

第一行 title、第二行说明、第三行数据的模板使用 `headRowNumber = 2`。真实 Excel 行号以一开始计数，因此第一条数据行的 `@ExcelLine` 为 `3`。

### 4.4 错误与结果

保留现有三参数 `ImportError(int line, String field, String message)` 构造方式和 `line()/field()/message()` 访问方式，扩展错误编码、title、原始值和批次级标识。现有消费者无需修改即可继续读取旧字段。

`ImportResult` 保留 `total/success/failed/errors`，增加：

- `failureFileId`：失败工作簿的 Mango File ID。
- `batchErrors`：无法归属到单行的结构或批次错误。
- `status`：`SUCCESS`、`PARTIAL_SUCCESS`、`FAILED`。

同一行多个错误分别保存在结构化列表中；失败工作簿的“失败原因”列按稳定顺序使用中文分号连接。

## 5. 导入数据流

1. MVC 参数解析器读取 Multipart 文件和注解配置。
2. 默认 Adapter 打开 `.xlsx`，选择 Sheet，解析 title 或 idx 映射。
3. 每个单元格先提取原始展示文本，再执行 Converter、字典或内置转换。
4. 转换成功的 DTO 注入真实 Excel 行号。
5. 编排器执行 Jakarta Validation，随后调用 `validateImportRows` 做跨字段、跨行和数据库校验。
6. 按模式筛选可入库行并调用业务 Service。
7. 聚合结构、转换、字段、业务和入库错误。
8. 存在行级错误时，从原始工作簿复制结构，只保留失败数据行，在末列追加“失败原因”。
9. `ExcelFailureFileStore` 将工作簿交给 Mango File 保存，结果写入 `failureFileId`。

读取规则：

- 使用 POI `DataFormatter` 保存用户看到的原始文本，避免文本数字前导零丢失。
- 公式默认读取计算结果，同时保留公式文本；没有可用缓存结果时报告单元格错误。
- 日期按 Excel 日期单元格和目标 Java 类型转换；纯文本日期必须由内置格式或自定义 Converter 明确处理。
- 金额使用 `BigDecimal`，禁止经过 `double` 再构造。
- 合并单元格只有左上角是值来源；数据区其他合并位置读取左上角展示值，并保留当前真实行号。
- 空行判定基于全部已映射列的格式化文本均为空。

## 6. 校验与事务

校验按以下阶段执行，前一阶段产生的错误不会阻止其他可安全读取的行继续收集错误：

1. Workbook/Sheet/表头结构校验。
2. 单元格读取和转换校验。
3. Jakarta Validation 字段约束。
4. `ImportableService.validateImportRows` 批量业务校验。
5. `ImportableService.importRows` 入库结果和异常归一化。

事务语义：

- `PARTIAL_SUCCESS`：只向 Service 传入无错误行；成功行可以提交，失败行进入结果和失败工作簿。
- `ALL_SUCCESS`：结构、转换、字段或业务校验存在任一错误时不调用入库；调用入库后发生异常时整体回滚。
- 默认编排器不假装控制业务 Service 内部事务。ALL_SUCCESS 入库通过独立事务边界执行，并要求业务 Service 的导入实现参与该事务。
- 批量异常能由业务返回行号时记录行级错误；无法定位时记录批次错误，ALL_SUCCESS 回滚，PARTIAL_SUCCESS 不猜测成功行。

现有 `ImportableService` 方法保持可用；新增可选的上下文感知导入入口使用 default method 适配旧实现，避免强制业务项目立即迁移。

## 7. 失败工作簿与模板

失败工作簿：

- 从上传的原始 workbook 派生，保留原列、原值、样式、说明行、数据验证、字典 Sheet、公式、列宽和冻结窗格。
- 默认删除成功数据行，仅保留失败行；按从后向前的顺序删除，避免行号漂移。
- 在数据 Sheet 最后一列追加“失败原因”，同一行多个原因用中文分号连接。
- 文件名使用原文件名加 `-failed` 和 `.xlsx`，不含租户、账号或敏感业务数据。
- 通过 Mango File 的内部保存契约写入当前租户文件记录，只返回 `fileId`。

模板下载：

- 配置 `templateLocation` 时原样复制 classpath `.xlsx`，不重新生成 workbook。
- 未配置时根据 `@ExcelColumn` 生成空模板；title 模式生成标题，idx 模式按 idx 放置字段标题或字段名。
- 原始模板不存在、不是 classpath 资源或不是 `.xlsx` 时返回明确配置错误。

## 8. 自动配置与兼容

- `mango-infra-excel-starter` 在没有用户自定义 `ExcelAdapter` 时注册 POI 默认实现。
- 用户自定义 `ExcelAdapter` 继续优先，现有扩展项目行为不变。
- 业务只需增加 Excel Starter 依赖即可注册 `@RequestExcel` 参数解析器。
- 普通 Controller 与 `BaseCrudController` 使用同一 Adapter 和导入编排能力。
- 既有注解默认值保持原行为；新增属性都有兼容默认值。
- POI 版本由 Mango Parent 统一管理，业务项目不直接锁版本。

## 9. 验收与测试映射

| 用例 | 类型 | 验证内容 |
|---|---|---|
| TC-431-01 | UNIT | title 规范化、别名、乱序列映射结果一致 |
| TC-431-02 | UNIT | title/idx 二选一、重复标题、缺失必填标题、未知列策略 |
| TC-431-03 | UNIT | Converter > dictType > 内置转换优先级和上下文完整性 |
| TC-431-04 | UNIT | 前导零、公式、日期、BigDecimal、空行、合并单元格和真实行号 |
| TC-431-05 | UNIT | Jakarta 与业务批量校验聚合一行三个错误 |
| TC-431-06 | UNIT | 失败工作簿只含失败行并保留原 workbook 结构和三条原因 |
| TC-431-07 | UNIT | classpath 模板下载保持说明行、字典 Sheet、验证、样式、列宽、冻结窗格 |
| TC-431-08 | API | 普通 Controller 通过 multipart `@RequestExcel` 真实导入乱序 xlsx |
| TC-431-09 | API | BaseCrudController 的 PARTIAL_SUCCESS 合法行入库、错误行不入库 |
| TC-431-10 | API | ALL_SUCCESS 任一错误不入库，入库异常整体回滚 |
| TC-431-11 | API | 失败文件经 Mango File 保存，当前租户得到可下载 `fileId` |
| TC-431-12 | API | 仅添加 Excel Starter 即自动注册默认 Adapter 和参数解析器 |

真实 xlsx fixture 放在对应测试模块 `src/test/resources`。API 测试从 HTTP multipart 入口进入，不直接调用 Service 冒充真实入口。正式结果提升到 `mango-docs/evidence/test-baseline/excel-import/{unit,api}/latest`。

## 10. 风险与取舍

- 现有 Excel 契约位于 Persistence Web Starter。为保持源码和二进制兼容，本次不搬包；新实现独立成 Starter，后续若治理 API 模块需单独发布迁移方案。
- System/File 桥接使改动跨平台模块，但避免 Infra 反向依赖 Platform。桥接模块必须分别验证缺失和存在时的条件装配。
- 原 workbook 派生失败文件的内存成本高于流式读取。首期设置可配置文件大小与行数上限，超限时拒绝并返回明确错误，不以 OOM 风险换取静默降级。
- PARTIAL_SUCCESS 下未知批次入库异常不能可靠推断哪些行成功，因此不伪造逐行成功结果；业务需要可靠部分提交时必须返回明确行级结果。

## 11. 完成标准

- Issue #431 的九项验收标准全部映射到自动化用例或明确文档验证。
- 默认 Adapter、title/idx、字典、自定义 Converter、校验、事务、失败文件和模板下载均有真实实现。
- 公共 API 兼容检查通过，现有自定义 Adapter 可继续装配。
- 受影响模块 `mvn verify` 和 Mango 质量门禁通过。
- Persistence README、能力地图、测试基线和业务接入示例同步更新。

