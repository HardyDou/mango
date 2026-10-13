已读完 `mango-pmo/rules/03-ai-coding-redlines.md`、`mango-pmo/agents/03-dev-agent.md`，并按 preflight（role=dev，phase=develop）核对了事实。先说结论：**方案本身不需要新增任何结构，但需求里“24 个”和“独立引用”两个前提与我实测到的事实有偏差，需要先 ASK 确认口径。**

## 事实核对（先说这个，它决定后面四项怎么写）

- `mango-ui/packages/common/components` 共 34 个目录，10 个有 `README.md`，24 个没有 → **数量与需求一致**。
- 但契约登记口径不同：`component-contracts.json` 只登记 17 个组件，其中缺 README 的只有 7 个（`MangoAvatar`、`MangoTableCell`、`RichTextViewer`、`MangoPageBackBar`、`MangoSideDrawerShell`、`MangoDetailSummary`、`MangoDescriptionList`）；另外 17 个目录（`Captcha`、`DictTag`、`TreeSelect`、`SSE`、`Websocket`、`ECharts`、`Editor`、`FormCreate`、`Sign` 等）**不在契约登记内**，属于未纳管的存量公共导出。
- 关键反例：`MangoAvatar` 的契约 `docs.path` 指向的是 `mango-ui/packages/common/README.md`（`mango-ui/packages/common/component-contracts.json:18`），而该包级 README 第 473 行已有 `MangoAvatar` 的完整语义说明和使用示例。也就是说“缺少组件级 README”不等于“缺少文档”。`RichTextViewer` 同理（包 README 第 367、629 行）。
- 双格式不一致：`docs.requiredSections` 写的是 `["概览","接入方式","API 与扩展","问题排查"]`，而 `mango-pmo/tools/audit-module-readmes.mjs` 的 `frontendEntrySectionGroups` 要求 9 段（含功能清单、参数与事件、后端依赖、权限与数据边界、快速开始、相关文档）。标杆 `MangoDialog/README.md` 实际是“组件级详述段 + 包审计九段”的叠加体。

**ASK 点（阻塞，需用户确认）**：交付范围是“24 个目录全覆盖”，还是“契约登记的 7 个 + 其余按需”？两者工作量与门禁含义不同；未纳管的 17 个是否要在本次一并登记进契约，属于上游设计决策，Dev 阶段不得自行新增。

## 1. 复用判断（REDLINE-REUSE-010）

按顺序逐级落位，结论是**第 1～4 级已完全覆盖，不需要进入第 5 级**：

