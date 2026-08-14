# Issue #791 Admin 发布矩阵兼容治理记录

## 1. 元数据

- Issue：[HardyDou/mango#791](https://github.com/HardyDou/mango/issues/791)
- 状态：IN_PROGRESS
- 交付模式：FULL（P0 启动/构建阻断且修改发布门禁自身，使用单份治理记录，不生成无关产品文档）
- 需求影响：L3，已发布 Admin 精确版本矩阵无法完成干净消费者生产构建。
- 方案风险：L3，涉及不可变 npm 坐标的替代版本、CLI 版本锁和发布前后消费者验证语义。
- 最终风险：L3
- 工作区决策：M01=REUSE；分支 `fix/issue-791-common-admin-shell-compat`，worktree `/Users/hardy/Work/mango-issue-791`。
- 启用措施：M01、M07、M08、M09、M10、M11、M14、M15。
- 不适用措施：M02（无数据库操作）、M12（无服务 API）、M13（菜单行为不变且由单元/生产构建覆盖）、M16（无人工视觉或破坏性动作）。

## 2. 目标、范围与非目标

目标：以新的不可变版本恢复 Admin 干净安装、类型检查和生产构建，并让发布前门禁真实组合“本地候选制品 + 私服未变制品”，发布后验证纯私服矩阵。

处理范围：

- 将父菜单高亮解析从 Common 公开子路径收回 Admin Shell 私有实现。
- Common 相关源码恢复到 `1.0.26` 已发布基线，不升级、不覆盖、不重发该坐标。
- 发布准备版本为 Admin Shell `1.0.60`、Admin `1.0.66`、CLI `1.0.107`。
- CLI release lock、Starter/模板版本、包 README、能力地图和发布说明同步。
- 消费者门禁区分私服已存在版本与尚未发布候选版本；404 才视为候选，错误版本、500 或连接失败均 fail closed。

非目标：

- 不修改 Mango Maven `1.0.36`、PMO `1.3.14`、Common `1.0.26` 或其它 npm 坐标。
- 不修改公开前端 API、菜单数据、页面 key、权限、租户、数据库和运行时配置。
- 不覆盖、删除、移动或重发 Admin Shell `1.0.59`、Admin `1.0.65`、CLI `1.0.106` 及其 Tag/Release。
- 不修改 Mango 仓库之外的代码或文档。

## 3. 根因与修复决定

Common `1.0.26` 于 2026-08-06 发布时不包含 `resolveActiveMenuPath`。后续菜单修复同时修改 Common 源码和 Admin Shell 导入，但没有升级包版本；Admin Shell `1.0.59` 发布时包含新导入，CLI 最终锁定了无法构建的 `1.0.59 + 1.0.26` 组合。

旧消费者门禁把所有工作区包重新构建并用本地 tarball 覆盖依赖，因此测试到的是“内容已经变化但仍名为 Common 1.0.26”的本地假矩阵。发布影响检查只看当前分支相对 main 的 diff，也无法发现此前已合并但未发布的依赖内容。

本次决定：

1. 菜单高亮算法只被 Admin Shell 的横向/纵向菜单消费，归属 Shell 私有模块；Common 回到已发布源码基线。
2. 发布前从消费私服查询每个精确坐标：存在的版本始终从私服安装，只有明确 404 的新版本才使用本地候选 tarball。
3. 私服查询的错误版本、500、认证或网络失败不得伪装成“尚未发布”。
4. CLI 已发布时，生成消费者也安装并执行私服 CLI；CLI 尚未发布时才执行本地候选 CLI。
5. CLI 作为依赖链最后一个制品，发布回查后再次运行同一门禁，此时所有坐标均从消费私服解析。

## 4. 验收映射

| ID | 场景 | 预期 | 自动化或证据 | 当前结果 |
|---|---|---|---|---|
| COMPAT-001 | 旧 `1.0.59 + 1.0.26` 纯私服矩阵 | Vite 明确报缺少 `resolveActiveMenuPath` | `package-consumer:typecheck --release-candidate-matrix --reuse-build` | PASS，已复现预期失败 |
| COMPAT-002 | 详情路由属于菜单项 | 父菜单保持高亮 | Admin Shell `activeMenuPath.spec.ts` | PASS |
| COMPAT-003 | 相似前缀但非子路由 | 不误选兄弟菜单 | Admin Shell `activeMenuPath.spec.ts` | PASS |
| COMPAT-004 | Common `1.0.26` 源码基线 | 两个菜单树文件与发布版本提交一致 | `git diff --exit-code e6596192... -- <files>` | PASS |
| GATE-001 | 私服精确版本存在 | 作为 registry dependency，不用本地覆盖 | matrix classifier 单测 | PASS |
| GATE-002 | 私服明确 404 | 作为本地候选 tarball | matrix classifier 单测 | PASS |
| GATE-003 | 私服 500/错误版本 | fail closed | matrix classifier 单测 | PASS |
| GATE-004 | 新候选混合私服未变包 | typecheck 和生产构建通过 | Node 22.23.1 执行发布前 mixed matrix：3 个候选 tarball + 26 个私服坐标；`vue-tsc`、2481 modules 生产构建通过 | PASS |
| GATE-005 | 三个新坐标全部发布后 | 纯私服安装、typecheck 和生产构建通过 | CLI 发布后 matrix | PENDING |
| REL-001 | 版本和依赖顺序 | Shell 1.0.60 -> Admin 1.0.66 -> CLI 1.0.107 | release impact、CLI lock、registry doctor | PENDING |
| REL-002 | PR/发布/回查 | required checks、双仓回查、Tag/Release、closeout 完整 | Mango release manifest | PENDING |

## 5. 发布矩阵

| 制品 | 当前 | 目标 | 顺序与说明 |
|---|---:|---:|---|
| Mango Maven | 1.0.36 | 不变 | 不发布 Maven |
| `@mango/common` | 1.0.26 | 不变 | 不发布；恢复源码基线 |
| `@mango/admin-shell` | 1.0.59 | 1.0.60 | 第一项，移除无效跨包导入 |
| `@mango/admin` | 1.0.65 | 1.0.66 | 第二项，精确依赖 Shell 1.0.60 |
| `@mango/cli` | 1.0.106 | 1.0.107 | 第三项，锁定完整修复矩阵 |
| `@mango/pmo` | 1.3.14 | 不变 | 不发布 PMO |
| 其它 npm 包 | 当前矩阵 | 不变 | 不发布 |
| Tag/Release | 无 | `v2026.08.14-admin-shell-1.0.60-admin-1.0.66-cli-1.0.107-compat-release` | 绑定合并后的 source commit |

## 6. M14 复核视角

| 视角 | 复核问题 | 当前结论 |
|---|---|---|
| 边界归属 | 算法是否只属于 Shell，Common 是否需要新公开 API | PASS；仅横向/纵向 Shell 菜单消费，不新增 Common API |
| 发布兼容 | 是否触碰旧制品或遗漏聚合/锁版本 | PASS；只准备三个新坐标，顺序为 Shell -> Admin -> CLI |
| 门禁真实性 | 是否仍可能由本地包覆盖未变私服依赖 | PASS；只为私服明确 404 的坐标创建 override，已存在版本从私服安装 |
| Fail-closed | registry 异常是否可能误判成候选版本 | PASS；仅识别明确 404，错误版本和其它失败直接终止 |

## 7. 回滚、剩余风险与收尾

- 发布前回滚：整体撤销 Shell 私有实现、门禁、版本和文档变更，不影响任何已发布制品。
- 发布后回滚：不覆盖新坐标；若仍有缺陷，继续使用新的补丁版本。旧错误矩阵保留为不可变审计历史。
- 剩余风险：发布前 mixed matrix 依赖消费私服可访问；网络或仓库错误会按设计阻断，而不会降级成本地全覆盖。
- 收尾条件：完整 gates、PR required checks、合并、release preflight、精确发布授权、registry doctor、状态机发布、双仓回查、纯私服矩阵、Tag/Release、Latest 文档和 closeout 全部完成。
