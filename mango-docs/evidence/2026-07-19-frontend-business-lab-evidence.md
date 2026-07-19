# 前端封闭业务开发环境交付证据（2026-07-19）

> 历史阶段证据：本文记录的是 28 包 PR-0F 检查点，已由
> `2026-07-19-frontend-production-candidate-evidence.md` 的 29 包最终候选证据取代。
> 后续判定必须使用新文档和其中绑定的提交、报告哈希，不得沿用本文的阶段状态。

## 1. 结论

- PR-0F 已建立可重复执行的 `Mango Business Lab`：使用本次源码打出的 28 个 npm tarball 和其中的 `@mango/cli` 生成 full preset 独立业务工程，没有复制 `mango-ui`，也没有把生成工程加入 Mango workspace。
- Linux/arm64、Node 22.23.1、pnpm 11.14.0 环境中，523 个依赖完成断网冻结安装，安装日志为 `reused 523 / downloaded 0`；DNS 与 HTTPS canary 均被网络层拒绝，成功外连数为 0。
- 生成工程的 format、ESLint、Stylelint、Vue typecheck、unit、production build 和聚合 `check` 全部通过；项目内 CLI 完成 workspace 初始化，Vite 最小 shell 返回 HTTP 200 后正常停止。
- PR-0F 的“业务开发环境基础”判定为 `READY`。它不证明业务 API/CSS/C4、真实后端、微前端、发布候选、灰度和回滚合同已完成；Mango 前端整体仍为 `NOT READY`，不得宣称生产毕业。

## 2. 固定环境与制品

| 项目                           | 实际值                                                                    |
| ------------------------------ | ------------------------------------------------------------------------- |
| 容器镜像                       | `mango/frontend-quality:node22-pnpm11.14`                                 |
| 镜像 identity                  | `sha256:2a04ce0242088af26fd0b147318842ae55ef09bcc2f917126347ff3ac0d2cf30` |
| Node / pnpm                    | `22.23.1` / `11.14.0`                                                     |
| 平台                           | `linux/arm64`                                                             |
| 业务工程                       | `.runtime/projects/frontend-standards-business-lab`                       |
| 外部依赖来源                   | `http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`              |
| Mango tarball                  | 28 个，全部带 SHA-256                                                     |
| 映射到本地 tarball 的 Mango 包 | 28 个                                                                     |
| 业务工程 lockfile SHA-256      | `e995d095039d8e0a6d921df2463e6ca122b5d75353b5d8022c7cf48907cddc34`        |
| preparation report SHA-256     | `2a2390a6ca08d2012eb80eb63e220ea37fb4f0b86153513bb7ad938ba84362b8`        |

准备阶段在与封闭阶段相同的 Linux 镜像中执行。macOS 只负责启动容器；`.runtime` 使用 Docker named volume，避免宿主 bind mount 的文件系统语义和平台可选依赖污染结论。准备报告记录的 Git SHA 为 `b3563d006e8d7a4aee038ba43683e1eb2c7aa9f4`，tarball 与 lockfile 哈希进一步绑定本次实际候选制品。

## 3. 封闭边界

封闭阶段使用 Docker `--network none`、`--cap-drop ALL`、`no-new-privileges` 和独立 HOME。HTTP/HTTPS/ALL proxy、npm user config 和凭证入口被清空；安装只允许读取准备阶段生成的离线 store。

| 断言                                            | 结果                |
| ----------------------------------------------- | ------------------- |
| 网络接口                                        | 仅 `lo`             |
| DNS canary                                      | 阻断，`EAI_AGAIN`   |
| HTTPS canary                                    | 阻断，`ENETUNREACH` |
| 成功外部连接                                    | 0                   |
| workspace/source 泄漏                           | 0                   |
| 主仓 `node_modules` 透传                        | 0                   |
| `workspace:` / `link:` / `portal:` / 源码 alias | 0                   |
| 宿主仓库绝对路径泄漏                            | 0                   |
| registry 凭证写入生成工程                       | 0                   |

runner 对生成工程的 package、lockfile、workspace、TypeScript 和 Vite 配置做文本边界检查，并对安装后的符号链接做真实路径检查。任何本地源码引用、越界链接、外部连接成功、canary 未执行或扫描输入为零都会失败。

## 4. 自动验证

