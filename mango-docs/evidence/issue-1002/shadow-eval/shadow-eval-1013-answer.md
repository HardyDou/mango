读完了两个规范文件和 `types.ts`、`MangoTableCell.vue`、README、`common/index.ts` 及现状消费者。以下是实现方案（未做任何改动）。

## 1. 复用判断（REDLINE-REUSE-010）

按顺序落到第 3 项为止，第 5 项只在 types.ts 补一个字面量：

| 顺序 | 对象 | 结论 |
|---|---|---|
| 1. Mango 已有能力 | `MangoDataTable` 的 `type` 分支链、`MangoTableCell.vue` 已有的 `rawValue` / `setByPath` / `beginEditing` / `commitChange` 提交链路、`MangoTableOptionValue = string \| number \| boolean`、`column.disabled`、`column.props` | 全部复用。**不需要新增开关组件**（`packages/common/components/` 下没有 MangoSwitch，也不需要建） |
| 2. 当前模块已有实现 | `input` / `select` / `radio` 三类可编辑单元格已经完成“取值 → 写回 `row[field]` → 规范 change 事件（含 previousValue）”的全链路，switch 只是换渲染控件，提交语义完全一致 | 复用 `handleValueUpdate` + `commitChange`，不新增提交路径 |
| 3. 标准库 | Vue `computed`；Element Plus `el-switch` 支持 `active-value` / `inactive-value` / `inline-prompt` / `active-text` / `inactive-text` | 直接用，不包一层 |
| 4. 已装依赖 | 无新增 | — |
| 5. 新增结构 | **只有一处**：`MangoTableColumnType` 联合类型增加 `'switch'` 一个字面量。不新增类型、不新增 props 字段、不新增配置项、不新增依赖 | 放弃的复用对象：用 `type: 'select'` + 两个 option 模拟开关（交互语义不同，会变成下拉框，不采纳）；用 `type: 'custom'` + 业务页自己写 `el-switch`（把同一段模板复制到多个页面，正是要消除的重复，不采纳） |

## 2. 术语（REDLINE-TERMS-030）

先看既有术语（`types.ts:7`）：

```ts
export type MangoTableColumnType = 'text' | 'status' | 'tag' | 'button' | 'input' | 'select' | 'radio' | 'custom';
```

既有规律：**单个小写英文词、与控件名对应、不带 `el-` / `mango` 前缀**（`radio` 对应 `el-radio-group`，已经是同一个先例）。用 `'switch'` 一致，不产生同义词。

需要你确认的两点：
- `switch` 是 JS 关键字/保留字语义词，但这里只用作字符串字面量，不影响编译；且同维度命名还有 `select`（同样是语言关键字）先例，可接受。
- 文档用中文叫“开关”还是“开关列”？README 第 37 行只列 `type` 的值清单，历史写法是直接列类型名，不额外起中文别名，所以**不新增中文术语**，避免“开关/切换/是否”三种说法并行。

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界（3 个文件，全部在既有文件内追加）**

1. `mango-ui/packages/common/components/MangoDataTable/types.ts`
   - `MangoTableColumnType` 加 `'switch'`。
   - 不加新字段：`active-value` / `inactive-value` / `inline-prompt` / 文案全部走已有的 `column.props`（`el-switch` 原生 props 直通，与 `input` / `select` 的 `v-bind="column.props"` 完全同一写法）。
   - 不加新的 option/类型别名——`options` 对布尔列没有意义。

2. `mango-ui/packages/common/components/MangoDataTable/MangoTableCell.vue`
   - 在分支链里插一段（放在 `radio` 之后、`v-else` 兜底之前，保持既有顺序即简单到复杂）：
     ```
     <el-switch v-else-if="column.type === 'switch'"
       v-bind="column.props" :disabled="isDisabled"
       :model-value="rawValue"
       @update:model-value="handleValueUpdate"
       @change="commitChange" />
     ```
   - 新增一个 `switchPair` computed 承担本任务的核心语义——**取值类型保持**：
     - 先读 `column.props.activeValue / inactiveValue`（含 kebab 形式 `active-value`），显式配置优先；
     - 未配置时按 `rawValue` 的运行时类型派生：`typeof rawValue === 'number'` → `{ active: 1, inactive: 0 }`；`typeof rawValue === 'boolean'` → `{ active: true, inactive: false }`；
     - `rawValue` 为 `null` / `undefined` / 其他类型（含字符串 `'1'`）：不猜类型，判为**无效列配置**，在开发期 `dev` 环境用 `console.warn` 指明 `field` 与要求（必须显式给 `column.props` 的 active/inactive 或保证字段有值）。兜底渲染 `false` 态，不静默写错类型、不 catch 吞掉。
   - 切换写入时 `handleValueUpdate` 收到的是 `el-switch` 的 `active-value` / `inactive-value`，即上面派生的 `1|0` 或 `true|false`，经既有 `setByPath` 写回行对象，`commitChange` 携带 `previousValue` 发 `cell-change`。**写入值与原始字段类型一致，全链路只有一种真值表示，不做 `Boolean()` 归一化**（那会把 `0/1` 列变成 `false/true`，正是需求要避免的）。
   - 边界：不做乐观更新回滚（既有 `input`/`select`/`radio` 也没有，`cell-change` 由消费者决定请求失败后如何回滚，加回滚属超出需求）。

3. `mango-ui/packages/common/components/MangoDataTable/README.md`
   - 同步第 37 行的 `type` 枚举清单（该 README 有全量列类型注释约定，第 200 行附近的 `types.ts` 摘录段也要对齐）。

   `packages/common/index.ts` 无需改动（`MangoTableColumnType` 已导出，字面量扩充自动生效）。

