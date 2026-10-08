# Shell 登录前置与现行页面测试适配

## 1. 验收范围

结论：原先卡在 `.tenant-select` 的两项超时已在隔离库复现。测试按现行单机构自动选择、多机构显式选择规则修正后，最终定向浏览器测试 11/11 通过，类型检查及 22 项单元合同测试通过。此前完整 P0 关闭重试连续执行三轮，12/12 通过。

- 源码基线：`7769add9f36e9d4d4d593a7e5b0af56652774b4b`；结果来自未提交工作区。`results.json` 记录最终测试文件 SHA-256，不把基线 SHA 冒充新改动提交。
- 工作区：`/Users/hardy/Work/mango-shell-e2e-login`，分支 `fix/shell-e2e-login`。
- 产品事实源：`mango-ui/packages/auth/src/views/login.vue`、`useMangoLoginFlow.ts`、CMS `CmsResourceView.vue`、Shell `runtimeHost.ts`、Workflow 的当前列表页面。
- 修改对象：Shell 的三个业务测试共用登录前置；新增机构发现与登录结果合同回归；更新 CMS 列表、运行标记和业务表头定位；配置可变微应用地址及 CI 能上传的截图路径。
- 不修改产品登录、授权、机构、菜单、数据库或运行时实现；不删除 P0 用例，不增加超时，不用重试替代成功。
- 需求影响 L1、方案风险 L1、最终 L1 / SIMPLE：仅测试与验证说明，错误恢复是回退这些文件，不涉及产品契约或数据迁移。
- M01=CREATE；M02 不重建已有库，仅由 CLI 创建本任务的新库。M08、M09、M10、M13 启用；无产品四阶段文档、无发布动作。

## 2. 执行环境

- 日期：2026-10-08。
- Node 22.23.1、pnpm 11.14.0、Java 21.0.12.1、Playwright 1.61.1、Chrome / Chromium。
- 前端：Shell `http://a.mango.io:31001`；RBAC `http://b.mango.io:32001/`；Workflow `http://c.mango.io:33001/`；Template `http://d.mango.io:34001/`；CMS `http://e.mango.io:35001/`。
- 后端：`http://127.0.0.1:18001`，启动后 health=UP。
- 数据库：`mango_dev_mango_shell_e2e_login_001`，本任务新建隔离库；账号标识 `admin`，机构 ID `1` / code `default`。
- CMS 数据使用每次执行时间戳及 `E2E` 前缀，成功用例执行既有清理；失败尝试的残留只在本任务隔离库内，不作为业务数据。
- 所有服务通过仓库源码 Mango CLI 启动；微应用配置 `VITE_MANGO_ALLOWED_ORIGINS=http://a.mango.io:31001`；Shell 读取本工作区 `.runtime/playwright/mango-admin-shell/runtime-config.json`。

可复核入口（先按 CLI 初始化并启动对应工作区）：

```bash
pnpm -C mango-ui --filter mango-admin-shell test

PLAYWRIGHT_BASE_URL=http://a.mango.io:31001 \
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
PLAYWRIGHT_RBAC_ENTRY=http://b.mango.io:32001/ \
PLAYWRIGHT_WORKFLOW_ENTRY=http://c.mango.io:33001/ \
PLAYWRIGHT_TEMPLATE_ENTRY=http://d.mango.io:34001/ \
PLAYWRIGHT_CMS_ENTRY=http://e.mango.io:35001/ \
pnpm -C mango-ui test:micro --grep '@p0|@auth-contract' --retries=0 --trace retain-on-failure
```

三轮稳定性抽查使用同组地址，参数改为 `--grep @p0 --retries=0 --repeat-each=3 --trace retain-on-failure`。该抽查发生在最终类型收紧与格式整理前；整理后的完整 11 项再次通过。

## 3. 功能验收记录