| 命令                                       | 结果                           |    耗时 |
| ------------------------------------------ | ------------------------------ | ------: |
| `pnpm install --offline --frozen-lockfile` | 523/523 离线复用，退出码 0     |  2.07 s |
| `pnpm run format:check`                    | 退出码 0                       |  0.68 s |
| `pnpm run lint`                            | 0 warning，退出码 0            |  1.29 s |
| `pnpm run stylelint`                       | 0 warning，退出码 0            |  0.68 s |
| `pnpm run typecheck`                       | 0 diagnostics，退出码 0        |  1.51 s |
| `pnpm run test:unit`                       | 1 file / 2 tests 通过          |  0.77 s |
| `pnpm run build`                           | 2502 modules，退出码 0         | 32.61 s |
| `pnpm run check`                           | 七项质量合同再次全通过         | 36.81 s |
| `mango workspace init`                     | workspace 与隔离数据库坐标生成 |  0.39 s |
| `mango dev start`                          | 前端进程启动                   |  0.39 s |
| `mango dev stop`                           | 进程正常停止                   |  0.55 s |

运行时 workspace 为 `mango_001`，前端端口 `30001`，隔离数据库名 `mango_dev_frontend_standards_business_lab_001`。最小 shell `http://127.0.0.1:30001/` 返回 HTTP 200，响应体 831 bytes。

CLI 自身另完成 full/custom/add/module/PMO 场景和 19 个 Node test；模板验收新增“生成后的完整 frontend 必须通过自己的 Prettier 配置”断言。Business Lab runner/lib 的 5 个边界单测通过。

## 5. 本批修复的真实缺陷

第一次封闭运行暴露并修复了以下问题：

1. macOS 与 Linux 的 esbuild 可选二进制不兼容，准备阶段改为与封闭阶段使用同一固定镜像。
2. Docker Desktop bind mount 在 pnpm 大量文件复制时返回 `ESTALE`，实验室 `.runtime` 改用 named volume。
3. Git worktree 的 `.git` 文件指向宿主绝对路径，容器准备器增加严格校验的完整 Git SHA 注入。
4. CLI frontend 模板被仓库 `.prettierignore` 掩盖，生成工程有 9 个格式问题；模板已按目标配置格式化，运行时 JSON 缩进和实验室 workspace YAML 已修复，并增加生成后格式回归。
5. 源码边界扫描曾把普通文档中的 `link:` 文本误判为依赖泄漏，现只在依赖与构建配置文件中检查依赖协议，并保留反向 fixture。

这些失败均来自真实 cold/offline 路径，修复后从 pack 和生成阶段重新执行，没有复用失败产物冒充通过。

## 6. 仍然阻断生产毕业的事实

full preset production build 虽然退出码为 0，但最大 `mango-platform` JavaScript asset 为 5,649,796 bytes（约 5.65 MB，gzip 约 1.82 MB），另有约 0.94 MB 的 form designer、0.92 MB 的 Element Plus、0.85 MB 的聚合 CSS 和 0.81 MB 的 rich text asset。构建还报告多组 charts 与 Mango package 循环 manual chunk，以及 VueUse PURE annotation 警告。

依赖解析仍包含停止维护的 `vue-i18n@9.2.2`，并报告 `codemirror@6.65.7`、`glob@7.2.3`、`inflight@1.0.6`、`lodash.isequal@4.5.0` 等 deprecated dependency。主仓自身的 ESLint/Prettier/Stylelint/typecheck 存量基线也尚未清零。

PR-0F 没有实现或验证以下合同，因此不得扩张结论：

- 业务 package 的 `src/api`、service、composable、页面和私有/公共组件目录边界；
- API DTO/错误/取消/重试、Axios 实例归属和页面禁止直接请求；
- CSS ownership、Element Plus 覆盖边界、C3/C4 四轴毕业；
- Wujie 实例生命周期、单体/微前端同源构建、真实后端和浏览器业务 E2E；
- Nexus staging candidate 回读、独立部署、灰度流量、失败注入与回滚演练；
- 全量零静态债务、bundle budget 和依赖弃用清零。

## 7. 阶段判定

PR-0F Business Lab 基础：`READY`，可以在当前任务分支提交。

下一入口：按设计执行 Phase 1 的新增债务和公共契约门禁，随后由 PR-1J 在本实验室中验证真实业务模块的 API/CSS/C4/部署合同。只有 Phase 2 清债、Phase 3 零基线、真实 Nexus candidate、单体/微前端浏览器矩阵、灰度和回滚全部通过后，整体状态才能变更为 `PRODUCTION GRADUATED`。
