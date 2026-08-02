---
documentId: PLAN-ISSUE-690-WEEKLY-REGRESSION
documentType: implementation-plan
pmoVersion: 1.3.8
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，Issue #690 的 13 个 P0 与 7 个 P1 阻断标准生成项目启动、升级、模块追加、生产制品、空库登录重启、真实 BSQL 消费和可信浏览器验收；solution=L3，修复横跨 CLI、生成模板、pnpm workspace、Maven 架构插件、Bootstrap 生命周期、Resource、认证边界、Persistence/BSQL 和浏览器 E2E，并需要一次性真实 MySQL 消费链；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Development Agent
approver: HardyDou
approvalEvidence: mango-docs/designs/issue-690-weekly-regression/review/PLAN-APPROVAL.md
upstreamDocumentId: TDD-ISSUE-690-WEEKLY-REGRESSION
upstreamDocumentHash: dcb9a0b52c036718cbae6e7d988997a52b405ffc876c631b70c02b0165f272a7
---

# Issue #690 周回归投产阻断修复实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-002、MOD-001、DM-001、FLOW-001、ERR-001、IMP-001 | Spring Boot processMode、full locker 配置及回归测试 | `mango-ui/packages/mango-cli` | 新旧 manifest 解析正确；dev 首参唯一为 runtime；冲突 fail-closed；full context 有 lease locker | TC-001 | 不改变 bootstrap CLI 的显式 bootstrap mode |
| DEL-002 | DEC-003、MOD-002、FLOW-002、ERR-002、IMP-002 | verify/install 同源架构 inventory 与 flattened 生成制品 | CLI full backend template、generated gate、Maven architecture plugin | 同一生成项目 verify/install 均无 blockingIssues；installed POM 无字面量 revision；外部 consumer 可解析 qualifier | TC-002、TC-003 | 不删除 CI-friendly revision，不发布制品 |
| DEL-003 | DEC-004、MOD-001、DM-002、FLOW-003、SEC-002、ERR-003、IMP-001 | 原子 release tuple upgrade | Mango CLI pmo upgrade、release manifest、项目受管文件 | dry-run 显示 Maven/CLI/PMO/npm；apply/reentry 一致；冲突不写；中断恢复原内容 | TC-004 | 不改用户自有依赖，不执行包发布 |
| DEL-004 | DEC-005、MOD-003、DM-003、FLOW-004、DB-001、SEC-001、ERR-004、IMP-003 | Bootstrap/runtime worker 隔离及读写集门禁 | Bootstrap、Job、Notice、Payment 等 provider starters | bootstrap context 无 runtime worker；plan/verify 无业务写；apply 只写 step allowlist；plan 不查未建业务表 | TC-005 | 不禁用 Bootstrap handler/service，不连接现有业务库 |
| DEL-005 | DEC-006、MOD-004、DM-004、FLOW-005、API-001、DB-003、UI-001、IMP-004 | generated typed ResourceDeclaration 与空库可用性 | business module template、Resource Registry、full template | 空库 apply 后 tenant/user/role/menu/API/business resource 非零；重入幂等；可登录并点击 generated menu | TC-006 | 不重新启用旧 runtime manifest runner，不批量改写外部用户仓库 |
| DEL-006 | DEC-007、MOD-002、MOD-005、DM-005、FLOW-006、FLOW-007、DB-002、ERR-005、IMP-005 | 结构化 BSQL 等价、性能和 Boot packaging 门禁 | `mango-maven-plugin` baseline、integration tests、Invoker | current monolith 可生成等价 baseline；B1/history/state/rows/time 有断言；root manifest 正例及缺失负例 fail-closed | TC-007 | 不用删除差异字符串放过，不发布 baseline |
| DEL-007 | DEC-008、MOD-006、FLOW-008、API-002、API-003、DB-004、SEC-003、ERR-006、UI-002、UI-003、IMP-006 | 自包含 tenant/workflow Chromium E2E | admin E2E support/specs 与必要真实 UI anchors | fixture 只清理本 run；定位不依赖“A公司”或中文展示名；共享 admin suite 串行稳定通过 | TC-009 | 不改变用户可见业务文案，不使用宽条件删除 |
| DEL-008 | MOD-001、MOD-002、MOD-003、MOD-004、MOD-005、MOD-006、MOD-007、IMP-001、IMP-002、IMP-003、IMP-004、IMP-005、IMP-006、IMP-009 | 能力说明、质量证据、真实消费链与专家复核 | CLI/Bootstrap/provider/Resource/Persistence/E2E README、`.runtime` evidence | M08 文档与实现一致；M09-M14 证据齐全；packed generate 到 BSQL consumer 和 Notice Chromium 无手工绕过；专家结论无阻断 | TC-008、TC-010、TC-015、TC-016、TC-017 | 不 commit、push、建 PR、写回/关闭 Issue 或发布 |
| DEL-009 | DEC-009、MOD-001、DM-006、FLOW-009、ERR-007、IMP-007 | module add workspace 事务与真实前端生成门禁 | CLI module add、business frontend templates、pnpm lockfile、packed fixtures | 长短命名生成后 frozen install 和 format/lint/typecheck/unit/build 立即通过；注入失败恢复原字节 | TC-011 | 不修改非受管依赖，不用隐式 pmo check 修 lockfile |
| DEL-010 | DEC-003、DEC-010、MOD-001、MOD-002、FLOW-002、FLOW-010、ERR-002、ERR-008、IMP-002、IMP-007 | module install/dev start 与可执行 Boot JAR | generated backend POM、generated gate、external process fixture | module add→install 后新进程标准 dev start；clean package 包含 Boot entries，java -jar 后 health/Flyway/resource 通过 | TC-012 | 不手工固化 revision，不用 spring-boot:run 替代生产制品 |
| DEL-011 | DEC-012、MOD-004、FLOW-011、API-004、API-005、SEC-004、ERR-009、UI-004、IMP-008 | 空库最低可登录数据与 PUBLIC 登录前合同 | Bootstrap、Identity/System/Auth/Resource、generated E2E | apply 后最低 tenant/admin/role/resource 非零；两条 PUBLIC API 匿名200、相邻受保护接口401；浏览器登录/menu/CRUD通过 | TC-013 | 不手工 seed，不扩大匿名 API 面 |
| DEL-012 | DEC-011、MOD-001、MOD-003、DM-007、FLOW-011、SEC-004、ERR-009、IMP-008 | stable generation 解析与可重启 runtime | CLI workspace/dev、Bootstrap receipt/control、real process E2E | generation/revision/fingerprint 从稳定来源显式传递；cold apply 后 start-stop-start 与 rolling generation 通过 | TC-014 | 不回退 generation=0，不接受人工首参/环境补丁 |
| DEL-013 | DEC-013、MOD-007、DM-008、FLOW-012、SEC-005、ERR-010、IMP-009 | API fallback 与 Vite source module 请求隔离 | `notice-announcement.spec.ts` | fallback 只处理 pathname 以 `/api/` 开头的请求；实际 `.ts/.vue` API 源码请求与 fallback 集合无交集；页面可见 | TC-015 | 不硬编码三个已知源码路径，不改变生产请求层 |
| DEL-014 | DEC-014、MOD-007、DM-009、FLOW-013、API-006、SEC-005、ERR-011、UI-005、IMP-009 | Notice 结构化通知与阈值浏览器合同 | `notice-site-message.spec.ts`、Notice client fixtures | realtime type/content/time/click、详情与 query 跳转、`<=10` 列表、`>10` 分类全部通过；渠道与敏感字段 fixture 对齐当前 API | TC-016 | 不回滚结构化 Notice，不用旧 title 代替消息类型 |
| DEL-015 | DEC-015、MOD-007、DM-010、FLOW-014、SEC-005、ERR-012、IMP-009 | Playwright reporter/artifacts 与 Vite watch 隔离 | `apps/mango-admin/playwright.config.ts`、`.runtime/playwright/mango-admin` | reporter/outputDir/定向截图位于 worktree runtime 根；config 可 list；Notice Chromium 期间无 report reload | TC-017 | 不通过关闭 HTML reporter 或修改生产 Vite 行为规避 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-002、DM-001、FLOW-001 | DEL-001 | Dev | CLI index、mango.dev.json、CLI tests | NONE | 实现 Spring Boot processMode 解析、兼容默认值、冲突校验和唯一首参拼装；清理 app.args 承载 mode 的旧可能路径 | command/custom command 边界明确，dev runtime 首参稳定，冲突错误可定位 | VAL-001 | B1-CLI | PLANNED |
| TASK-002 | MOD-001、FLOW-001、IMP-001 | DEL-001 | Dev | full application.yml、template checks | TASK-001 | 开启 locker 并把 mode/locker 静态与 context 断言接入现有 CLI 门禁 | 生成配置含 locker，真实 context 可获得 `ILeaseLocker` | VAL-001 | B1-CLI | PLANNED |
| TASK-003 | DEC-003、MOD-002、FLOW-002 | DEL-002 | Dev | ArchitectureMojo、generated backend gate | TASK-001 | 复现并统一 verify/install 的 reactor project、source 和 class ownership 输入；在标准生成项目执行 install 正向门禁 | 两 lifecycle 的 module catalog 完整一致且 blockingIssues 为空 | VAL-002 | B1-CLI | PLANNED |
| TASK-004 | DEC-003、FLOW-002、IMP-002 | DEL-002 | Dev | full backend root POM、CLI checks、external consumer fixture | TASK-003 | 按主仓模式配置 flatten plugin，检查 local repo installed POM，并从 reactor 外消费 qualified starter | installed POM 已解析 revision，外部 consumer resolve/compile 成功 | VAL-003 | B1-CLI | PLANNED |
| TASK-005 | DEC-004、DM-002、FLOW-003、SEC-002 | DEL-003 | Dev | CLI pmo upgrade、release manifest、CLI fixtures | TASK-001 | 从随包 manifest 构建受管 tuple plan，预检定位，dry-run 展示，整组写入与失败恢复，验证重入 | Maven/CLI/PMO/全部受管 npm 一致；冲突与写失败不留混版 | VAL-004 | B1-CLI | PLANNED |
| TASK-006 | DEC-006、DM-004、FLOW-005 | DEL-005 | Dev | business-module template、template README、CLI checks | TASK-002 | 用 Registry typed declaration 替换新生成模块的旧 resource-manifest 输出，并清理只服务旧生成路径的模板断言 | declaration identity/fields 合法稳定，新生成物不存在旧 manifest 路径 | VAL-005 | B1-CLI | PLANNED |
| TASK-007 | DEC-005、MOD-003、DM-003、FLOW-004 | DEL-004 | Dev | Bootstrap/Job/Notice/Payment auto-config 与 tests | NONE | 为 runtime worker bean 统一应用 runtime-only condition，保留 Bootstrap 所需 handler/service；检查 Lombok 机械代码边界 | bootstrap bean inventory 不含 scheduler/dispatcher/worker/ready listener，runtime 保持原装配 | VAL-006 | B2-BOOTSTRAP | PLANNED |
| TASK-008 | DB-001、SEC-001、ERR-004、TC-005 | DEL-004 | Dev/QA | Bootstrap integration tests、dedicated MySQL | TASK-007 | 为 plan/apply/verify 建立查询与写表分类断言，覆盖空库 plan 和 apply allowlist | plan 不查未建业务表；plan/verify 业务写集空；apply 无 runtime worker 越界写 | VAL-007 | B2-BOOTSTRAP | PLANNED |
| TASK-009 | MOD-004、FLOW-005、DB-003、API-001、UI-001 | DEL-005 | Dev/QA | Registry、generated full project、login/menu | TASK-006、TASK-008 | 在专用空库执行 typed declarations、重入、runtime、登录和 generated menu 验收 | 平台和业务资源计数/来源正确，登录与菜单用户结果通过 | VAL-008 | B2-BOOTSTRAP | PLANNED |
| TASK-010 | DEC-007、DM-005、FLOW-006、DB-002 | DEL-006 | Dev | BaselineGenerator、MySqlBaselineStore、unit/integration tests | NONE | 建立结构化 table/column/index/constraint snapshot 与受控 view/trigger canonical 比较，输出对象级 diff | charset/collation canonical 正例等价，真实 schema 变化负例阻断，unknown fail-closed | VAL-009 | B3-BSQL | PLANNED |
| TASK-011 | MOD-005、FLOW-006、TC-007 | DEL-006 | Dev/QA | PersistenceColdBaselinePerformanceIntegrationTest | TASK-010 | 固定 bootstrap mode 并断言 B1 history、control/module COMPLETED、导入行数、数据量与耗时 | cold baseline 生产路径真实执行且性能阈值有业务状态证据 | VAL-010 | B3-BSQL | PLANNED |
| TASK-012 | FLOW-007、ERR-005、TC-007 | DEL-006 | Dev | baseline-boot-package Invoker | TASK-010 | 固定 root manifest JAR 布局，证明 post-build hook 执行，并加入缺 entry 必失败用例 | 正例列出 root entry；缺 entry 与 hook 未执行都使 Invoker 失败 | VAL-011 | B3-BSQL | PLANNED |
| TASK-013 | DEC-008、FLOW-008、DB-004、SEC-003 | DEL-007 | Dev/QA | E2E support、menu/workflow specs、必要 UI component | TASK-009 | 抽取 run-owned tenant/category fixture 与安全 cleanup，按 code/data anchor 定位，相关 shared admin suites 串行 | 空库不依赖 seed copy，cleanup 不越界，Chromium 定向套件可重复通过 | VAL-012 | B4-E2E | PLANNED |
| TASK-014 | IMP-001、IMP-002、IMP-003、IMP-004、IMP-005、IMP-006、IMP-009 | DEL-008 | Dev | capability README/map | TASK-005、TASK-008、TASK-009、TASK-011、TASK-012、TASK-013、TASK-021、TASK-022、TASK-023 | 同步 CLI、Bootstrap/providers、Resource/menu、Persistence/BSQL、E2E 的接入、失败语义和验收入口 | 能力说明与代码/命令一致且专项 checker 通过 | VAL-013 | B5-CLOSE | PLANNED |
| TASK-015 | DEC-001、MOD-001、MOD-002、MOD-003、MOD-004、MOD-005、MOD-006、MOD-007、TC-008、TC-015、TC-016、TC-017 | DEL-008 | Dev/QA | packed CLI、runtime project、isolated Maven repo、dedicated DB、Chromium | TASK-004、TASK-005、TASK-009、TASK-011、TASK-012、TASK-013、TASK-014、TASK-021、TASK-022、TASK-023 | 从 packed CLI 生成项目，顺序执行 verify/install、upgrade、bootstrap plan/apply/verify、runtime health/login/menu、monolith BSQL、reactor 外 consumer 与 Notice Chromium | 20 项要求均有直接证据且全链无手工修改或绕过 | VAL-014 | B5-CLOSE | PLANNED |
| TASK-016 | SEC-001、SEC-002、SEC-003、TC-010 | DEL-008 | Tech Lead/QA | full diff、tests、evidence | TASK-015 | 执行测试质量门禁、机械 Java/Lombok review、旧入口搜索和 M14 独立视角复核 | 无旧 fallback、无新增测试替身风险；复核输出 blockers/advice/PASS-FAIL 且 blocker 为零 | VAL-015 | B5-CLOSE | PLANNED |
| TASK-017 | DEC-009、DM-006、FLOW-009、ERR-007 | DEL-009 | Dev | CLI module add、business frontend templates、pnpm workspace fixtures | TASK-006 | 在任何写入前校验显式模块/聚合中文显示名；把生成/格式化/managed integration/lockfile 更新纳入同一 snapshot/rollback；对短名与超长项目/模块/业务名执行真实渲染 | 纯英文显示名失败且项目原字节不变；命令成功后首次 frozen install 与完整 pnpm check 通过；formatter/pnpm 注入失败不留半模块 | VAL-016 | B1-CLI | PLANNED |
| TASK-018 | DEC-003、DEC-010、FLOW-002、FLOW-010、ERR-008 | DEL-010 | Dev/QA | full app POM、generated backend/artifact gate、external consumer/process | TASK-003、TASK-004、TASK-017 | 显式绑定 repackage；执行 module add→install→新进程 dev start 和 clean package→JAR entries→java -jar | installed POM 无 revision；标准 dev start、Boot manifest/BOOT-INF、health/Flyway/resource 均成立 | VAL-017 | B1-CLI | PLANNED |
| TASK-019 | DEC-012、MOD-004、API-004、API-005、SEC-004 | DEL-011 | Dev/QA | Bootstrap/Identity/System/Auth/Resource 与 generated browser fixture | TASK-008、TASK-009 | 将最低租户/管理员/角色/授权和平台/业务 typed resources 纳入 apply；统一 PUBLIC annotation 与匿名过滤链 | empty DB 计数非零，PUBLIC 200/protected 401，真实浏览器登录/menu/CRUD通过 | VAL-018 | B2-BOOTSTRAP | PLANNED |
| TASK-020 | DEC-011、DM-007、FLOW-011、ERR-009 | DEL-012 | Dev/QA | CLI workspace/dev、Bootstrap receipt/control、runtime E2E | TASK-008 | 解析并校验 stable generation/revision/fingerprint，显式注入 runtime；拒绝缺失/歧义/0/stale | cold apply 后两次启停及 rolling generation 均使用匹配稳定 identity | VAL-019 | B2-BOOTSTRAP | PLANNED |
| TASK-021 | DEC-013、DM-008、FLOW-012、ERR-010 | DEL-013 | Dev/QA | notice-announcement Playwright fixture | NONE | 用 pathname predicate 替换包含式 API catch-all，记录 fallback 与实际 Vite API source modules，加入非空 source 集及零交集断言 | 登录与公告页非白屏；所有 fallback pathname 都以 `/api/` 开头；`.ts/.vue` source 未被 fulfill | VAL-020 | B4-E2E | PLANNED |
| TASK-022 | DEC-014、DM-009、FLOW-013、API-006、UI-005、ERR-011 | DEL-014 | Dev/QA | notice-site-message spec、Notice fixture | NONE | mock unread category stats、route tags/reference impact，按敏感字段拆分更新渠道契约；覆盖 structured alert/detail/action、列表/分类阈值和 query 跳转 | Notice 单测与完整 Chromium 用例通过，不再断言旧 realtime title 或不可见动作 disabled | VAL-021 | B4-E2E | PLANNED |
| TASK-023 | DEC-015、DM-010、FLOW-014、ERR-012 | DEL-015 | Dev/QA | mango-admin Playwright config 与截图输出 | NONE | 将 outputDir、list+HTML reporter 和定向截图移动到 worktree `.runtime/playwright/mango-admin`，执行 config list 与 Notice Chromium 并审查 Vite 日志 | artifacts/report 均在外置根；无 `playwright-report/index.html` reload | VAL-022 | B4-E2E | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001、TASK-002、TASK-003、TASK-004、TASK-005、TASK-006、TASK-017、TASK-018 | Plan 批准且 TDD hash 有效 | CLI mode/locker/install/flatten/tuple/declaration/module transaction/Boot artifact 定向门禁全部满足 | NONE | TASK-007、TASK-010 | 发现新 public contract 或架构选择时停止并修订 TDD | Dev owner |
| MS-002 | TASK-007、TASK-008、TASK-009、TASK-019、TASK-020 | runtime-only condition 已明确，generated declaration 可用 | Bootstrap bean/query/write、minimum login/PUBLIC、stable generation 与两次重启证据成立 | MS-001 的 TASK-002、TASK-006、TASK-018 | TASK-010、TASK-011、TASK-012 | worker/最低数据/identity 与 Bootstrap 冲突时回到对应设计决策 | Bootstrap owner |
| MS-003 | TASK-010、TASK-011、TASK-012 | 专用 MySQL 可用且 current migrations 可执行 | semantic BSQL、performance 与 packaging 正负例全部成立 | NONE | TASK-001、TASK-007、TASK-013 | metadata 无法表达对象语义时回到 DEC-007，不删差异 | Persistence owner |
| MS-004 | TASK-013、TASK-019、TASK-021、TASK-022、TASK-023 | MS-002 的空库平台与业务资源可登录且 PUBLIC 合同成立；Notice fixture 可独立运行 | self-owned fixture 的登录/menu/CRUD/workflow 与 Notice pathname/structured/reporter Chromium 定向套件稳定 | MS-002 | TASK-014 | API/fixture/artifact 不满足安全隔离时修订 TDD，不宽删或放松断言 | Frontend QA owner |
| MS-005 | TASK-014、TASK-015、TASK-016 | MS-001 至 MS-004 全部满足 | M08、M09-M14、test quality、20 项真实消费链和专家复核无阻断 | MS-001、MS-002、MS-003、MS-004 | NONE | 任一证据只能证明局部时补足对应真实入口，不缩小结论 | Dev/QA/Tech Lead |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001 | TASK-001、TASK-002 | M09/M10/M11 | 执行 `node mango-ui/packages/mango-cli/scripts/check-cli.mjs` 中 mode/locker 正负例，再用 packed full app 启动 context | Node、Maven、JDK21 | new/legacy/conflict manifest | task workspace | runtime 首参唯一、冲突失败、locker bean 可用 | `.runtime/issue-690/cli-mode-locker/` | Dev | 任一字符串断言未进入真实 command/context 时补真实 smoke |
| VAL-002 | TC-002 | TASK-003 | M11 | 执行 generated backend gate 的 clean verify 与 clean install，并读取两次 architecture report | isolated generated project/local repo | standard controller/service | 无业务库 | module inventory 等价且 blockingIssues 为空 | `.runtime/issue-690/generated-architecture/` | Dev/QA | install 误报继续定位 lifecycle inputs，不改规则放行 |
| VAL-003 | TC-003 | TASK-004 | M09/M11 | install qualified reactor，检查 local repo POM，再在 reactor 外 consumer 执行 Maven verify | isolated local repo/consumer | workspace-qualified starter | 无 registry 写入 | POM 无字面量 revision且 consumer resolve/compile | `.runtime/issue-690/flatten-consumer/` | Dev/QA | reactor 内通过不能替代 consumer，失败即停 |
| VAL-004 | TC-004 | TASK-005 | M10/M11 | CLI fixtures 依次执行 upgrade dry-run、apply、reentry、manifest 缺项、冲突和注入写失败 | temp project fixtures | 旧 release tuple | 仅临时文件 | 全生态一致、非受管字段不变、失败恢复 byte-for-byte | `.runtime/issue-690/release-tuple/` | Dev | 部分写入即阻断并修复 plan/rollback |
| VAL-005 | TC-006 | TASK-006 | M09/M10 | 生成 business module 并执行 CLI check、Resource schema validation、旧路径搜索 | generated fixture | module code `order` | 无数据库 | typed declaration 合法且旧 manifest 不再生成 | `.runtime/issue-690/resource-template/` | Dev | 旧生成入口残留则删除对应模板/断言 |
| VAL-006 | TC-005 | TASK-007 | M10/M11 | 对 bootstrap/runtime modes 启动 ApplicationContextRunner/Spring context，断言实际 worker 与 handler 集 | backend test JVM | Job/Notice/Payment providers | 无共享库 | worker 只在 runtime，Bootstrap handler 保留 | `.runtime/issue-690/bootstrap-beans/` | Dev | 只测 bean 数量不计证据，必须按 worker 行为类型断言 |
| VAL-007 | TC-005 | TASK-008 | M11 | 在专用 MySQL 执行 plan/apply/verify，捕获查询表与 before/after write set | dedicated issue DB | empty schema/declarations | 仅 task DB | plan 不查询未建表；plan/verify 无业务写；apply 属于 allowlist | `.runtime/issue-690/bootstrap-write-set/` | QA | 越界即记录 bean/table/command 并停止 |
| VAL-008 | TC-006 | TASK-009 | M11/M12/M13 | empty DB apply/reentry→runtime health→POST /auth/login→查询计数/来源→Chromium 点击 generated menu | packed full project | generated order module | default/system tenant与测试账号 | 六类基础/业务资源非零，登录和菜单操作真实成功 | `.runtime/issue-690/empty-db-resource/` | QA | health UP 但数据或菜单为空判 FAIL |
| VAL-009 | TC-007 | TASK-010 | M10/M11 | 执行 plugin unit/integration tests，覆盖 implicit/explicit charset-collation 等价及 column/index/constraint 差异 | JDK21、MySQL | canonical DDL fixtures | temporary schema | 语义等价通过，真实差异与 unknown 输出 object path 并失败 | `.runtime/issue-690/bsql-semantic/` | Dev | 禁止通过删除差异文本修复测试 |
| VAL-010 | TC-007 | TASK-011 | M11 | 定向执行 `PersistenceColdBaselinePerformanceIntegrationTest` | dedicated MySQL | 5 modules/375 tables/37500 rows | temporary schema | mode=bootstrap，B1/history、COMPLETED、rows/size/time 均断言 | `.runtime/issue-690/bsql-performance/` | Dev/QA | skipped cold path 或只看 exit code 判 FAIL |
| VAL-011 | TC-007 | TASK-012 | M09/M11 | 执行 Maven Invoker baseline-boot-package 正例与 missing-entry 负例，列出 JAR entries 和 hook sentinel | Maven Invoker | Boot package fixture | isolated IT dir | root manifest 存在；缺失或 hook 未执行使 build 失败 | `.runtime/issue-690/bsql-packaging/` | Dev | Invoker SUCCESS 但 sentinel/negative 未观察判 FAIL |
| VAL-012 | TC-009 | TASK-013 | M12/M13 | 以 Chromium 串行运行 menu-navigation 与 workflow-management 定向 specs，重复一次验证稳定性 | dedicated app/DB | run-unique tenant/category codes | suite-owned data only | 不依赖中文 copy，无共享状态并发假失败，cleanup 精确 | `.runtime/issue-690/e2e/` | Frontend QA | trace/network/console 或 cleanup 异常逐项失败 |
| VAL-013 | TC-001、TC-005、TC-007、TC-009 | TASK-014 | M09 | 执行 capability docs checker、链接/命令复核和 diff review | repository | affected README/map | 无外部写 | 每类公开变化都有真实用法、失败语义和验收入口 | `.runtime/issue-690/capability-docs/` | Dev | 文档与代码漂移即修正文档或实现 |
| VAL-014 | TC-008 | TASK-015 | M11/M12/M13 | 使用 packed CLI 和专用资源完整执行 generate→verify/install→upgrade→bootstrap plan/apply/verify→runtime→login/menu→monolith BSQL→external consumer→Notice Chromium | `.runtime/projects`、isolated repo、dedicated DB、Chromium | workspace mango_023 与 generated module | 不写现有业务库 | 20 项逐项有直接证据且无手工修改 | `.runtime/issue-690/true-consumer-chain/` | Dev/QA | 任一步失败回到 owning task，整链重跑 |
| VAL-015 | TC-010 | TASK-016 | M09/M14 | 执行 `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、changed test mock audit、`git diff --check`、旧入口搜索和独立 Tech Lead/QA 复核 | repository + all evidence | full diff | 无外部写 | test quality PASS；旧 fallback 清理；复核 blocker=0 | `.runtime/issue-690/review/` | Tech Lead/QA | blocker 未关闭不得进入完成审计 |
| VAL-016 | TC-011 | TASK-017 | M09/M10/M11 | packed CLI 先用纯英文 `--module-name`/`--aggregate-name` 验证零写入失败，再对短名/长名项目各执行 module add→首次 `pnpm install --frozen-lockfile`→format/lint/typecheck/unit/build，并注入 formatter/pnpm 写失败 | `.runtime/projects`、isolated pnpm store | invalid + two naming fixtures | workspace files only | 无效显示名明确失败且受管文件/目录原字节不变；lockfile含新 importers/api-schema；真实产物格式正确；失败恢复 byte-for-byte | `.runtime/issue-690/module-add-transaction/` | Dev/QA | 隐式第二次 install 或仅字符串基线通过判 FAIL |
| VAL-017 | TC-012 | TASK-018 | M09/M11/M12 | generated module install 后从新进程执行标准 dev start；再 clean package、检查 Main-Class/Start-Class/BOOT-INF，并 `java -jar` 验证 health/Flyway/resource | isolated repo/ports/dedicated DB | qualified generated app | task DB only | dev start 与 production JAR 均无需手工 POM/参数修改 | `.runtime/issue-690/generated-artifact/` | Dev/QA | spring-boot:run 通过不能替代 java -jar；普通 jar 立即失败 |
| VAL-018 | TC-013 | TASK-019 | M11/M12/M13 | empty DB apply/reentry 后未登录请求 branding/login-options 与 protected 对照，Chrome 登录并点击 generated menu 完成 CRUD | dedicated DB/Chromium | minimum Bootstrap identities/resources | system/default tenant | PUBLIC 200/protected 401；登录/menu/CRUD真实通过 | `.runtime/issue-690/empty-db-login/` | QA | health-only、手工 seed 或宽匿名判 FAIL |
| VAL-019 | TC-014 | TASK-020 | M10/M11/M12 | CLI receipt parser 正负例；real chain 执行 cold apply→start→stop→start→rolling generation，并核对每次 process args/state | dedicated DB/workspace | stable + next generation receipts | task DB only | generation>0且revision/fingerprint匹配；两次重启与滚动升级通过 | `.runtime/issue-690/runtime-generation/` | Dev/QA | 默认0、人工 env 或读取错库立即失败 |
| VAL-020 | TC-015 | TASK-021 | M09/M13 | ESLint/Prettier 后运行 `notice-announcement.spec.ts` Chromium，记录实际 `/@fs`/`/src/api` 模块与 fallback request sets | local Vite/Chromium | existing mocked announcement fixture | browser fixture only | source set 非空、fallback 全为 `/api/`、交集为空、公告页可见 | `.runtime/playwright/mango-admin/` | Frontend QA | source 集为空视为证据不足；任一 MIME/console/request 失败阻断 |
| VAL-021 | TC-016 | TASK-022 | M10/M13 | 执行 `@mango/notice` 单测及 `notice-site-message.spec.ts` Chromium | local Vite/Chromium | realtime message、1/12 unread stats、channel fixtures | current test user/tenant | structured fields、详情/业务 query、list/grouped、secretValues 与完整管理链通过 | `.runtime/playwright/mango-admin/` | Frontend QA | 只过组件或只过新增前半段不算通过，必须整条 spec 收口 |
| VAL-022 | TC-017 | TASK-023 | M09/M13 | `playwright test --list` 后运行两条 Notice Chromium specs，检查 report/artifacts absolute path 与 WebServer log | local/external Vite | Playwright config | worktree runtime only | 579 tests 可枚举；两 specs PASS；产物在 `.runtime`；无 reporter reload | `.runtime/playwright/mango-admin/report` | Frontend QA | Vite root 内产物或 reload 立即失败 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DEC-005、DB-001 | `mango_dev_mango_issue_690_weekly_regression_023` 或同任务派生库 | 名称匹配 `mango_dev_*`、库不存在或明确为本任务一次性库、连接参数不含共享环境 | 创建空库，执行 Bootstrap 三命令并记录 table/query/write set | TASK-008 后 TASK-009/015 | 无业务数据备份；仅一次性库 | VAL-007、VAL-008、VAL-014 | URL/库名不匹配、库内存在非本任务数据即停止 | 仅删除确认属于本任务的临时 schema，保留诊断日志 | QA |
| DATA-002 | DEC-007、DB-002 | BSQL 临时 MySQL schemas | schema prefix/task ownership、migration catalog 与 checksum 可记录 | 生成 V/B1 两侧 schema snapshots、performance data 和 consumer DB | TASK-010 后 TASK-011/012/015 | 无生产回填 | VAL-009、VAL-010、VAL-011、VAL-014 | metadata unknown、schema diff、性能状态不完整即停止 | plugin finally cleanup；失败时先保留安全 diff 再清理 | Persistence owner |
| DATA-003 | DEC-008、DB-004 | E2E 专用应用数据库 | fixture code prefix 与 API token scope 验证 | API 创建 tenant/category/definitions 并按 captured id/code 逆序清理 | TASK-013/015 | 不备份；只操作 suite-owned records | VAL-012、VAL-014 | 无法证明 record ownership 时禁止删除 | 输出 handle，人工仅按 id/code 定向清理 | Frontend QA |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001、IMP-002、IMP-007、DEL-001、DEL-002、DEL-003、DEL-009、DEL-010 | CLI README、full template README、business module template README | processMode、locker、flatten、release tuple、module add 事务/frozen check、external consumer、Boot JAR 和升级恢复 | CLI owner | 示例与 packed CLI 行为一致 | VAL-013、VAL-016、VAL-017 | M08 已启用，适用 |
| DOC-002 | IMP-003、IMP-004、IMP-008、DEL-004、DEL-005、DEL-011、DEL-012 | Bootstrap、Job、Notice、Payment、Resource/Auth/System README/能力地图 | bootstrap/runtime bean 边界、typed declaration、minimum login/PUBLIC、stable generation 与 restart | provider/Resource/Auth owners | 不复制 PMO 长期规则，链接真实配置和命令 | VAL-013、VAL-018、VAL-019 | M08 已启用，适用 |
| DOC-003 | IMP-005、DEL-006 | Persistence/BSQL README | schema semantic equality、performance contract、root JAR manifest 与 monolith golden case | Persistence owner | 与 test/Invoker 实际 entry 一致 | VAL-013 | M08 已启用，适用 |
| DOC-004 | IMP-006、IMP-009、DEL-007、DEL-013、DEL-014、DEL-015 | admin E2E README 或测试使用说明 | fixture ownership、stable code/data anchors、pathname API mock、structured Notice、外置 artifacts、serial Chromium 与 cleanup | Frontend QA | 定向命令在当前 package scripts 可执行且报告路径明确 | VAL-013、VAL-020、VAL-021、VAL-022 | M08 已启用，适用 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 四批共享生成物导致后批掩盖前批错误 | 根因和证据不可靠 | 按 MS-001 至 MS-004 独立门禁后再汇合 | 回到 owning task 修复并重跑该批与全链 | Dev owner | 交付前 | CLOSED | NONE | NONE |
| RISK-002 | L3 | RISK | MySQL 或 E2E fixture 指向共享数据 | 数据污染或越界删除 | DATA-001 至 DATA-003 名称与 ownership 预检 | 停止执行，不做宽清理，报告精确 handle | QA owner | 首次写库前 | CLOSED | NONE | NONE |
| RISK-003 | L3 | RISK | schema normalization 或 worker isolation 通过放宽断言实现 | 假通过并保留投产阻断 | 真实差异负例、write set、unknown fail-closed 和 M14 review | 拒绝删差异/跳过测试，回修设计实现 | Tech Lead | 交付前 | CLOSED | NONE | NONE |
| RISK-004 | L2 | RISK | 全链耗时导致只跑局部验证 | 不能证明 Issue 放行标准 | 定向门禁先行，最终复用一次性 repo/DB 完整跑 VAL-014 | 保持目标未完成并继续运行，不用局部 PASS 替代 | QA owner | 完成审计前 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001、DEC-002、MOD-001、DM-001、FLOW-001、ERR-001、TC-001 | DEL-001 | TASK-001、TASK-002 | VAL-001 | MS-001、DOC-001、RISK-001 | CLI mode 与 locker |
| DEC-003、MOD-002、FLOW-002、ERR-002、TC-002、TC-003、IMP-002 | DEL-002 | TASK-003、TASK-004 | VAL-002、VAL-003 | MS-001、DOC-001、RISK-001 | verify/install、flatten、consumer |
| DEC-004、DM-002、FLOW-003、SEC-002、ERR-003、TC-004、IMP-001 | DEL-003 | TASK-005 | VAL-004 | MS-001、DOC-001、RISK-001 | atomic release tuple |
| DEC-005、MOD-003、DM-003、FLOW-004、DB-001、SEC-001、ERR-004、TC-005、IMP-003 | DEL-004 | TASK-007、TASK-008 | VAL-006、VAL-007 | MS-002、DATA-001、DOC-002、RISK-002 | Bootstrap beans/query/write set |
| DEC-006、MOD-004、DM-004、FLOW-005、API-001、DB-003、UI-001、TC-006、IMP-004 | DEL-005 | TASK-006、TASK-009 | VAL-005、VAL-008 | MS-001、MS-002、DATA-001、DOC-002 | typed Resource 与 empty-db menu |
| DEC-007、MOD-002、MOD-005、DM-005、FLOW-006、FLOW-007、DB-002、ERR-005、TC-007、IMP-005 | DEL-006 | TASK-010、TASK-011、TASK-012 | VAL-009、VAL-010、VAL-011 | MS-003、DATA-002、DOC-003、RISK-003 | BSQL semantic/performance/packaging |
| DEC-008、MOD-006、FLOW-008、API-002、API-003、DB-004、SEC-003、ERR-006、UI-002、UI-003、TC-009、IMP-006 | DEL-007 | TASK-013 | VAL-012 | MS-004、DATA-003、DOC-004、RISK-002 | self-contained Chromium E2E |
| DEC-009、MOD-001、DM-006、FLOW-009、ERR-007、TC-011、IMP-007 | DEL-009 | TASK-017 | VAL-016 | MS-001、DOC-001、RISK-001 | module add lockfile/format transaction 与完整前端门禁 |
| DEC-003、DEC-010、MOD-001、MOD-002、FLOW-002、FLOW-010、ERR-002、ERR-008、TC-012、IMP-002、IMP-007 | DEL-010 | TASK-018 | VAL-017 | MS-001、DOC-001、RISK-001 | install/dev start 与可执行 Boot JAR |
| DEC-012、MOD-004、FLOW-011、API-004、API-005、SEC-004、ERR-009、UI-004、TC-013、IMP-008 | DEL-011 | TASK-019 | VAL-018 | MS-002、MS-004、DATA-001、DOC-002、RISK-002 | minimum login、PUBLIC boundary、menu/CRUD |
| DEC-011、MOD-001、MOD-003、DM-007、FLOW-011、SEC-004、ERR-009、TC-014、IMP-008 | DEL-012 | TASK-020 | VAL-019 | MS-002、DATA-001、DOC-002、RISK-004 | stable generation、restart、rolling identity |
| DEC-013、MOD-007、DM-008、FLOW-012、SEC-005、ERR-010、TC-015、IMP-009 | DEL-013 | TASK-021 | VAL-020 | MS-004、DOC-004、RISK-001 | pathname API fallback 与 Vite source module 隔离 |
| DEC-014、MOD-007、DM-009、FLOW-013、API-006、SEC-005、ERR-011、UI-005、TC-016、IMP-009 | DEL-014 | TASK-022 | VAL-021 | MS-004、DOC-004、RISK-001 | structured Notice、list/grouped、详情与跳转、渠道 fixture |
| DEC-015、MOD-007、DM-010、FLOW-014、SEC-005、ERR-012、TC-017、IMP-009 | DEL-015 | TASK-023 | VAL-022 | MS-004、DOC-004、RISK-001 | external reporter/artifacts 与无 Vite reload |
| DEC-001、MOD-001、MOD-002、MOD-003、MOD-004、MOD-005、MOD-006、MOD-007、TC-008、TC-010、TC-011、TC-012、TC-013、TC-014、TC-015、TC-016、TC-017 | DEL-008 | TASK-014、TASK-015、TASK-016 | VAL-013、VAL-014、VAL-015、VAL-016、VAL-017、VAL-018、VAL-019、VAL-020、VAL-021、VAL-022 | MS-005、DOC-001、DOC-002、DOC-003、DOC-004、RISK-004 | M08、M09-M14、20 项 true consumer chain、expert review |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/2026-08-02-issue-690-weekly-regression-implementation-plan.md` |
| 生命周期 handoff | PASS | `check-lifecycle-handoff.mjs` 按 FULL 的 TDD→Plan 适用阶段检查 hash、trace 与 gate |
| 依赖图 | PASS | TASK-001 至 TASK-023 前置关系无环；MS-001 至 MS-004 独立，MS-005 汇合 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-004 为已通过计划控制关闭的风险；无 BLOCKER 或 EXCEPTION |
| 实施审批 | APPROVED | HardyDou 于 2026-08-02 明确要求不再确认并开始目标模式，随后要求自行吸收 Issue #690 追加回归；记录见 `mango-docs/designs/issue-690-weekly-regression/review/PLAN-APPROVAL.md` |
