# 标准交付记录

任务：Issue #722 框架级 `crypto.randomUUID` 兼容修复

## 1. 元数据

- 任务 ID：Issue #722
- 交付模式：STANDARD
- 需求影响：L2 - 最新 Mango 发布矩阵在缺少 `crypto.randomUUID` 的旧浏览器、WebView 或受限运行环境中可能于管理端启动或 MSW 请求阶段异常，影响所有业务消费项目的公共运行时兼容性。
- 方案风险：L2 - 方案会扩展 `@mango/common` 公共 API、在 Admin Shell 启动阶段补齐 Web Crypto 方法，并调整 Admin 的开发 worker 与生产构建边界；不改变后端、数据、权限或租户合同。
- 最终风险：L2
- 工作区决策：CREATE - `fix/issue-722-crypto-random-uuid-compat`

## 2. 目标与范围

- 目标：由 Mango 框架统一兼容缺少 `crypto.randomUUID`、但具备 `crypto.getRandomValues` 的运行环境，业务应用只需升级匹配发布矩阵，不编写私有 polyfill。
- 成功条件：优先保留原生实现；缺失时生成 RFC 4122 v4 UUID 并幂等安装；没有安全 Web Crypto 时不伪造安全方法；MSW worker 可独立降级；生产包不携带或注册 mock worker；新增直接调用会被门禁阻断。
- 处理范围：`@mango/common` UUID/兼容 API、`@mango/admin-shell` 启动注入、Mango Admin 的 MSW worker 与生产构建、静态质量门禁、单测和 Chromium 回归、能力说明。
- 不处理范围：业务项目私有入口、后端 ID 生成、非 UUID 随机数兼容、第三方浏览器本身缺少全部 Web Crypto 的能力替代、版本发布和业务依赖升级执行。

## 3. 可观察系统要求

| ID     | 参与者或入口                        | 输入或前置条件                                           | 预期行为                                                             | 失败语义                                                    | 验收标准                                                      |
| ------ | ----------------------------------- | -------------------------------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------- |
| SR-001 | 使用 Mango Admin Shell 的业务管理端 | `crypto.randomUUID` 缺失且 `crypto.getRandomValues` 可用 | Shell 启动阶段自动安装兼容方法，返回 RFC 4122 v4 UUID                | 页面初始化或后续依赖调用抛出 `randomUUID is not a function` | 浏览器删除原生方法后仍能启动，方法存在且版本位/变体位正确     |
| SR-002 | 现代浏览器                          | 原生 `crypto.randomUUID` 可用                            | 保留原生函数，不覆盖、不重复安装                                     | 原生函数引用或行为被替换                                    | 单测证明安装前后函数引用相同                                  |
| SR-003 | 不具备安全 Web Crypto 的环境        | `crypto` 或 `getRandomValues` 缺失                       | 不安装基于 `Math.random` 的伪 Web Crypto 方法，并返回明确不可用状态  | 暴露看似安全、实际弱随机的 `crypto.randomUUID`              | 单测证明未写入方法且状态为 unavailable                        |
| SR-004 | Mango Admin 开发态 MSW worker       | worker scope 缺少 `randomUUID`，仍有 `getRandomValues`   | request ID 使用 worker 本地 RFC 4122 v4 fallback                     | 拦截请求时报错并中断                                        | Node worker-scope 回归验证原生与 fallback 两条路径            |
| SR-005 | Mango Admin 生产构建                | 执行 production build                                    | 不启动 MSW、不输出 `mockServiceWorker.js`，运行时继续注销历史 worker | 生产包携带或可启用 mock worker                              | 构建产物检查无 worker 文件，源码/产物证明注册只存在于开发分支 |
| SR-006 | Mango 前端维护者                    | 新增 `.randomUUID()` 直接调用                            | 静态门禁只允许公共兼容实现和受控 worker 资产                         | 绕过兼容 API再次引入调用点                                  | 门禁自身测试与全仓扫描均通过                                  |

## 4. 技术决定