以下 PASS 只表示列出的脚本断言通过，不代表平台全部功能、真实多机构授权或发布验收完成。

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | `/cms/sites`、内容分类/标签、栏目、内容、发布、导航、广告位/投放 | CMS 原有业务闭环 | 本次 E2E 时间戳资源 | 新增/编辑值回查、启停、草稿到审核再发布及下线、发布关系回查 | 新列表骨架内操作，搜索标签兼容冒号，弹窗表单、状态及详情字段断言 | 业务请求成功和数据断言保留；未另设全局 console 监听 | `cms.png`；`results.json` 的 final CMS 用例 | PASS |
| TASK-002 | TC-002 | 套餐/角色/菜单、流程发起/申请/已办、CMS | 混合模式远程装配和回切 | bootstrap 账号及菜单 | 预期模块/运行单元/MICRO_ROUTE、远程来源及页面关键字段 | 按角色、列标题和 `data-page` 定位，不依赖旧标题或外层 main | 远程来源按配置 origin 识别；全量请求错误清单未单独采集 | `hybrid.png`；`results.json` 的 final hybrid 用例 | PASS |
| TASK-003 | TC-003 | 同上页面的 monolith profile | 本地装配 | 同一隔离库 | 明确期望 LOCAL_ROUTE 与本地运行单元，远程资源集合为空 | 验证对应按钮、表头和搜索输入 | performance 来源断言覆盖配置的 RBAC/Workflow/CMS origin | `monolith.png`；`results.json` 的 final monolith 用例 | PASS |
| TASK-004 | TC-004 | 已登录 Shell → `/#/login` | 未授权事件处理 | 已建立的真实登录会话 | 事件总线必须存在，跳转登录且 token 清空 | 登录输入框重新显示 | 登录机构请求与返回字段经过共用前置校验；未采集全局 console 清单 | `results.json` 的 final unauthorized 用例；过程截图留在 runtime | PASS |
| TASK-005 | TC-005 | `/auth/login-institutions`、`/auth/login`、`/#/home` | 单机构真实登录 | admin/default，真实返回一项 | 下拉不显示，请求机构、响应机构、会话机构一致且存在 token | 精确输入框、登录按钮及首页承载区 | 真实请求状态及成功语义均校验，不输出响应 token | `single-institution.png`；`results.json` 的真实单机构用例 | PASS |
| TASK-006 | TC-006 | 真实 LoginView，受控机构发现响应 | 多选项前置分支 | 增加一个明确的合同测试选项，将真实 default 放在第二项 | 按机构编码定位选项，不依赖首项或名称；仍登录真实 default | 使用键盘展开选择器并点击对应编码选项 | 只替换机构发现响应；不代表额外机构实际获得授权 | `results.json` 的多机构受控选项用例；过程截图留在 runtime | PASS |
| TASK-007 | TC-007 | 真实 LoginView，受控错误响应 | 防止失败误通过 | 空列表、业务错误、HTTP 503、登录 HTTP 401、登录业务失败 | 明确拒绝；机构查询失败不提交登录；登录失败不建立 token | 保持登录页，不把隐藏下拉作为成功依据 | 401/503 和业务错误是声明的受控输入，不称后端验收 | `results.json` 的五项负向浏览器合同用例 | PASS |

## 4. 回归与静态验证

| 检查 | 结果 | 证据 |
|---|---|---|
| 修改前 P0 | 2 超时、2 未执行 | `.runtime/shell-e2e-login/before.log`；`results.json` before |
| 登录机构与结果单元合同 | 22/22，含缺失目标、重复 ID/编码、不完整数据、错误登录机构和强制改密 | `pnpm -C mango-ui --filter mango-admin-shell test`；`.runtime/shell-e2e-login/unit-final.log` |
| P0 三轮无重试抽查 | 12/12，0 skipped | `.runtime/shell-e2e-login/p0-repeat.log`；`results.json` p0Repeat |
| 最终 P0 + 登录浏览器合同 | 11/11，0 skipped，0 retry | `.runtime/shell-e2e-login/browser-final.log`；`results.json` final |
| TypeScript | PASS，已接入应用 test 脚本 | `tsconfig.e2e.json`；unit-final.log |
| 当前任务范围 ESLint、Prettier、test-quality、workspace-layout、Catalog、diff | PASS；目标 ESLint 使用 `apps/mango-admin-shell/e2e`、`tests` 和 Playwright 配置范围 | `.runtime/shell-e2e-login/lint-targeted-gate.log` 及当前任务 runtime 检查日志 |