**当前消费者（必须有，否则构成 OVERDESIGN-060）**

现状：`MangoDataTable` 的生产消费者只有 `packages/detail/src/MangoCollapseDetailPage.vue`；全仓有 **111 处手写 `<el-switch>`**，其中确实嵌在表格单元格里的真实场景包括：
- `packages/rbac/src/views/app/index.vue:266`（`row.status`，`active-value=1 / inactive-value=0`）
- `packages/system/src/views/public-path/index.vue:46`、`:84`
- `packages/payment/src/views/methods/PaymentMethodRoutePanel.vue:149`

本任务的落地方式我建议二选一，请你定：
- **A（推荐）**：本次只交付 `MangoDataTable` 的 `switch` 能力 + 单测，并在 PR 里明确“迁移清单”，把上述页面迁到 `MangoDataTable` + `type: 'switch'` 作为同一 PR 的消费者改造（否则新增类型当下零生产消费者，Review 会按 `REDLINE-OVERDESIGN-060` 第一条打回）。
- **B**：本次只做能力，同时把 `MangoCollapseDetailPage` 中已有布尔列改为 `type: 'switch'` 作为真实消费者。

需要你确认走 A 还是 B（以及 A 里迁哪几个页面）——这一步不定，本任务在红线口径下不成立。

**验证入口**

- 单测（主入口）：`mango-ui/packages/common/components/MangoDataTable/__tests__/MangoTableCell.spec.ts`，`vitest`，追加 4 条用例，且必须断言可观察目标而非“渲染出来了”：
  1. `rawValue` 为 `1` 的列渲染为选中态，触发 `update:modelValue`/`change` 后 `row.field === 0`，且 `typeof` 仍为 `number`（不是 `false`）；
  2. `rawValue` 为 `true` 的列切到关闭后 `row.field === false`，`typeof` 仍为 `boolean`（不是 `0`）；
  3. `cell-change` 只发一次，`previousValue` 等于切换前的原值；
  4. `column.props` 显式给出 `activeValue/inactiveValue` 时以配置为准（覆盖派生的 `1/0`）。
- 静态门禁：`pnpm lint`、`pnpm package-consumer:typecheck`（`MangoTableColumnType` 是跨包导出类型，必须走这条消费者类型检查，不能只看本包 `tsc`）。
- 若采用 A 方案且迁移了真实页面：对应页面跑 `pnpm test:micro --grep` 的既有 E2E 用例（如 `rbac`/`system` 相关 spec），并做真实接口联调（切换后刷新列表，确认后端落库的仍是 0/1 或 boolean，未被前端改写类型）。

## 4. 简单任务问卷（REDLINE-QUEST-050）

不生成固定问卷、不生成术语表文档。理由：本任务没有引入新的公共概念体系，只是给已有的列类型枚举补一个字面量，词汇沿用既有 `radio` / `select` 同维度命名，无认知风险。按 REDLINE-QUEST-050 只需“按需记录术语决定”——我会在 PR 描述里用一句“为什么叫 `switch` 而不叫 `isSwitch`/`switchColumn`”带过，落进 PR 描述即可，不单独立文档。唯一需要产出决策记录的是 A/B 消费者方案（涉及迁移范围），按 `rules/14-decision-expert-review.md` 的三视角（实现/兼容/回退）写进 PR。

## 5. 过度设计（REDLINE-OVERDESIGN-060）

逐条自查：

- **无消费者抽象**：风险点在第 1 条。若 A/B 方案不定、只交付能力，`type: 'switch'` 在本次 PR 内零真实消费者 → 违规。必须绑定迁移页面或 `MangoCollapseDetailPage`。上面已列为阻塞项。
- **配置项无读取逻辑**：不新增任何配置项。`active-value` / `inactive-value` 走既有 `column.props` 直通，不做 Mango 层自己的 `switchConfig` / `switchProps` 包装。
- **单一实现接口 / 扩展点**：明确**不做** `MangoSwitch` 组件抽取。虽然全仓 111 处 `el-switch`，但绝大多数是表单场景（`file/settings`、`user` 等），抽象成通用开关组件需要处理 v-model 契约、尺寸、文案、样式 token，且目前没有已批准需求要求统一开关视觉；在表格单元格里只此一处使用 → 按本条判定为过度设计。
- **新增依赖**：0 个。`el-switch` 是已安装的 Element Plus 组件。
- **公共类型仅内部使用**：`MangoTableColumnType` 已在 `packages/common/index.ts:59` 对外导出，`switch` 是给业务包消费者用的公共字面量，不属此条。反向注意：**不要**新增只在本文件用的 `MangoTableSwitchValue` 之类内部类型别名——直接用既有的 `MangoTableOptionValue`（已含 `boolean | number | string`）表达即可。
- **多名称**：不得在代码、README、PR 中并行出现 `switch` / `toggle` / `boolean` / `是否`，统一 `switch`。
- **以“代码更整洁”为唯一理由新增抽象**：不适用，本次无新增抽象层。

---

**结论**：方案规模是「1 个联合类型字面量 + 1 个模板分支 + 1 个派生取值对的 computed + 4 条单测 + README 同步」。**当前唯一阻塞点是你选 A 还是 B（真实消费者落点）**，这一点不定，第 1 节和第 5 节对 `REDLINE-OVERDESIGN-060` 都不成立，我不会开工。请回复 A 或 B（A 的话请指定迁移页面）。

下一步：你确认后，我按 `rules/13-agent-text-output.md` 先给改动清单与风险，再实施并跑上述验证命令。
