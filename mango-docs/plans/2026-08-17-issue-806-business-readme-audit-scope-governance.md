# Issue #806 业务消费仓 README 审计作用域治理记录

## 1. 元数据

- Issue：[HardyDou/mango#806](https://github.com/HardyDou/mango/issues/806)
- 状态：IN_PROGRESS
- 交付模式：FULL（PMO 核心门禁、规则和发布 baseline 修改自身，使用单份治理记录，不生成无关产品文档）
- 需求影响：L3，错误门禁会阻断所有启用 M08 的业务消费仓提交，或诱导业务仓伪造 Mango 源仓目录。
- 方案风险：L3，改变源仓与业务仓 README 审计作用域、业务 baseline 投影和 PMO/CLI 可消费发布物。
- 最终风险：L3
- 工作区决策：M01=CREATE 后在任务 worktree 中 M01=REUSE；分支 `fix/issue-806-business-readme-audit-scope`，worktree `/Users/hardy/Work/mango-issue-806`。
- 启用措施：M01、M07、M08、M09、M10、M11、M14、M15。
- 不适用措施：M02（无数据库变更）、M12（无服务 API）、M13（无 UI）、M16（机器正反例和真实消费仓足以观察目标）。

## 2. 目标、范围与非目标

目标是在同一 PMO 工具中正确区分 Mango 源仓和业务消费仓，使两条 M08 README 审计既不要求业务仓包含 Mango 源码，又不会以空集合假通过。

处理范围：

- 共享解析脚本所在项目的根目录、仓库类型和业务 `mango.config.json.paths`。
- Mango 源仓继续审计固定平台 README、模块 README 和源码事实。
- 业务消费仓从 `<businessDocs>/capabilities/README.md` 解析明确引用且位于配置后端或前端目录内的本仓 README。
- 业务 source-facts 索引本仓 POM、前端依赖、Java/前端源码、配置和页面事实；外部 Mango 坐标继续由锁定版本与发布物验证。
- 同步规则、规则索引、PMO README、能力地图、正式测试、`@mango/pmo` Changeset 和 Business Starter 机械投影。
- PR 合并后发布机器计划解析出的 PMO/CLI patch tuple，并在真实业务消费仓升级验证。

非目标：

- 不在业务仓创建 `mango/`、`mango-ui/` 或 `mango-business-starter/` 目录。
- 不修改业务仓锁定 baseline 绕过检查。
- 不把所有业务目录下任意 README 强制解释为能力说明；业务能力地图是公开能力资产的唯一索引入口。
- 不在本 Issue 重写业务项目的历史 README，也不处理 Nexus 同版本覆盖；后者统一记录在 #807。
- 不改变业务 API、数据库、菜单、权限、租户、页面或应用运行时。

## 3. 根因与失败链路

两条脚本都使用脚本文件位置向上两级推断仓库根。在 Mango 源仓中，`mango-pmo/tools` 向上两级碰巧得到仓库根；在业务项目的 `business-pmo/mango-baseline/tools` 中，同一算法只得到 `<business-repo>/business-pmo`。

因此形成两个不同的错误：

1. `audit-module-readmes.mjs` 无条件加入 11 个 Mango 源仓固定 README，在合法业务仓固定失败。
2. `audit-readme-source-facts.mjs` 只扫描 `mango/**`、`mango-ui/**` 和 `mango-business-starter/**`，业务仓中集合为空后退出 0，形成假通过。

只对不存在文件执行跳过会保留第二个错误，也会让 Mango 源仓删除固定 README 后静默通过，因此不采用。

## 4. 方案决定

### 4.1 共享项目作用域

`tools/lib/readme-audit-scope.mjs` 是两条审计的唯一根目录和仓库类型解析入口：

- 优先从脚本所在目录向上识别完整 Mango 源仓或带锁定 baseline 的业务消费仓。
- `--project-root` 只作为可验证的显式入口；目标不是两类合法仓库时失败。
- 业务路径必须是项目内相对目录，绝对路径和 `../` 越界失败。

### 4.2 Mango 源仓保持严格

- 固定顶层 README 继续无条件进入 module audit；文件缺失报告 `README file`。
- 平台、基础设施和前端 package README 继续按现有完整结构、链接、脚本和源码注册规则审计。
- source-facts 继续扫描 Mango 源码、POM、package 和能力地图。

### 4.3 业务消费仓只审计仓库自有事实

- 能力地图固定使用 `<businessDocs>/capabilities/README.md`。
- module audit 只接收能力地图明确链接、真实存在并位于配置后端或前端目录内的 README；没有任何合法链接时失败。
- 业务 README 保留非空、占位符、命令、package script 和本地链接检查，不强制业务文档复制 Mango 源仓固定标题结构。
- source-facts 同时审计能力地图与其链接 README，并以业务 POM、package 依赖、配置和源码为事实索引。
- 外部 Mango Maven/npm 坐标不属于业务仓源码所有权；其版本、内容和完整性由 PMO/CLI lock、release manifest 和消费仓验证负责。

## 5. 验收映射

| ID | 场景 | 预期 | 自动化或真实证据 | 当前结果 |
|---|---|---|---|---|
| RAS-001 | packaged `business-pmo/mango-baseline/tools` 布局 | 自动识别业务项目根并审计配置资产 | `readme-audit-scope.test.mjs` 正例 | PASS |
| RAS-002 | 业务能力地图没有本仓 README | 明确失败，不允许空集合 | empty-scope 反例 | PASS |
| RAS-003 | 业务 README 声明不存在的 HTTP 入口 | source-facts 报精确 API | missing endpoint 反例 | PASS |
| RAS-004 | 显式错误项目根 | fail closed | wrong-root 反例 | PASS |
| RAS-005 | Mango 源仓固定 README 被删除 | module audit 仍失败 | missing source README 反例 | PASS |
| RAS-006 | `mango.config.json.paths` 越界 | 解析阶段失败 | path traversal 反例 | PASS |
| RAS-007 | 当前 Mango 源仓两条审计 | 全量平台 README 与源码事实通过 | 源码命令 | PASS |
| RAS-008 | 真实保函升级 worktree source-facts | 审计集合非空、无源仓目录误报 | 3 个业务资产实际回读 | PASS |
| RAS-009 | 真实保函升级 worktree module audit | 不再出现 11 个源仓 README | 当前仅发现 1 个业务 README 真实断链 | PASS（#806 误报消失） |
| RAS-010 | PMO package 与 Business Starter 投影 | 唯一源构建、manifest 和投影无漂移 | Node 22 `@mango/pmo` build/check、sync `--write`、sync `--check`；144 managed files | PASS |
| RAS-011 | 发布 PMO/CLI 与干净消费 | hosted/group 哈希一致，业务升级后正式命令可执行 | release manifest 与业务升级记录 | PENDING |

## 6. 发布与兼容

- 直接变化的发布包只有 `@mango/pmo`，使用 patch Changeset。
- 精确依赖 PMO 的 CLI 是否联动及目标版本由 `mango release plan` 和固定依赖图解析，不手写包清单。
- Mango Maven 和运行时前端包没有源码变化，不因本 Issue 手工加入发布批次。
- 业务升级不涉及服务停机、数据库 migration 或运行时配置；只升级 PMO/CLI baseline 并重新执行 M08 与本地 Gate。

## 7. M14 复核视角

| 视角 | 阻断问题 | 当前结论 |
|---|---|---|
| 作用域与所有权 | 是否仍从安装目录推断仓库根，或把外部 Mango 源码当业务仓所有 | 无；共享解析器区分两类仓库，业务只审计配置目录与能力地图声明资产 |
| Fail-closed | 错误根、空集合、路径越界、源仓 README 缺失是否可能静默通过 | 无；四类反例均在任何发布动作前失败 |
| 业务兼容 | 是否要求业务仓改目录、复制源仓 README 或接受未知外部坐标 | 无；业务目录来自现有配置，外部坐标仍由锁定版本和发布验证负责 |
| 投影一致性 | 是否手改 dist/starter，或遗漏 helper、规则和测试 | 待最终 package manifest、Starter `--check` 和 tarball 回读确认 |

## 8. 回滚与剩余风险

- 发布前回滚：整体撤回共享解析器、两条脚本、规则、文档、测试、Changeset 和机械投影。
- 发布后不覆盖已发布坐标；发现缺陷时发布新的 PMO/CLI patch。
- 真实保函仓仍有一个能力地图已引用 README 的断链，属于业务文档真实问题，不是 #806 的源仓误报；待业务仓升级新 tuple 后在原业务任务 worktree 修复并重跑 Gate。
- Nexus 已发布坐标不可变性由 #807 统一治理；本 Issue 发布前仍必须执行版本不存在与双仓完整性检查。

## 9. 收尾清单

- [x] 根因复现、基线与 Issue 范围确认。
- [x] 共享作用域、两条审计和 6 个正反例实现。
- [x] 源仓与真实业务仓只读验证。
- [ ] PMO package、Starter 投影和本地 Runner 对等 Gate 全绿。
- [ ] 普通修复 PR 提交、required checks、Review 和合并。
- [ ] 机器 release plan、不可变发布、双仓回查与纯消费者验证。
- [ ] 业务仓升级新 tuple、修复真实断链、重跑 M08/Gate 并继续原 PR。
