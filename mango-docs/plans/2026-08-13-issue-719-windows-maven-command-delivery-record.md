# 标准交付记录

任务：Issue #719 Windows Maven 命令解析修复。

## 1. 元数据

- 任务 ID：GitHub Issue #719
- 交付模式：STANDARD
- 需求影响：L2 - Windows 业务项目无法通过公开 CLI 启动 Spring Boot 后端、执行本地生命周期或完成空库验收
- 方案风险：L2 - 命令解析进入 doctor、install、bootstrap 和 runtime 的共享执行边界，必须保持跨平台参数与进程管理语义
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-719`，`fix/issue-719-windows-maven`）
- 启用能力：M01、M08、M09、M10、M11

## 2. 目标与范围

- 目标：让 Windows PowerShell 中安装到 `PATH` 的标准 Maven `mvn.cmd` 可被 Mango CLI 的全部开发生命周期入口一致执行。
- 成功条件：doctor、可选 install、managed bootstrap 和后台 runtime 使用同一平台命令解析；manifest 与 plan 继续显示跨平台命令名 `mvn`；macOS/Linux 的命令、参数和 Maven Reactor 行为不变。
- 处理范围：`@mango/cli` 平台命令解析、workspace/dev 执行入口、单元与 CLI 生命周期回归、CLI README 和能力地图。
- 不处理范围：发布流程命令 adapter、Windows Maven/Node 安装、业务仓依赖升版、Maven goal/参数、数据库 bootstrap 业务语义和 CLI 制品发布。

## 3. 可观察系统要求

| ID      | 参与者或入口                      | 输入或前置条件                         | 预期行为                                                     | 失败语义                                     | 验收标准                                                 |
| ------- | --------------------------------- | -------------------------------------- | ------------------------------------------------------------ | -------------------------------------------- | -------------------------------------------------------- |
| REQ-001 | Windows `mango dev doctor`        | `mvn.cmd` 位于 `PATH`                  | 通过 Windows shim 执行 `--version` 并报告 Maven 可用         | Maven 确实不可用时报告 `missing mvn`         | 平台解析测试覆盖 `mvn -> mvn.cmd` 与 PATH/PATHEXT lookup |
| REQ-002 | Windows bootstrap/install/runtime | `spring-boot-maven` app 或显式 install | 同步和后台命令均通过相同执行器解析 shim，原参数原样传递      | 命令找不到或退出非零时沿用现有日志与失败语义 | 源码入口审计与 CLI 生命周期测试通过                      |
| REQ-003 | macOS/Linux 开发入口              | 同一 manifest                          | 继续执行 `mvn`，不启用 shell，不改变参数、Reactor 或日志语义 | 现有失败语义不变                             | 平台单测与真实 macOS CLI 生命周期回归通过                |
| REQ-004 | 业务开发者                        | Windows PowerShell 与项目内 CLI        | 无需把 manifest 命令写成平台专有 `mvn.cmd`                   | PATH 缺失时有明确诊断步骤                    | README 与能力地图说明可发现                              |

## 4. 技术决定

| ID      | 对应要求                  | 接口/数据/权限/兼容性决定                                                                                                        | 影响路径                                      | 回滚方式                                                    |
| ------- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------------- |
| DEC-001 | REQ-001、REQ-002、REQ-003 | 在 CLI 包内建立平台命令解析器；只把已知 Windows shim 命令映射为 `.cmd`，其它命令保持原样                                         | `packages/mango-cli/src/platform-command.mjs` | 删除解析器并恢复各调用点直接 spawn                          |
| DEC-002 | REQ-001                   | CLI 直接按 Windows `PATH`/`PATHEXT` 检查 `mvn.cmd` 是否存在；Unix 检查 PATH 中的可执行位，不依赖外部 shell 查找工具              | CLI command lookup                            | 恢复 Unix-only lookup，但 Windows doctor/runtime 会再次失败 |
| DEC-003 | REQ-002、REQ-003          | doctor、业务模块命令、可选 install、managed lifecycle、runtime 和前端准备共用同步/异步执行包装；公开 plan/PID 信息保留逻辑命令名 | `packages/mango-cli/src/index.mjs`            | 将各入口恢复为直接 `spawn`/`spawnSync`                      |
| DEC-004 | REQ-004                   | README 说明自动解析和 PATH 排障，不要求平台专有 manifest                                                                         | CLI README、能力地图                          | 删除对应说明                                                |

## 5. 实施清单

| ID      | 对应决定         | 顺序 | 改动路径                            | 完成条件                                                      |
| ------- | ---------------- | ---: | ----------------------------------- | ------------------------------------------------------------- |
| IMP-001 | DEC-001、DEC-002 |    1 | 平台命令解析器与单元测试            | Windows/non-Windows 映射、PATH/PATHEXT 检查和参数传递均有断言 |
| IMP-002 | DEC-003          |    2 | CLI doctor 与所有开发执行入口       | 不再由入口直接执行逻辑 Maven/package-manager 命令             |
| IMP-003 | DEC-004          |    3 | CLI README、能力地图                | Windows 使用与排障入口可发现                                  |
| IMP-004 | 全部             |    4 | 定向测试、CLI 包回归与 PMO 必需检查 | M09/M10/M11 证据通过或明确记录环境限制                        |

## 6. 验收映射与结果

| 要求 ID                   | 验证方式             | 命令或步骤                                                                | 结果             | 证据                                                                                    |
| ------------------------- | -------------------- | ------------------------------------------------------------------------- | ---------------- | --------------------------------------------------------------------------------------- |
| REQ-001、REQ-002、REQ-003 | M10 平台解析单测     | `node --test mango-ui/packages/mango-cli/tests/platform-command.test.mjs` | PASS（7 tests）  | Windows/non-Windows 平台模拟与当前平台参数传递断言                                      |
| REQ-002、REQ-003          | M11 CLI 生命周期回归 | `pnpm -C mango-ui --filter @mango/cli test`                               | PASS（75 tests） | doctor、plan、真实 Maven Reactor bootstrap/runtime、后台进程与 packed consumer 全部通过 |
| REQ-003                   | M09 静态验证         | Prettier、ESLint、`git diff --check`、admin 样式检查                      | PASS             | 代码/文档格式、CLI ESLint、18 个 package style exports 与 12 个官方模块样式治理通过     |
| REQ-004                   | M08 能力说明         | README/能力地图审计、标准记录 checker                                     | PASS             | README 结构与源码事实审计、标准交付记录均通过                                           |

## 7. 例外与剩余风险

- 当前开发机为 macOS，无法声明真实 Windows PowerShell/Maven 进程验收通过；Windows 命令选择、PATH/PATHEXT 检查和 shell 合同由显式 `win32` 平台模拟测试覆盖，真实 Windows 消费验证保留为发布前验收项。
- 本任务不发布或升级 `@mango/cli`；业务项目只有在包含本修复的新 CLI 版本发布并升级项目内锁定依赖后才能获得修复。
- 全量 `mango-docs` 集合检查仍被 `origin/main` 已存在的 167 条历史生命周期文档错误阻断，包括旧 `pmoVersion`、缺少 frontmatter 和旧章节合同；本次新增标准记录的定向 checker 通过，未扩大范围修复这些历史资产。
- 当前默认 Node 为 `v26.5.0`，与仓库声明的 `>=22.23.1 <23` 不一致；全部定向与 CLI 回归均通过，但 Node 22 的 CI 结果仍是最终支持版本证据。