### 复核选择

已询问是否需要外部同行评审；未指定，按 `AGENT_ONLY` 继续。以下均为同一 Agent 的分析视角，不是独立子 Agent、外部专家或人工审批。

| 视角 | 当前依据与决定 | 剩余边界 |
|---|---|---|
| 用户与事实 | 产品登录页已按机构数量决定是否显示选择器；测试应追随真实规则 | 不为旧测试恢复无条件下拉框 |
| 技术与风险 | 三处登录前置合并，按后端选项数量与目标编码处理，校验请求/响应/会话机构 | 不采用 `isVisible` 后直接跳过的弱断言；不改生产权限 |
| 验证与交付 | 修改前复现，22 项合同单测、7 项浏览器合同和 4 项原 P0；原业务断言继续执行 | 受控响应不冒充真实多机构授权；历史间歇失败单独追踪 |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 微应用回切时一次空白 | 定向调试中曾失败，之后同产品源码多轮通过；根因尚未定位 | 不能把测试适配当作运行时缺陷修复 | 已登记 [Issue #1014](https://github.com/HardyDou/mango/issues/1014)，保留回切断言与失败日志；后续复现需保存 trace | 未申请忽略失败或发布豁免 |
| 真实多机构授权闭环 | 多机构新增用例控制的是机构发现响应 | 只能证明前置脚本和选择器分支，不证明后台授权 | 产品授权任务使用真实机构/成员 fixture 单独验证 | 不声称本次已覆盖 |
| 全量 CMS 按钮、其它 P1/P2、生产构建、多浏览器 | 本任务范围是登录前置与已失败 P0；第三处登录调用已替换并通过类型检查 | 不等于所有长期浏览器套件通过 | 后续按相应任务运行完整套件 | 无自动豁免 |
| console/network 全局审计 | 本轮保留原业务请求断言与截图，未新增全局错误收集器 | 不宣称所有浏览器 console 或网络请求均无错误 | 完整 UI 或发布验收补独立事件清单 | 不扩大此次验收结论 |
| 初次本机微应用 CORS | 工作区端口与默认允许 origin 不同，已通过已有环境变量配置正确 origin | 属于本机验证环境准备，不改产品 CORS 策略 | README 写明配置；最终回归使用同一 origin | 非产品变更 |
| 全仓 `mango-ui lint` | ratchet 报告未修改存量文件的新诊断，涉及 `apps/mango-admin/e2e/specs/workflow-management.spec.ts`、`packages/admin-shell/src/**`、`packages/common/**`、`packages/file/**`、`packages/workflow/**`；当前任务文件的定向 ESLint 已通过 | 全仓门禁当前阻断，不能归因于本任务；不扩大范围修复存量基线 | 已保存 `.runtime/shell-e2e-login/mango-ui-lint.log`，后续单独维护静态质量基线并重跑全仓门禁 | 本任务不申请将该全仓结果视为通过，也不据此宣称发布可用 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 测试维护者 | 共用登录前置、当前页面定位、单位及浏览器合同、P0 结果 | Shell `e2e/`、`tests/` 与本目录 | 上述 test 与 test:micro 命令 | 仅本任务隔离库；不记录密码和 token | 保留真实业务断言；未知错误直接失败 | 已验证，尚未提交 |
| 发布维护者 | 此结果只证明测试修正，不是制品发布完成 | 本目录与 Issue #1014 | 独立 Mango 发布流程 | 不升版、不发制品、不创建 Tag | 发布版本、Changeset 和其它发布门禁另行完成 | 尚未发布 |
