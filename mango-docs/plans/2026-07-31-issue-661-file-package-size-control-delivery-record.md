# Issue 661 文件打包大小控制交付记录

## 1. 元数据

- 任务 ID：ISSUE-661-FILE-PACKAGE-SIZE-CONTROL
- Issue：[HardyDou/mango#661](https://github.com/HardyDou/mango/issues/661)
- 交付模式：STANDARD
- 需求影响：L2 - 为公共 File API 增加单 ZIP 大小控制和可观察结果契约，影响本地与远程文件打包调用方，但不改变存量未启用大小控制的行为。
- 方案风险：L2 - 改动覆盖 `mango-file-api`、`mango-file-core`、starter Controller、远程 Feign 契约及压缩协作；无数据库变化，失败后可整体回退新增接口和实现。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-661-file-package-target`，分支 `feat/issue-661-file-package-target`
- 保障措施：M01=CREATE；M08=ENABLE；M09=ENABLE；M10=ENABLE；M12=ENABLE

## 2. 目标与范围

- 目标：文件中心始终生成一个 ZIP，并提供自动与手动两种大小控制模式；无论能否达到目标，均保存 ZIP 并返回实际结果和逐文件压缩摘要。
- 成功条件：自动模式按可压缩文件大小比例分摊目标并动态再分配；手动模式严格使用 entry 目标；`compression=NONE`、Office 和其它不受压缩组件支持的文件不改变；最终结果以 ZIP 实际字节数判断。
- 处理范围：文件 API Command/VO/枚举、核心打包算法、Controller、Feign、文件模块 README、定向单元和 API 契约测试。
- 不处理范围：拆包或分卷、改变 DOC/DOCX/XLS/XLSX/PPT/PPTX/ZIP 内容、数据库变更、发布 Maven 版本、修改 `mango-infra-fileproc` 的格式支持范围。
- 兼容性：保留 `packageFiles(FilePackageCommand): FileRecordVO` 及 `/file/files/package`；存量入口保持既有单包和单文件压缩语义。新增大小控制入口返回结构化结果，不改变现有返回类型。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | Java API、HTTP 或 Feign 调用自动模式 | `sizeControlMode=AUTO`，提供正数 `maxPackageSizeBytes` 和非空 entries | 始终生成一个 ZIP；仅对允许压缩的条目按当前大小比例分配压缩目标，完成后用真实 ZIP 字节数判断 | 无法达到目标不抛出“超限”异常；返回 `targetAchieved=false`、实际大小和原因 | 多个不同大小 PDF/图片按比例获得目标，最终记录和摘要一致 |
| REQ-002 | Java API、HTTP 或 Feign 调用手动模式 | `sizeControlMode=MANUAL`，各 entry 可提供 `targetSizeBytes`，可选 `maxPackageSizeBytes` | 每个 entry 只按自己的目标压缩，不做自动比例分配或补压缩；总目标仅用于结果判断 | 单文件或总 ZIP 未达标仍正常返回；逐条和总体结果明确标记 | 手动目标原样传给压缩能力，总目标不会改变其它 entry |
| REQ-003 | 调用方保护原件 | entry 明确设置 `compression=NONE` | AUTO 和 MANUAL 均不得压缩该 entry | 即使因此无法达到目标也正常返回，并说明该 entry 未参与压缩 | 压缩能力不接收 NONE entry，ZIP 内容保持原样 |
| REQ-004 | 打包含 Office、ZIP 或其它不受支持格式 | 压缩能力 `supports()` 返回 false | 文件原样进入 ZIP，不根据扩展名强制转换或压缩 | 目标无法达到时返回未达标摘要，不丢失或跳过文件 | DOCX、Excel 等内容逐字节保持不变且只出现一次 |
| REQ-005 | 自动模式存在不可达的分配目标 | 某些可压缩条目达到实际最小值，仍有剩余缺口 | 将未完成额度按其余仍可缩小条目的当前大小比例重新分配，直到达到总目标或没有条目继续缩小 | 禁止无限循环；无进一步缩小时保存当前最优 ZIP | 不可缩小候选退出后，其份额被重新分配；终止条件可测试 |
| REQ-006 | 任一大小控制模式完成 | 路径合法、无重复且源文件可读取 | 结果包含文件记录、模式、请求/实际 ZIP 大小、总体是否达标、是否执行压缩、逐 entry 前后大小/目标/达标状态和说明 | 非大小控制类错误仍沿用既有确定异常，例如空输入、非法路径、重复路径、文件不存在 | 本地 API、HTTP Controller 和 Feign 返回模型一致 |
| REQ-007 | 存量调用方 | 未调用新增大小控制入口 | `/package`、`FileApi.packageFiles()` 和现有 `compression`、`perFileTargetSizeBytes`、entry 覆盖规则保持不变 | 不引入返回类型或路径破坏 | 现有打包测试继续通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-002、REQ-006、REQ-007 | 新增独立大小控制 API 和 HTTP 路径，使用专用 Command/Result VO；旧 `packageFiles` 不改签名。模式使用 API 枚举 `AUTO`、`MANUAL`，避免字符串分支散落 | `mango-file-api`、starter、starter-remote | 删除新增接口、Controller 和 Feign 方法；旧入口不受影响 |
| DEC-002 | REQ-001、REQ-005 | AUTO 先生成基线 ZIP；超限量按可压缩候选的当前字节数占比分配。候选压缩结果没有变小时退出本轮并从后续分配中移除；每轮重建 ZIP 并以真实大小决定是否继续 | `mango-file-core` | 回退大小控制服务路径 |
| DEC-003 | REQ-002、REQ-003、REQ-004 | MANUAL 只读取 entry `targetSizeBytes`；entry 的有效压缩档位为 entry 覆盖顶层。有效档位为 NONE 或 `FileCompressApi.supports()` 为 false 时原样保留。可选总 ZIP 目标只填充总体达标字段 | API Command、core 候选解析 | 回退手动大小控制路径 |
| DEC-004 | REQ-001、REQ-005、REQ-006 | 压缩目标按当前候选大小比例分摊，使用向上取整保证每轮至少尝试消除真实超限字节；压缩能力返回未达标或不再缩小时记录实际结果并动态再分配，不承诺不可达目标 | core 分配算法、结果摘要 | 回退分配器及相关 VO |
| DEC-005 | REQ-006 | 总体结果区分“ZIP 总目标是否达成”和“所有 entry 手动目标是否达成”；未提供相应目标时使用可空布尔值表达“不适用”，不把“不适用”伪装为成功 | `FilePackageResultVO`、entry 结果 VO | 删除新增 VO |
| DEC-006 | REQ-001、REQ-006 | 临时处理物写入任务级临时目录并及时关闭/删除，ZIP 通过文件流生成和校验，避免把全部源文件与完整 ZIP 同时常驻内存；现有压缩 SPI 返回单文件 `byte[]` 的限制保留为已知边界 | core 打包实现 | 回退临时文件实现 |
| DEC-007 | REQ-007 | 新入口复用现有租户可见性、路径安全、重复路径、存储保存、purpose/accessLevel/bizType/bizId/bizMeta/directoryId 规则，不新增权限或数据库字段 | core、starter | 删除新增入口即可回退 |

### 4.1 大小分配公式

AUTO 模式某轮真实 ZIP 大小为 `actualZipSize`，目标为 `maxPackageSizeBytes`，超限量为：

```text
requiredSaving = actualZipSize - maxPackageSizeBytes
```

对仍可继续缩小的候选集合，候选 `i` 的本轮分摊量和新目标为：

```text
allocatedSaving(i) = ceil(requiredSaving * currentSize(i) / sum(currentSize))
targetSize(i) = max(1, currentSize(i) - allocatedSaving(i))
```

压缩后重新生成 ZIP。某候选输出不小于其本轮输入，或压缩能力明确报告无法达到更小目标时，将其标记为不可继续缩小；剩余缺口在下一轮只向其余候选分摊。最终只以重新生成后的 ZIP 实际字节数判断 `packageTargetAchieved`。

### 4.2 返回结果

结果模型至少表达：

- 最终 ZIP `FileRecordVO`；
- `sizeControlMode`；
- 可选的 `maxPackageSizeBytes`；
- `actualPackageSizeBytes`；
- 可空的 `packageTargetAchieved`；
- 可空的 `entryTargetsAchieved`；
- `compressionApplied`；
- 每个 entry 的 `fileId`、ZIP 路径、原始大小、输出大小、请求目标、是否支持压缩、是否实际压缩、可空达标状态和说明；
- 总体说明，明确未达标由 NONE、格式不支持或压缩能力极限中的哪些事实造成。

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | DEC-001、DEC-005 | 1 | `mango-file-api` | 模式、Command、总体/entry Result VO 和新 FileApi 方法完成，校验与 Javadoc 明确 |
| IMP-002 | DEC-002、DEC-003、DEC-004、DEC-006、DEC-007 | 2 | `mango-file-core` | 自动比例分配、手动目标、动态再分配、临时文件生命周期和保存链路完成 |
| IMP-003 | DEC-001、DEC-007 | 3 | `mango-file-starter`、`mango-file-starter-remote` | HTTP 与 Feign 契约暴露新入口，权限保持现状 |
| IMP-004 | 全部 | 4 | `mango-file-core`、starter 测试 | 自动、手动、NONE、不支持格式、不可达目标、兼容入口测试完成 |
| IMP-005 | DEC-001、DEC-005、DEC-007 | 5 | `mango-file/README.md` | 两种模式、参数、返回和未达标语义有可消费示例 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-002、REQ-003、REQ-004、REQ-005 | M10 定向单元测试 | `mvn -B -ntp -pl :mango-file-core -am -Dtest=FileServicePackageFilesTest -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true test` | PASS：10/10 | AUTO 比例分摊、触底动态再分配、不可达目标正常返回、MANUAL、NONE/不支持格式均覆盖 |
| REQ-006、REQ-007 | M12 API 契约与兼容测试 | `mvn -B -ntp -pl :mango-file-starter -am -Dtest=FileControllerPackageSizeControlTest,FileControllerAccessModeTest -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true test`；消费者模块 compile | PASS：2/2；`mango-file-preview-core`、`mango-file-preview-app` 编译通过 | Controller、登录权限、Feign/API 消费者兼容 |
| 全部 | M09 模块质量门禁 | 直接模块 `mvn verify -DskipTests`；四模块 `checkstyle:check`；`mango:check -Dgate=no-new-violations`；`mango:architecture -Dmango.architecture.base=origin/main` | PASS：Checkstyle 0 新违规；基线 `newIssueCount=0`；架构 `dependency=0, pmd=0, blocking=0` | PMD 7/ArchUnit/依赖边界和专项基线均通过 |
| 全部 | 差异与测试质量检查 | `git diff --check`；`node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：diff clean；测试质量 8 文件；Mock 审计 block=0、warn=0 | 仓库质量脚本输出 |
| REQ-001、REQ-002、REQ-006 | M08 能力说明检查 | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs`；`node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD` | PASS：README/source facts 无问题；能力说明检查 20 个变更文件通过 | `mango/mango-platform/mango-file/README.md` 已补 AUTO/MANUAL/NONE/未达标语义 |

## 7. 例外与剩余风险

- `FileCompressApi` 当前以单文件 `byte[]` 返回压缩结果。此次通过逐文件处理和临时 ZIP 限制峰值，但单个超大 PDF/图片仍受压缩 SPI 的内存模型约束；本 Issue 不扩展 `mango-infra-fileproc` SPI。
- ZIP DEFLATE、文件名和中央目录会造成实际大小与条目字节和不同。AUTO 模式必须按实际 ZIP 反复校验；无法达到目标时返回当前最优结果，不提供硬上限保证。
- 本任务不发布 Maven 制品；消费项目需等待后续 Mango 版本发布后接入新增契约。
- 完整 `mvn verify` 的既有 `FileServiceConcurrentSaveIntegrationTest` 需要外部 MySQL，当前环境因未提供 `MANGO_DB_USERNAME` 失败；本次新增和相关回归测试均通过，静态门禁使用 `-DskipTests` 并已由 PMD 7/ArchUnit/基线检查补充验证。