| 层级 | 复用对象 | 复用方式 |
|---|---|---|
| 1. Mango 已有能力 | `MangoDialog/README.md`、`MangoListPage/README.md` 等 10 份组件 README | 作为格式标杆逐节对齐（双段结构：详述段 + 审计九段） |
| 1. Mango 已有能力 | `mango-pmo/tools/audit-module-readmes.mjs` 的 `frontendEntrySectionGroups` | 作为章节要求的唯一权威口径 |
| 1. Mango 已有能力 | `mango-pmo/tools/audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`mango-ui/scripts/quality/check-component-contracts.mjs` | 作为验证入口，不新写校验脚本 |
| 1. Mango 已有能力 | `mango-ui/packages/common/README.md` 第 418–631 行的组件导出表与 API 说明 | 作为每个组件 README 的事实源，避免重复描述导致口径分裂 |
| 2. 当前模块已有实现 | `components/<Name>/index.vue` 及其 `types.ts`、`__tests__/` | props/emits/slots/expose/类型的唯一事实来源 |
| 3. 标准库 | Markdown 本身 | 无新增依赖 |
| 4. 已装依赖 | 无（文档任务不消费运行时依赖） | 不适用 |

**被放弃的第 5 级候选与原因**：不新增 README 生成脚本、不新增文档校验工具、不新增文档站条目、不改 `docs.requiredSections`。原因：现有审计脚本已能覆盖章节缺失、占位词、可执行命令、源码事实引用四类检查，再加一层生成器属于本 PR 无消费者结构（同时命中 `REDLINE-OVERDESIGN-060`）。

## 2. 术语（REDLINE-TERMS-030）

- 本任务**不引入新的公共类型、API、配置、权限码、事件、页面 key、Resource 类型或抽象概念**。产出物是 Markdown，不参与类型系统。
- 文档正文中出现的每个标识符都必须逐字取自源码（`index.vue` 的 `defineProps`/`defineEmits`/`defineExpose`、`types.ts` 导出名、`index.ts` 导出名），**文档作者无权新造或改名**。
- 命名一致性风险集中在三处，需按现有主术语写：
  1. `MangoTableCell` 的实际实现位于 `components/MangoDataTable/MangoTableCell.vue`，没有独立目录。文档放在哪里、以什么标题呈现，必须沿用契约里已有的主术语 `MangoTableCell`，不得改叫 `MangoDataTableCell` 之类。
  2. `MangoDetailSummary` / `MangoDescriptionList` 名称相近，`MangoPageBackBar` / `MangoSideDrawerShell` 同属页面外壳族——必须逐一确认各自的主职责，避免写成互为同义词。
  3. 已存在的 `Pagination`（无 Mango 前缀）与其他 `Mango*` 前缀并存，这是既有事实，不在本任务内重命名（重命名属兼容性变更）。
- **不确定项一律 ASK**：组件边界不清、两个组件职责疑似重叠、组件导出名与目录名不一致时，先 ASK 再落笔，不自行判定同义词。
- 文档内不得新增“必须/禁止”类长期规则正文（`rules/08-capability-docs.md` 第 6 节禁止项），约束性描述只允许出现在“重要约束”小节且仅复述组件真实行为。

## 3. 最小实现（REDLINE-MINIMAL-020）

- **改动边界**：仅新增 `mango-ui/packages/common/components/<Name>/README.md`。不改 `index.vue`、`types.ts`、`index.ts`、`component-contracts.json`、包级 `README.md`、任何 `mango-pmo/rules/**`。若确认需要同步契约 `docs.path` 或 `requiredSections`，那属于上游设计变更，需单独 ASK 并回到 tech-lead 阶段，不在本任务内顺手改。
- **不在边界内**：源组件行为修复、缺失测试补齐（34 个目录中 13 个无 `__tests__`）、未纳管组件的契约登记、包级 README 重排。
- **当前消费者**：`@mango/common` 的使用方——业务仓开发者按 npm 包独立消费组件时，需要在不读源码的前提下判断 props/emits/slots 与适用场景；`pnpm -C mango-ui component-contracts:check` 与 `node mango-pmo/tools/audit-module-readmes.mjs` 需要 README 作为被审计对象。注意 `MangoAvatar`、`RichTextViewer` 已有包级 README 说明，其消费者是“需要单组件细节”的人，需在 ASK 中确认是否真需要，否则这两份属于低价值重复。
- **责任边界**：适用于公开从 `@mango/common` 导出的、组件语义完整可独立描述的组件；不适用于页面私有文件（如 `MangoTableCell.vue` 这类内嵌实现要按契约口径单独裁决）、不适用于 `hooks/`、`utils/`、`api/`（属包级 README 范围）。
- **验证入口**（按成本从低到高，逐条给出真实输出）：
  1. `node mango-pmo/tools/audit-module-readmes.mjs` —— 每份 README 的 9 个章节组必须全 `OK`。
  2. `node mango-pmo/tools/audit-readme-source-facts.mjs` —— 校验 README 引用的源码路径与命令真实存在。
  3. `pnpm -C mango-ui component-contracts:check` —— 当前基线 `component contracts PASS {"registryCount":20,"componentCount":51,"legacyCount":179,"publicVueExportCount":230}`，改后必须仍 PASS（新增纯 README 不应改变该输出）。
  4. 反向核对：从 `index.vue` 与 `types.ts` 逐项比对文档中每个 prop/emit/slot/类型，确保无遗漏、无编造、无默认值错误。
- 这份交付的“最小”不表示可以省略内容深度：`rules/frontend/03-component-development.md` 第 9 节要求公共组件文档必须说明适用场景、不适用场景、props、emits、slots、类型和样式变量，深度不可打折。

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不生成固定问卷，不生成术语表。** 依据：本任务不新增公共概念，认知风险集中在“组件职责归属”这一处，按需记录术语决定即可。具体做法：

- 不新建任何问卷、模板、核对表文件。
- 只在交付说明中记录一条术语决定：本次是否就 `MangoTableCell`、`MangoDetailSummary`、`MangoDescriptionList`、`MangoPageBackBar`、`MangoSideDrawerShell` 的职责边界进行了 ASK，以及 ASK 的结论。
- 若 ASK 后用户未回复而必须先动工，**停下，不猜**，把未确认的组件标记为阻塞，不写推测性文档。

另需注意：本任务本质落在 `rules/08-capability-docs.md` 的 M08 范围（改变前端公开组件的接入说明），触发的是**能力文档同步**而非问卷；M08 的开关与 `ENABLE/DISABLE` 判定需在交付说明中显式给出，这一条在 preflight 的 Must read 第 19 项里。交付模式按 `rules/11-delivery-assurance.md` 判定；本任务建议 STANDARD（多文件但机械性一致），是否升级 FULL 需按 M08 判定结果确认。

## 5. 过度设计（REDLINE-OVERDESIGN-060）

逐条对照禁止清单，本方案的判定与预防：

| 过度设计条目 | 本任务是否命中 | 说明 |
|---|---|---|
| 新增抽象但本 PR 无消费者 | 不命中 | 不新增抽象 |
| 新增配置项但无读取逻辑 | 不命中 | 不改任何配置文件 |
| 新增接口/扩展点且只有一个实现 | 不命中 | 不新增接口 |
| 新增依赖但标准库已够 | 不命中 | 不新增依赖，`package.json` 不动 |
| 新增公共类型但仅单模块内部使用 | 不命中 | 不新增类型 |
| 同一概念多个名称 | **需主动防御** | 24 份文档横跨同一包，最容易在这里踩坑：同一 prop 或 slot 在不同组件文档里被写成不同叫法、`MangoDetailSummary` 与 `MangoDescriptionList` 被描述成同义、`MangoTableCell` 出现新名字。防御手段是全部标识符逐字抄自源码，并在交稿前做一次跨 24 份文档的名称一致性扫描。 |
| 以“代码更整洁”为由新增抽象层 | **需主动防御** | 最可能的形态是“顺手写个 README 生成器/校验脚本/共享章节片段”。明确不做：现有 `audit-module-readmes.mjs` 已覆盖章节校验，生成器属无消费者结构。 |

额外需要点出的两个真实风险（属质量边界，不是过度设计，但必须写明）：

1. **内容重复导致口径分裂**：`MangoAvatar`、`RichTextViewer` 等的包级 README 已有说明，若逐字复制进组件 README，后续两处必然漂移。处理方式应是一处详述、另一处指向，具体取舍需在 ASK 中随范围一并确认。
2. **文档不得充当验收证据**：`rules/03-ai-coding-redlines.md` 第 4 节禁止以文档替代真实链路验证。本次只声明文档交付完成，不声明任何组件功能、接口或页面的可用性变化；未跑过的组件测试一律标记未验证——这一点尤其要写清，因为 13 个目录本身没有 `__tests__`。

**下一步**：等你回复两个 ASK 后即可进入实施——(a) 交付范围是 24 个目录全覆盖，还是契约登记的 7 个（其余 17 个未纳管组件是否本次纳管）；(b) `MangoAvatar`、`RichTextViewer` 这类包级 README 已覆盖的组件，是否仍要独立 README，以及重复内容以哪一处为准。

本轮未修改任何文件，仅执行了读取与只读检查命令。
