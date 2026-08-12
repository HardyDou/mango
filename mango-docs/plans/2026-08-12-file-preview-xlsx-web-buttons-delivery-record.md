# 标准交付记录

任务：保函系统 Issue #905 触发的 Mango XLS/XLSX Web 预览按钮能力优化。

## 1. 元数据

- 任务 ID：保函系统 Issue #905 / Mango 文件预览能力优化
- 交付模式：STANDARD
- 需求影响：L2 - 新增公开配置并改变 XLS/XLSX Web 预览页的可观察入口与布局
- 方案风险：L2 - 配置需要经过动态刷新、请求属性和 FreeMarker 模板协作，且必须保持其它预览类型不变
- 最终风险：L2
- 工作区决策：CREATE（`D:\Project\mango-file-preview-xlsx-buttons`，`codex/file-preview-xlsx-buttons`）
- 启用能力：M01、M08、M09、M10、M13

## 2. 目标与范围

- 目标：由 `mango-file-preview-engine` 正式控制 XLS/XLSX Web 预览页的“跳转 HTML 预览”和“打印”入口。
- 成功条件：开关关闭时两个按钮及按钮区域不渲染，Luckysheet 从顶部铺满；开关开启或缺失时保留现有行为；其它文件类型和文件能力不受影响。
- 处理范围：引擎配置、动态刷新、FreeMarker 请求属性、`officeweb.ftl`、模板/配置回归、模块 README、能力地图、业务接入说明和 changelog。
- 不处理范围：业务项目 CSS 或 iframe 注入、`@mango/file` 组件行为、PDF/图片/音视频/普通文档模板、下载与新窗口入口、数据库和权限合同。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | XLS/XLSX Web 预览 | `office.xlsx.web.buttons.enabled=false` | 不渲染两个按钮和按钮区域，Luckysheet 使用 `top: 0` | 任一入口残留或顶部保留空白视为失败 | FreeMarker 真实渲染测试通过 |
| REQ-002 | XLS/XLSX Web 预览 | 配置缺失或值为 `true` | 保留两个按钮、事件和 `20px` 顶部区域 | 当前兼容行为丢失视为失败 | 配置默认值和模板渲染测试通过 |
| REQ-003 | 其它文件预览与文件操作 | 任意开关值 | PDF、图片、视频、音频、普通文档、下载和新窗口行为不变 | 非目标模板或公开接口引用新开关视为范围泄漏 | 模板范围断言、模块测试和构建通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-002 | 新增 `office.xlsx.web.buttons.enabled=${KK_OFFICE_XLSX_WEB_BUTTONS_ENABLED:true}`；缺失时默认 `true` 保持兼容，不复用含义不同的 `office.preview.switch.disabled` | 引擎 properties、`ConfigConstants`、`ConfigRefreshComponent` | 删除新配置链路并恢复模板静态渲染 |
| DEC-002 | REQ-001、REQ-002 | 由 `AttributeSetFilter` 把布尔值注入 FreeMarker；模板关闭时不生成按钮 DOM，并把 Luckysheet `top` 改为 `0px` | `AttributeSetFilter.java`、`officeweb.ftl` | 恢复按钮区和固定 `top:20px` |
| DEC-003 | REQ-003 | 新开关只在 `officeweb.ftl` 消费，不修改其它模板、文件 API、权限、下载或前端组件 | 引擎模板与测试 | 删除范围断言后仍需人工确认所有非目标入口 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-001 | DEC-001 | 1 | `ConfigConstants.java`、`ConfigRefreshComponent.java`、主/测试 properties | 配置默认值、环境变量和动态刷新使用同一语义 |
| IMP-002 | DEC-002 | 2 | `AttributeSetFilter.java`、`officeweb.ftl` | 开关两种状态的 DOM 与布局符合要求 |
| IMP-003 | DEC-001、DEC-002、DEC-003 | 3 | `XlsxWebPreviewTemplateTest.java` | 关闭、开启、布局和非目标模板回归全部通过 |
| IMP-004 | 全部 | 4 | 模块 README、能力地图、业务指南、changelog、本记录 | 配置、默认值、边界和发布步骤可被消费者发现 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-002、REQ-003 | M10 模板与配置回归 | `mvn -f mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml '-Dtest=XlsxWebPreviewTemplateTest,XlsxWebPreviewConfigurationTest,PdfViewerCompatibilityTests' test` | PASS（12 项测试） | Maven Surefire 输出 |
| 全部（引擎模块） | M10 模块全量回归 | `mvn -f mango/mango-platform/mango-file-preview/mango-file-preview-engine/pom.xml test` | PASS（46 项通过，1 项既有 skip；0 failure，0 error） | Maven Surefire 输出 |
| REQ-001、REQ-002、REQ-003 | M09 模块构建 | `mvn -f mango/pom.xml -pl :mango-file-preview-engine -am '-DskipTests' package` | PASS（Reactor 8 模块，产出 engine JAR） | Maven Reactor 输出 |
| 全部 | M09/M08 质量与文档门禁 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/workspace-layout-check.mjs --root .`、`node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`、`node mango-pmo/tools/check-business-guides.mjs`、`node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`、`git diff --check`、`cd mango-docs; npm ci; npm run docs:build` | PASS；文档站构建成功（存在既有 chunk size warning；`npm ci` 报 2 moderate、3 high 依赖漏洞，未自动修复） | 对应命令输出 |
| REQ-001、REQ-002、REQ-003 | 完整 Reactor 测试（基线核验） | `mvn -f mango/pom.xml -pl :mango-file-preview-engine -am test` | 未通过：失败集中在上游 `mango-maven-plugin` Windows 路径断言（24 failures、4 errors），engine 模块未执行；属于基线/环境问题 | Maven Surefire 输出 |
| REQ-001、REQ-002 | M13 页面验证 | 尝试启动预览引擎并分别以开关关闭/开启渲染 XLSX Web 页面 | 未验证：当前工作区未具备可直接启动的预览服务与测试 XLSX 数据，未以静态检查冒充浏览器验证 | 环境限制与剩余风险 |

## 7. 例外与剩余风险

- `mango workspace init` 当前无法执行：本机 PowerShell 找不到 `mango` CLI；未影响独立 worktree 和 Maven 定向验证，最终交付需保留该环境限制。
- 当前引擎仅将 `.xlsx` 路由到 `officeweb.ftl`/Luckysheet；`.xls` 继续走既有 Office 转换预览路径。本次不改变格式路由，因此开关不会影响 `.xls` 转换结果，也不会把它强制切换到 Luckysheet。
- 不创建 Maven 或 npm 版本号。正式发布时使用仓库内 `mango-release` 流程，将本变更纳入下一次 Mango Maven 批次；`@mango/file` 源码未变化，不需要为本能力单独发布 npm 包。
