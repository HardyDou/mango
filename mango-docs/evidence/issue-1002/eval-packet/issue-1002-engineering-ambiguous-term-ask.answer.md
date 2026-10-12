## 结论：`ASK` — 命名阻塞，不能由我替你定

依据 `mango-pmo/rules/03-ai-coding-redlines.md:60`：**术语冲突或主名称不确定时必须 `ASK`**。当前上下文标注 `canonicalTermUnknown: true`，并且这次改动是 `publicTypeChange: true`（公共类型），两个条件同时命中，属于必须回问而不是自行裁决的情形。

### 为什么不能直接选

先看清三个候选其实不在同一层，把"复用 resource"和"复用 targetId"并列比较本身就是问题：

- `resource` 是**类型/概念名**。`mango-pmo/rules/backend/02-naming.md` §5 要求"同一业务概念在模块、包、类、表中使用同一领域词"，但正因为它是最泛的词，直接叫 `Resource` 会和已有的 Resource Registry / Resource Handler / ResourceSync 这套体系撞名，读者无法分辨你说的是注册表的资源声明，还是这个新公共类型。
- `targetId` 是**字段/引用名**。现有 `mango-docs` 中它的语义已经固定：资源 handler 写入目标表后返回的目标主键（见 `designs/2026-06-26-resource-identity-baseline-design.md`、`plans/2026-08-25-issue-835-...`）。这里要新增的是 **type**，用 `targetId` 命名一个类型属于跨语义层借用，`02-naming.md` §7 与 §5 都会判它不通过。
- 新名称：目前没有任何规范源能证明某个新词是 canonical。我不能凭偏好造一个领域词然后说它符合规范——这正是 `03-ai-coding-redlines.md` 要求 ASK 的场景。

### 需要你补充的阻塞点

请回答其中能回答的部分，我据此给出命名与落点方案：

1. **这个概念本身的语义是什么**——它是"被引用的目标对象"、"某种可寻址的外部实体"，还是别的东西？一句话定义。
2. **它的作用域**——公共 API / 领域类型 / 持久化实体 / 只是某个 command 的字段？后缀要按 `02-naming.md` §2 选（如 `XxxVO`、`XxxQuery`、`XxxEntity`）。
3. **是否已存在同义概念**——仓库里（含 `mango-docs` 设计文档）是否已有承担同一职责的术语？若有，指的是哪一处。
4. **谁消费它**——跨模块 API 还是模块内部？决定是否走 `api` 包以及是否需要 `XxxApi` 命名。
5. **是否与 Resource Registry 的既有 `resource` 语义重合**——重合则应是复用既有概念加限定词，而不是新增第三套命名；不重合则必须能用一个词把两者区分开。

补充后我会按 `03-ai-coding-redlines.md` 的复用优先与稳定术语原则，给出单一主名称 + 落地位置，并标注证据来源；若你给出的信息仍指向冲突，我会继续 ASK 而不是折中。

（本次为只读评估，未修改任何文件；上面只做了规则与现有术语的检索。）
