# 标准交付记录

## 1. 元数据

- 任务 ID：issue-1171-ooxml-schemas
- 交付模式：STANDARD
- 需求影响：L2 - 影响所有用 `mango-file-preview-starter` 且基于 Apache POI 5 解析 Excel/Word 的业务项目；问题不阻断启动，但必须人工绕行。
- 方案风险：L2 - 改动发布到业务仓消费的 Maven 依赖图，需要防止引擎自身 DOCX 相关能力缺类。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-1169`，分支 `fix/issue-1169-1171-dialog-center-poi-conflict`（`M01=CREATE`，`mango workspace init` slot 17）。两个 Issue 按用户要求在同一个 worktree 修复并合并在一个 PR 中。

## 2. 目标与范围

- 目标：`mango-file-preview-engine` 不再向消费方传递 `org.apache.poi:ooxml-schemas`，消除与 POI 5.2.5 的 `NoSuchFieldError` 冲突。
- 成功条件：引擎与 starter 的依赖树不含 `ooxml-schemas`；引擎编译、`mvn verify` 通过；POI 5 解析在不再需要业务侧排除后可用。
- 处理范围：`mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml`、模块 README、文件上传业务指南。
- 不处理范围：`mango-infra-fileproc` 的 POI 使用、POI 版本升级、starter 版本号和发布执行。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 业务项目 Maven 构建 | 依赖 `mango-file-preview-starter` | 依赖图不出现 `org.apache.poi:ooxml-schemas` | 依赖树仍出现该 jar | `dependency:tree -Dincludes=org.apache.poi:ooxml-schemas` 无输出 |
| SR-002 | 预览引擎自身 | 执行模块构建与测试 | 引擎可编译，54 个既有测试通过 | 编译失败或测试失败 | `mvn -pl :mango-file-preview-engine verify` BUILD SUCCESS |
| SR-003 | 使用 POI 5 的业务代码 | 同一 classpath 中 POI 5.2.5 + 旧 `ooxml-schemas:1.1` 且旧 jar 优先 | 旧依赖移除后不再出现 `NoSuchFieldError` | 仍抛 `NoSuchFieldError ... DocumentFactory Factory` | 同一 smoke 程序在修复后 classpath 输出 `poi-version=5.2.5` |
| SR-004 | 预览引擎 DOCX 能力 | 引擎运行期使用 `fr.opensagres.xdocreport.core.io.IOUtils` | 保留提供 `IOUtils` 的 xdocreport 依赖 | 运行期 `NoClassDefFoundError` | 修复后 classpath 仍含 `fr.opensagres.xdocreport.core` |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001、SR-003、SR-004 | 删除未被引擎代码引用的 `org.apache.poi.xwpf.converter.core` 和 `.xhtml`，保留提供 `IOUtils` 的 `fr.opensagres.xdocreport.document`；不新增依赖、不改 BOM 版本 | `mango-file-preview-engine/pom.xml` | 恢复这两个依赖声明 |
| TD-002 | SR-001 | 在 POM 内写注释说明禁止重新引入 `org.apache.poi.xwpf.converter.*` 的原因 | `mango-file-preview-engine/pom.xml` | 随 TD-001 回滚 |
| TD-003 | SR-001、SR-004 | 更新模块 README 的升级与兼容说明、业务上传指南的常见失败条目 | `mango-file-preview/README.md`、`mango-docs/guides/business-integration/file-upload-form.md` | 随代码回滚文档 |

### 4.1 决策复核

- 事实与用户视角：Issue #1171 由业务仓 `mango-baohan-risk` 在台账导入时触发，业务侧已在 `backend/app/pom.xml` 排除 `ooxml-schemas` 绕行。
- 技术与风险视角：`dependency:tree` 证明该 jar 只通过 `org.apache.poi.xwpf.converter.core:1.0.6` 传递；`grep` 证明引擎代码只使用 `fr.opensagres.xdocreport.core.io.IOUtils`，未引用任何 `org.apache.poi.xwpf.converter` 类型。保留 lite schema 不可行：xdocreport 1.0.6 引用的 `FontsDocument`、`SettingsDocument`、`STOnOff` 等 14 个类不在 `poi-ooxml-lite:5.2.5` 中，因此不能仅排除旧 jar 而依赖 lite。
- 验证与交付视角：用 POI 5.2.5 加旧 `ooxml-schemas:1.1` 的 classpath smoke 复现 `NoSuchFieldError`，修复后同一程序通过；`mvn verify` 与 core/starter 测试全部通过。
- 评审选择：`AGENT_ONLY`（用户未要求外部同行评审；结论保留再次发起外部评审的入口）。

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-001、TD-002 | 1 | `mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml` | 两个转换器依赖删除，保留 `fr.opensagres.xdocreport.document` |
| TASK-002 | TD-003 | 2 | `mango/mango-platform/mango-file-preview/README.md` | 增加 Issue #1171 升级与兼容说明 |
| TASK-003 | TD-003 | 3 | `mango-docs/guides/business-integration/file-upload-form.md` | 常见失败表新增 POI 5 冲突条目 |
| TASK-004 | TD-001 | 4 | 定向验证 | 依赖树、`mvn verify`、core/starter 测试和 classpath smoke 全部执行 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M09 静态验证 | `mvn -o -pl :mango-file-preview-engine,:mango-file-preview-starter dependency:tree -Dincludes=org.apache.poi:ooxml-schemas` | PASS | 输出为空；修复前同命令输出 `ooxml-schemas:jar:1.1:compile` |
| SR-002 | M09/M11 构建与集成测试 | `mvn -o -pl :mango-file-preview-engine verify`；`mvn -o -pl :mango-file-preview-core,:mango-file-preview-starter test` | PASS | 引擎 54 tests、core 19 tests、starter 16 tests，均 BUILD SUCCESS |
| SR-003 | M11 classpath 复现 | `/tmp/poi1171/PoiSmoke`：修复前 `ooxml-schemas:1.1` 优先时抛 `NoSuchFieldError ... CTWorkbook ... DocumentFactory Factory`；修复后同程序输出 `poi-version=5.2.5` | PASS | 两次 java 运行输出记录在本文档与 PR 描述 |
| SR-004 | M09 依赖回读 | `dependency:build-classpath` 后 grep `xdocreport` | PASS | `fr.opensagres.xdocreport.core-1.0.6.jar` 仍在 classpath |

### 6.1 真实场景验证

`mango dev start` 启动本 worktree 的单体应用（后端 `18017`、数据库 `mango_dev_mango_issue_1169_017`），单体依赖 `mango-file-preview-starter`，是本次改动的真实消费方：

| 检查 | 方法 | 结果 |
|---|---|---|
| 运行态 classpath | 读取真实 JVM 进程的 `-cp`（536 项）并检索 `ooxml-schemas` | 0 处；包含 `poi-5.2.5`、`poi-ooxml-5.2.5`、`poi-ooxml-lite-5.2.5`、`fr.opensagres.xdocreport.core-1.0.6` |
| 业务解析链路 | 用该真实 classpath 运行 `AppPoiBusinessPath`：POI 5 写入带单元格样式的 xlsx，再用 `WorkbookFactory` 读回 | `poi=5.2.5 rows=2 a2=E2E_1171_001 b2=1234.56` |
| 引擎字节码 | `grep -rl "xwpf/converter" mango-file-preview-engine/target/classes` | 无引用，确认删除的依赖从未被引擎代码使用 |

## 7. 例外与剩余风险

- 未在业务仓 `mango-baohan-risk` 执行真实台账导入回归（`M16` 未启用）；本仓已用真实单体应用的运行时 classpath 执行同样的 `WorkbookFactory` 读写，业务侧升级后需删除 POM 排除并复跑导入。
- 已确认的后端质量门禁只覆盖直接修改的 Maven 模块与 file-preview reactor；`mango-infra-fileproc` 等其它使用 POI 的模块未纳入本次验证。
- 未执行发布动作，版本号与 `CHANGELOG.md` 由后续发布流程负责。