| ID     | 对应要求       | 接口/数据/权限/兼容性决定                                                                                              | 影响路径                                                                                          | 回滚方式                           |
| ------ | -------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ---------------------------------- |
| TD-001 | SR-001~SR-003  | `@mango/common` 提供纯 RFC 4122 v4 生成器、优先原生的安全 UUID 入口和幂等 installer；fallback 只使用 `getRandomValues` | `mango-ui/packages/common`                                                                        | 删除新增 API，并回退 Shell 调用    |
| TD-002 | SR-001、SR-002 | Admin Shell 在配置和标准安装入口调用同一个幂等 installer，覆盖 CLI 生成入口与现有手工 Shell 入口                       | `mango-ui/packages/admin-shell`                                                                   | 删除两个启动调用点                 |
| TD-003 | SR-004         | 受控 MSW worker 内保留独立 request ID 生成器；worker 不能依赖应用 bundle，缺少安全随机源时明确失败                     | `mango-ui/apps/mango-admin/public/mockServiceWorker.js`                                           | 恢复上游 worker 并接受旧环境不兼容 |
| TD-004 | SR-005         | production 分支不动态加载 MSW；Vite 构建结束删除仅开发态 worker 资产，同时保留应用启动时的历史注册清理                 | `mango-ui/apps/mango-admin/src/mocks/browser.ts`、`src/main.ts`、`vite.config.ts`、`build-config` | 删除构建插件并恢复生产 mock 开关   |
| TD-005 | SR-006         | 新增直接调用扫描器，只豁免公共兼容实现和受控 worker；接入统一静态检查                                                  | `mango-ui/scripts/quality`、`mango-ui/package.json`                                               | 删除门禁脚本和命令接入             |
| TD-006 | SR-001~SR-006  | 不修改 API、持久化、权限、租户或后端合同；发布影响预计为 `common -> admin-shell -> admin -> CLI` 匹配矩阵              | README、能力地图、后续发布批次                                                                    | 本任务不执行发布，无制品或数据回滚 |

## 5. 实施清单

| ID     | 对应决定       | 顺序 | 改动路径                                                           | 完成条件                                       |
| ------ | -------------- | ---: | ------------------------------------------------------------------ | ---------------------------------------------- |
| IM-001 | TD-001         |    1 | `packages/common/utils/webCrypto.ts`、公开导出与单测               | 四类公共兼容行为有稳定测试                     |
| IM-002 | TD-002         |    2 | `packages/admin-shell/src/config.ts`、`src/appBootstrap.ts` 与测试 | 标准和手工 Shell 入口均不要求业务调用 polyfill |
| IM-003 | TD-003、TD-004 |    3 | Admin app worker、启动条件、生产构建清理插件                       | worker fallback 与生产隔离均可自动验证         |
| IM-004 | TD-005         |    4 | 质量脚本、脚本测试、根命令                                         | 全仓直接调用扫描通过且反例测试可阻断           |
| IM-005 | TD-006         |    5 | Common/Admin Shell README、能力地图                                | 业务升级方式、边界和发布影响清晰               |
| IM-006 | TD-001~TD-006  |    6 | 定向测试、构建、静态门禁、Chromium E2E                             | 验收结果和例外回填到本记录                     |

## 6. 验收映射与结果

| 要求 ID        | 验证方式         | 命令或步骤                                                                                                  | 结果 | 证据                                                                                                                                                                                                                                     |
| -------------- | ---------------- | ----------------------------------------------------------------------------------------------------------- | ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SR-001~SR-003  | M10 单元测试     | `pnpm --filter @mango/common test`；`pnpm --filter @mango/admin-shell test`                                 | PASS | Common 24 files / 300 tests；Admin Shell 12 files / 54 tests，失败 0                                                                                                                                                                     |
| SR-004、SR-006 | M10 质量脚本测试 | `node --test scripts/quality/random-uuid-compatibility.test.mjs`；`pnpm quality:gate:test`                  | PASS | UUID 专项 5/5；全量前端质量门禁 89/89；覆盖直接调用反例、worker 原生/fallback、生产资产移除和开发态注册边界                                                                                                                              |
| SR-005         | M11 构建集成     | `pnpm --filter @mango/admin-shell build`；`pnpm --filter mango-admin build`，并检查 `apps/mango-admin/dist` | PASS | Admin Shell 和 Admin production build 均 exit 0；`mockServiceWorker.js` 不存在；bundle 不含 `setupWorker`、`MOCKING_ENABLED` 或 `Mock Service Worker started`，并保留历史 worker 注销路径                                                |
| SR-001~SR-006  | M09 静态验证     | Common build、全仓 typecheck、UUID/架构/前端边界/E2E selector/两项 Admin 样式门禁、`git diff --check`       | PASS | Common build 与类型生成通过；全仓 typecheck、`random-uuid:check`、`architecture:check`、`frontend-boundaries:check`、`e2e-selectors:check`、`admin:styles:check`、`admin:module-styles:check`、diff whitespace 均 exit 0；质量脚本 89/89 |
| SR-001         | M13 UI/E2E       | `pnpm exec playwright test e2e/specs/crypto-random-uuid-compat.spec.ts --project chromium`                  | PASS | Chromium 1/1；页面脚本前移除 `Crypto.prototype.randomUUID` 后 Admin 正常启动，框架自动补齐且 UUID 版本位/变体位正确，无相关 page error                                                                                                   |

## 7. 例外与剩余风险

- 本任务不发布 npm 包或 CLI；业务项目获得修复仍依赖后续独立发布批次和匹配依赖升级。
- 完全缺少 `crypto.getRandomValues` 的环境无法安全实现 Web Crypto UUID；框架明确不安装弱随机伪实现，此类环境需升级浏览器/WebView 或部署到支持安全上下文的入口。
- 本任务不执行制品发布；后续发布需按 `common -> admin-shell -> admin -> CLI` 生成并验证匹配版本矩阵。
