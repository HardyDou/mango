# Issue #786 历史生命周期文档合同兼容治理记录

## 1. 元数据

- Issue：[HardyDou/mango#786](https://github.com/HardyDou/mango/issues/786)
- 状态：IN_PROGRESS
- 交付模式：FULL（PMO 文档合同、检查器和发布物修改自身，使用单份治理记录，不生成无关产品文档）
- 需求影响：L3，影响所有升级 PMO 后继续校验已审批历史生命周期文档的业务仓。
- 方案风险：L3，改变 PMO 合同解析边界、历史兼容条件、业务 baseline 投影和不可变 npm 发布物。
- 最终风险：L3
- 工作区决策：M01=CREATE 后在任务 worktree 中 M01=REUSE；分支 `fix/pmo-historical-document-contract-1.3.14`，worktree `/Users/hardy/Work/mango-pmo-historical-doc-contract`。
- 启用措施：M01、M08、M09、M10、M11、M14、M15。
- 不适用措施：M12（无服务 API）、M13（无 UI 或浏览器可见变化）、M16（无破坏性数据动作）。

## 2. 目标、范围与非目标

目标是在不改写已审批历史文档、不伪造 SHA-256、不恢复已撤回当前模板章节的前提下，使文档集合检查器按历史版本的真实合同校验三重锁定文档。

处理范围：

- `check-document-set.mjs` 向 validator 传递精确的历史 `pmoVersion`。
- TDD/Plan 合同声明 PMO 1.3.10、1.3.11、1.3.12 的精确历史章节变体。
- validator 将声明的历史章节插入到精确位置，并继续执行其余当前结构与语义检查。
- 同步 PMO 规则、索引、测试、README、能力地图、Business Starter 投影、PMO/CLI 版本锁与发布说明。
- 发布 `@mango/pmo@1.3.14` 和精确依赖它的 `@mango/cli@1.0.106`，Maven 保持 `1.0.36`。

非目标：

- 不修改保函项目 82 份生命周期文档或其审批证据。
- 不让任意历史版本跳过当前合同，也不按标题猜测历史合同。
- 不改变文档生命周期 schema revision、CLI 命令语法、Mango Maven 或运行时前后端能力。
- 不重新发布或移动 `@mango/cli@1.0.105`、既有 Tag 和 GitHub Release。

## 3. 根因与复现

`check-document-set.mjs` 已正确校验 `.mango-pmo-legacy-documents.json` 的相对路径、内容 SHA-256 和 `pmoVersion`。但 validator 过去只在 metadata 层放宽历史版本，结构校验仍无条件使用当前 PMO 1.3.13 的 H2 白名单。

真实发布 Tag 对比确认 PMO 1.3.10、1.3.11、1.3.12 的 TDD/Plan 相对 1.3.13 仅多出以下原合同章节：

```markdown
## 0. 参考资料与代码基线

| 类型 | 路径或模板ID | 版本或提交 | 用途 | 实际采用范围 |
```

在保函升级 worktree 执行当前 1.3.13 checker 时，82 份生命周期文档中 12 份 1.3.11 TDD/Plan 因 `TDD-META-001`、`PLAN-META-001` 和合同外 H2 失败。

## 4. 方案决定

### 4.1 精确版本输入

validator 不再接收宽泛的 `allowHistoricalPmoVersions=true`，而只接收已由集合检查器完成路径、SHA-256、版本三重锁验证的 `historicalPmoVersion`。metadata 中的历史版本也需要与该精确值相同。

### 4.2 合同拥有历史结构

TDD/Plan 合同通过 `historicalSectionVariants` 声明：

- 适用版本仅为 1.3.10、1.3.11、1.3.12。
- 插入位置是首个当前合同章节之前。
- 标题精确为“参考资料与代码基线”。
- 表头精确为“类型、路径或模板ID、版本或提交、用途、实际采用范围”。
- 至少存在一行数据。

当前模板不恢复该章节。PMO 1.3.13 作为 1.3.14 的普通历史版本加入版本白名单，但没有历史章节变体。

### 4.3 Fail-closed 边界

以下场景保持失败：

- 文档不在业务根目录历史基线中，或路径/hash/版本任一不匹配。
- 当前 1.3.14 文档携带历史章节。
- 历史文档出现未声明章节、重复标题、错误插入位置或错误表头。
- 历史章节变体配置命中多个版本定义、引用不存在的插入点或定义重复标题。
- 其它 metadata、表格、ID、审批、追踪、上游摘要、阻断项、依赖图或禁用内容校验失败。

## 5. 验收映射

| ID | 场景 | 预期 | 自动化或真实证据 | 当前结果 |
|---|---|---|---|---|
| HIST-001 | 三重锁定的 1.3.10/1.3.11/1.3.12 TDD/Plan 包含历史章节 | 按精确历史合同通过 | 三版本 document-contract 正例矩阵 | PASS |
| HIST-002 | 锁定历史文档增加未知 H2 | 合同外章节失败 | 未知章节反例 | PASS |
| HIST-003 | 相同历史内容未登记 hash 基线 | 不启用历史结构 | 未锁定反例 | PASS |
| HIST-004 | 当前版本文档使用历史章节 | 当前合同拒绝 | 当前版本反例 | PASS |
| HIST-005 | 历史表头变化 | 缺少预期表格且出现合同外表格 | 错误表头反例 | PASS |
| HIST-006 | 保函真实 82 份文档集合 | 不修改业务文档并全部通过 | `check-document-set.mjs --root .../docs` | PASS，82/82 |
| HIST-006A | 三重锁定的 1.3.13 文档不含历史章节 | 作为历史版本通过但不附加章节变体 | 1.3.13 no-variant 回归 | PASS |
| HIST-007 | PMO/CLI/Starter 投影 | 包、manifest、CLI lock 与 starter 无漂移 | package/CLI/sync gates | PENDING |
| HIST-008 | 私仓发布与干净消费 | hosted/group 均解析精确制品，真实保函升级通过 | Mango release manifest 与业务消费记录 | PENDING |

## 6. 发布矩阵

| 制品 | 当前 | 目标 | 顺序与说明 |
|---|---:|---:|---|
| Mango Maven | 1.0.36 | 不变 | 本任务不发布 Maven |
| `@mango/pmo` | 1.3.13 | 1.3.14 | 先发布并完成 hosted/group 回查 |
| `@mango/cli` | 1.0.105 | 1.0.106 | 精确依赖 PMO 1.3.14，后发布 |
| 其它 npm 包 | 当前矩阵 | 不变 | 不重新发布 Notice、Admin、File 或其它运行时包 |
| Tag/Release | 无 | `v2026.08.14-pmo-1.3.14-cli-1.0.106-historical-document-compat-release` | 绑定合并后的 source commit |

## 7. M14 复核视角

| 视角 | 复核问题 | 当前结论 |
|---|---|---|
| 历史合同精确性 | 变体是否来自真实 1.3.10/1.3.11/1.3.12 Tag，是否仅覆盖唯一差异 | PASS；唯一差异为 TDD/Plan 的“参考资料与代码基线”章节，三版本矩阵均通过 |
| Fail-closed 与安全边界 | 是否可能让未锁定、已变更、当前或未知结构文档绕过合同 | PASS；未锁定、当前版本、未知章节、错误表头反例及 PMO 全套门禁通过 |
| 升级与兼容 | 1.3.13 文档、新文档、CLI lock、Starter 投影是否一致 | PASS；1.3.13 no-variant、新 1.3.14 文档、29 包版本锁和 142 文件投影通过 |
| 回滚与不可变发布 | 发布失败后是否会重发已成功坐标，业务项目如何恢复 | PASS；发布只走 Mango release 状态机，1.0.105 只回填，业务升级事务保持原子回滚 |

## 8. 回滚与剩余风险

- 发布前回滚：整体回滚 validator、合同、规则、测试、版本和投影，不修改业务文档。
- 发布后回滚：不覆盖 1.3.14/1.0.106；若发现缺陷，以新的 PMO/CLI 版本修复。尚未升级的业务仓保留原版本，升级事务失败由 CLI 原子回滚。
- 1.0.105 已发布批次只回填真实完成状态，不触发重新发布或移动 Tag。
- 剩余风险是历史版本未来发现其它真实合同差异；处理时需要相同的 Tag 证据和精确合同声明，不能扩大为通用历史跳过。

## 9. 收尾清单

- [x] PMO/CLI/Starter 和全仓 gates 通过。
- [x] M14 四视角复核无阻断项。
- [ ] 合并最新 `main` 后重跑受影响验证。
- [ ] PR required checks 通过并合并。
- [ ] release preflight、branch protection、registry doctor 和版本不存在检查通过。
- [ ] Mango release 状态机发布、hosted/group 回查、干净消费和保函真实升级通过。
- [ ] 发布后 closeout PR 将新发布段从 `PENDING` 回填为真实状态。
