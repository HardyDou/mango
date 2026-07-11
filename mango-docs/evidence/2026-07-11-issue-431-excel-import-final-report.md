# Issue #431 Excel 导入能力交付报告

日期：2026-07-11  
关联 Issue：<https://github.com/HardyDou/mango/issues/431>  
分支：`issue-431-excel-import`

## 1. 交付结论

Issue #431 的默认 Excel 导入链路已完成。业务模块引入 `mango-infra-excel-starter` 后可使用默认 POI Adapter，也可继续提供自定义 `ExcelAdapter` 覆盖默认实现。

本次交付支持：

- `@ExcelColumn(title = "状态")` 按标题映射，允许列乱序；`idx` 使用零基列序号，且与 `title` 二选一。
- `converter` 与 `dictType` 同时配置时由自定义 Converter 优先；字典 label 通过 System 字典服务转换为 value。
- 内置类型转换、结构校验、Jakarta Validation、业务批量校验和逐行多原因聚合。
- `PARTIAL_SUCCESS` 只提交合法行；`ALL_SUCCESS` 任一错误不提交，运行时异常整体回滚。
- 从原工作簿生成仅含失败行的工作簿，通过 Mango File 保存并返回 `failureFileId`。
- classpath 原始模板逐字节下载，以及无模板时的注解标题模板生成。

## 2. 验收结果

| 验收项 | 结果 | 证据 |
|---|---|---|
| 乱序中文标题与零基 idx 映射 | PASS | `PoiExcelAdapterTest` |
| Converter 优先于 dictType | PASS | 单元测试与定向 Mutation |
| 字典 label 转换及类型适配 | PASS | `PoiExcelAdapterTest` |
| Jakarta 与业务错误聚合 | PASS | `BaseCrudControllerTest` |
| PARTIAL/ALL 事务语义 | PASS | MockMvc + H2 事务集成测试 |
| 失败工作簿与 Mango File ID | PASS | 单元及 Controller 测试 |
| classpath 模板原样输出 | PASS | 字节级断言 |

正式基线：

- `mango-docs/evidence/test-baseline/excel-import/unit/latest`
- `mango-docs/evidence/test-baseline/excel-import/api/latest`

UNIT 11 项、API/集成 11 项，共 22 项测试通过。4 个关键定向突变种子全部被杀死，Mutation kill rate 为 100%。

## 3. 验证记录

受影响的 48 模块 Maven Reactor 验证通过：

```bash
mvn -f mango/pom.xml \
  -pl mango-infra/mango-infra-excel-starter,mango-infra/mango-infra-persistence/mango-infra-persistence-web-starter,mango-platform/mango-system/mango-system-starter,mango-platform/mango-file/mango-file-starter \
  -am verify
```

同时通过：

- PMO `quality-gate`（43 个任务变更文件，0 issue）。
- `quality-baseline check`。
- 模块 README 审计、README 源事实审计、能力文档检查。
- `git diff --check`。

全仓无基线的 `mango:check -Drule=all` 扫描到 24,486 个存量问题，因此不能作为本任务新增问题结论；正式 PR 使用 `no-new-violations` 债务基线校验本次差异。

## 4. 兼容性与发布

- 既有 Excel 公共接口采用兼容扩展，保留原构造器与访问方法。
- 用户自定义 Adapter、字典 Provider 和失败文件 Store 均可通过条件装配覆盖默认 Bean。
- 不修改数据库结构，不需要 migration 或数据回滚。
- 本次不执行 Maven/NPM 正式制品发布；随代码合并进入后续统一发布流程。
