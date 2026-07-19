# 前端 CI 与业务边界门禁交付证据（2026-07-19）

## 1. 阶段结论

- PR-1B/1G/1H/1I 的基础门禁已在提交 `dc19e287d` 落地：稳定聚合工作流、affected/full 选择器、业务 API 分层、CSS 所有权和微前端厂商隔离均有机器检查与反向 fixture。
- 工作流 `Frontend Quality / frontend-quality` 不使用 path filter；范围未知、空 diff、共享配置、lockfile、CI、前端 PMO 规则或 checker 变化一律升级 full。
- 当前 21 条存量边界违规已按精确 identity 登记，只允许持平或减少；新增文件和新增 identity 立即失败，不能用总数额度替换债务。
- 本批在固定 Node 22 环境通过静态、类型和测试验证，但 GitHub 工作流尚未推送运行，required check、30 日样本窗、浏览器、真实部署、灰度和回退均未完成，因此整体仍为 `NOT READY`。

## 2. 固定验证环境

| 项目         | 实际值                                                             |
| ------------ | ------------------------------------------------------------------ |
| 镜像         | `mango/frontend-quality:node22-pnpm11.14`                          |
| 镜像 SHA-256 | `2a04ce0242088af26fd0b147318842ae55ef09bcc2f917126347ff3ac0d2cf30` |
| 平台         | Linux arm64                                                        |
| Node         | 22.23.1                                                            |
| pnpm         | 11.14.0                                                            |
| 网络         | `--network none`                                                   |
| 安装         | Docker volume 内 frozen/offline；复用 753、下载 0                  |

Docker Desktop bind mount 曾在 pnpm 复制 `echarts` 时返回 `ESTALE/-116`。该次运行判为基础设施失败，不计入通过；最终改用 Docker volume 内源码副本和依赖 store，避免在 bind mount 上安装。

## 3. 边界事实

| 规则域        | 存量 identity | 新增策略                                                                    |
| ------------- | ------------: | --------------------------------------------------------------------------- |
| API           |             5 | 阻止 API 导入 UI 框架、直建底层传输、读环境、绝对 endpoint、数值化 `ApiId`  |
| CSS           |             1 | 页面样式必须 scoped/module；微应用实际使用带样式包时显式导入合法样式 export |
| 页面/组件分层 |             6 | 阻止 presentation 直接创建或导入请求传输                                    |
| 微前端厂商    |             9 | Wujie import/global 只能存在于 `@mango/app-runtime` adapter                 |
| 合计          |            21 | 精确 identity 只减不增                                                      |

现状中 Wujie 的 mount/unmount/preload 主实现已位于 `@mango/app-runtime`；四个微应用入口和公共 request 仍有直接 `$wujie` 访问，按存量登记，后续必须迁移为厂商无关 runtime provider。这里的 Wujie 是 Mango 当前微前端实现依赖，不是业务规范、CI 或审批能力的规则来源。

## 4. affected/full 合同

- 新增、删除、重命名、package exports、peer dependency、样式入口和动态注册变化均按 workspace owner 解析，并沿反向依赖图选择全部传递消费者。
- `.github/workflows/**`、`mango-ui` 根配置、lockfile、架构/边界基线、质量脚本和 PMO 前端规则变化强制 full。
- 已分类的纯后端或文档变化可返回 `none`，但聚合 job 仍出现并执行工具链版本检查；未知仓库路径 fail-closed 转 full。
- 报告写入 `.runtime/frontend-quality/affected.json`，包含 base/head/merge-base、变更路径、选择理由、workspace、发布包、实际 Node/pnpm、执行命令和最终状态。
- affected 模式始终执行全量静态债务棘轮，只缩小 workspace build/test；发布包变化仍执行 exports 和独立 consumer 合同。

## 5. 自动验证结果

| 验证                            | 结果                                                        |
| ------------------------------- | ----------------------------------------------------------- |
| affected selector fixture       | 5/5 通过                                                    |
| API/CSS/vendor boundary fixture | 4/4 通过                                                    |
| 全部质量脚本测试                | 49/49 通过                                                  |
| workspace 测试                  | 399/399 通过                                                |
| ESLint                          | 232 error / 903 warning，ratchet 通过、无新增               |
| Prettier                        | 584 个存量不一致文件，ratchet 通过、无新增                  |
| Stylelint                       | 935 error / 0 warning，ratchet 通过、无新增                 |
| TypeScript                      | 25 个失败 workspace / 784 diagnostics，ratchet 通过、无新增 |
| 架构图                          | 37 workspace，错误 0                                        |
| 组件登记                        | 18 registry、195 legacy、漏登 0                             |
| Admin 样式聚合                  | 18 个 package style export 通过                             |
| Admin module style              | 12 个 official module 通过                                  |
| Workspace layout                | 通过                                                        |
| Workflow YAML / Prettier        | 通过                                                        |

37 workspace production build 在同一批源代码上通过。构建仍报告 Sass `@import` 弃用、动态/静态 import 混用、PURE annotation 和多个超大 chunk；这些是生产阻断债务，不计为毕业通过。

## 6. 发布、灰度与回退状态

| 项目                       | 当前状态               | 毕业要求                                                 |
| -------------------------- | ---------------------- | -------------------------------------------------------- |
| 本地 tarball/Business Lab  | 已通过前序阶段         | 每个发布候选重跑                                         |
| GitHub aggregator          | 文件已就绪，未推送运行 | 非 draft PR 100% 出现，满足稳定样本窗                    |
| required check             | 未配置远端分支保护     | 单拥有者按机器规则启用，不增加人工会签                   |
| Nexus staging/hosted/group | 未授权写入             | candidate 与正式坐标分别回读                             |
| 单体/微前端浏览器          | 未完成                 | 真实后端、交互、样式、卸载和副作用验证                   |
| 灰度                       | 未执行                 | 按 deployment registry 定向流量/租户，观测错误和性能阈值 |
| 回退                       | 未演练                 | 恢复上一稳定 CI/config 和部署版本，full inventory 复验   |

本批回退边界是关闭 affected 优化并转 full，保留稳定 aggregator 身份和全部诊断基线；禁止通过 `continue-on-error`、抬高基线或移除失败证据恢复流水线。
