# Mango Infra Excel Starter

## 1. 概览

`mango-infra-excel-starter` 基于 Apache POI 提供 Mango 官方 Excel 适配器，可直接配合 `@ExcelImport`、`@RequestExcel` 和 `BaseCrudController` 导入 `.xlsx` 工作簿。

## 2. 功能清单

| 能力 | 入口 |
|------|------|
| 按中文标题、别名或固定零基列号读取字段 | `@ExcelColumn` |
| 字典、自定义 Converter 和内置类型转换 | `ExcelDictionaryProvider`、`ExcelColumnConverter` |
| Sheet、说明行、空行、公式、日期、金额、合并单元格 | `@ExcelImport`、`@RequestExcel` |
| 生成失败工作簿并追加逐行失败原因 | `ExcelAdapter.createFailureWorkbook` |
| 原样下载 classpath 模板或生成空模板 | `templateLocation`、`/import-template` |
| 按字段筛选并导出原生数字、布尔、日期和时间单元格 | `@ExcelExport.include`、`exclude` |
| 生成单级或多级自定义表头 | `ExcelHeadGenerator` |

## 3. 能力边界

- 首期只读取和写出 `.xlsx`，不支持旧 `.xls`。
- Starter 负责工作簿协议、字段转换和失败文件内容，不实现业务名称到业务编码、跨表关联和去重规则。
- Jakarta Validation、业务批量校验、事务编排和 Mango File 保存由 Persistence Web、业务 Service 与平台桥接协作完成。
- POI 使用内存工作簿模型，上传大小应由宿主应用的 Servlet multipart 限制控制。
- 默认导出实现不猜测模板的数据起始行。配置导出 `templateKey` 或 `templateLocation` 时会明确失败；需要带样式导出模板的宿主必须提供自定义 `ExcelAdapter`，不会静默忽略配置后生成错误文件。

## 4. 模块入口

自动配置入口为 `ExcelAutoConfiguration`。未声明自定义 `ExcelAdapter` 时注册 `PoiExcelAdapter`；业务自定义 Adapter 优先。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.excel</groupId>
    <artifactId>mango-infra-excel-starter</artifactId>
</dependency>
```

DTO 示例：

```java
public class TenderImportRow {
    @ExcelLine
    private Long line;

    @ExcelColumn(title = "状态", required = true,
            dictType = "tender_status", converter = TenderStatusConverter.class)
    private String status;

    @ExcelColumn(idx = 2)
    private BigDecimal amount;
}
```

`title` 与 `idx` 必须二选一。`converter` 与 `dictType` 同时存在时只执行 Converter，`dictType` 作为字段元数据传入 Converter。

同一个 DTO 内重复的固定列号、跨字段重复的标题/别名会在读取前失败；固定列号和标题在具体工作簿中解析到同一列时返回 `DUPLICATE_COLUMN_MAPPING`，不会让后一个字段静默覆盖前一个字段。

## 6. 配置说明

本 Starter 没有独立配置前缀。导入行为由 `@ExcelImport` 或 `@RequestExcel` 声明：

| 属性 | 默认值 | 含义 |
|------|--------|------|
| `sheetName` | 空 | 非空时按 Sheet 名选择 |
| `sheetIndex` | `0` | Sheet 名为空时使用的零基序号 |
| `headRowNumber` | `1` | 数据开始前的总行数；第一行固定为 title |
| `ignoreEmptyRow` | `true` | 是否跳过映射列均为空的行 |
| `unknownColumnPolicy` | `IGNORE` | 未声明标题忽略或报错 |
| `templateLocation` | 空 | classpath 原始 `.xlsx` 模板 |
| `failureRowPolicy` | `FAILED_ONLY` | 失败文件默认只保留失败数据行 |

导出配置由 `@ExcelExport` 形成 `ExcelExportContext`：

| 属性 | 默认值 | 默认 POI 实现 |
|------|--------|----------------|
| `fileName` | `export.xlsx` | RFC 5987 UTF-8 下载文件名 |
| `sheetName` | `sheet1` | 输出 Sheet 名 |
| `include` | 空 | 非空时只保留列出的 Java 字段名；未知字段立即失败 |
| `exclude` | 空 | 从导出字段中排除 Java 字段名；未知字段立即失败 |
| `headGenerator` | 接口类型 | 自定义 Bean 或无参实现可生成多级表头，列数必须与筛选后字段一致 |
| `templateKey` / `templateLocation` | 空 | 默认 POI 实现明确拒绝；带样式模板由自定义 `ExcelAdapter` 承担 |

普通数字、布尔、日期和时间保持 Excel 原生单元格类型。超过 Excel 15 位安全精度的整数或十进制数按文本写出，避免下载后被截断或改写。

## 7. API 与扩展

- `ExcelColumnConverter<T>`：字段级自定义转换，可读取原始文本、计算值、字段元数据、行列号和导入上下文。
- `ExcelDictionaryProvider`：`dictType` 对应的 label 到 value 解析；`mango-system-starter` 提供默认桥接。
- `ExcelFailureFileStore`：失败工作簿保存；`mango-file-starter` 提供 PRIVATE 文件桥接并返回 `fileId`。
- `ExcelAdapter`：保留自定义实现入口，默认新增结构化读取和失败工作簿方法。
- `ExcelHeadGenerator`：按筛选后的字段顺序返回各列的多级表头；列数不一致会直接拒绝导出。

## 8. 数据与初始化

无数据库 migration、Runner 或初始化数据。失败文件记录由 Mango File 使用当前租户上下文保存，字典值由 Mango System 查询现有字典数据。

## 9. 管理入口

本模块不创建菜单、按钮或独立管理页面。失败文件下载继续使用 Mango File 的权限与租户隔离入口；字典维护继续使用 Mango System 现有入口。

## 10. 快速开始

1. 添加 Excel Starter 依赖。
2. 在导入 DTO 字段声明 `@ExcelColumn(title = "中文标题")` 或 `@ExcelColumn(idx = 0)`。
3. 普通 Controller 使用 `@RequestExcel List<Row>`，或让 Service 实现 `ImportableService<Row>` 后使用 `BaseCrudController`。
4. 第一行 title、第二行说明、第三行数据时配置 `headRowNumber = 2`。
5. 跨字段、数据库关联和重复校验放入 `validateImportRows`，入库逻辑放入 `importRows`。

## 11. 问题排查

- 提示没有 Adapter：确认依赖的是 `mango-infra-excel-starter`，且自动配置没有被排除。
- 缺少标题：确认 title 规范化后精确匹配；title 模式不会回退到 idx。
- 字典无法解析：确认已装配唯一 `ExcelDictionaryProvider`，当前租户存在对应 `dictType` 和 label。
- Converter 未生效：确认注解配置了 Converter，Bean 唯一或类具有无参构造。
- 没有 `failureFileId`：确认装配 `mango-file-starter`；结构化错误不依赖文件服务，仍会正常返回。
- 导出字段缺失：`include`、`exclude` 使用 Java 字段名而不是中文标题，拼写错误会明确失败。
- 导出模板配置失败：默认 POI Adapter 不推断模板数据锚点；带样式模板必须切换为业务自定义 `ExcelAdapter`。

## 12. 验证基线

```bash
mvn -f mango/pom.xml -pl :mango-infra-excel-starter test -DskipTests=false
```

基线覆盖注解映射、字典和 Converter、公式/日期/金额/合并单元格、失败工作簿、字段碰撞、字段筛选、多级表头、原生类型与精度保护。`PoiExcelMvcIntegrationTest` 使用随机端口真实 Tomcat，通过 HTTP multipart 导入并下载、回读 `.xlsx` 导出文件；核心入口不使用 Mockito 替代。

## 13. 相关文档

- [Persistence Web Excel 接入](../mango-infra-persistence/README.md#741-默认-excel-导入)
- [Issue #431 详细设计](../../../mango-docs/designs/2026-07-11-issue-431-excel-import-design.md)
- [后端测试规范](../../../mango-pmo/rules/backend/08-test.md)

## 14. 补充资料

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
