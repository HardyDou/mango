# Issue #1128 系统字典 FLAT/TREE 交付记录

> 先读：本次为 Mango 系统字典公共能力增强。现有字典继续按 `FLAT` 语义工作；新增 `TREE` 字典用于维护父子关系和树形查询。旧 API、旧资源声明和旧字典数据不改变返回语义。
>
> 真实场景：业务模块需要维护保函要素等层级选项时，使用 Mango 的 `TREE` 字典；普通状态、类型和开关仍使用 `FLAT` 字典。
>
> 关键输入/输出：输入为字典类型 `shape`、字典项 `parentValue` 和资源声明字段；输出为兼容的平铺选项、显式树形选项和后台按字典形态切换的维护界面。
>
> 成功边界：旧 FLAT 字典和调用方保持原行为；TREE 字典可以创建、读取、维护和校验父子关系。失败边界：非法父节点、循环关系、删除有子节点节点和非法形态变更必须返回明确业务错误。
>
> 不处理：不把组织、行政区划、文件目录或带领域扩展属性的业务树迁移到系统字典；不改变旧 `/system/dict/data/options` 的平铺返回结构。

## 1. 元数据

- 任务 ID：ISSUE-1128
- 交付模式：STANDARD
- 需求影响：L2 - 影响系统字典公共 API、持久化数据和管理页面，但旧 FLAT 入口保留兼容语义。
- 方案风险：L2 - 涉及数据库增量 migration、Resource Registry、后端 API 和前端管理页面，范围限定在 system 能力。
- 最终风险：L2
- 工作区决策：CREATE 后复用 `/Users/hardy/Work/mango-issue-1128`
- 评审选择：AGENT_ONLY；未发起外部同行评审。

## 2. 目标与范围

- 目标：在同一套系统字典能力中支持 `FLAT` 和 `TREE` 两种字典形态，并保持向后兼容。
- 成功条件：
  - 未声明 `shape` 的旧字典默认为 `FLAT`。
  - 旧 `getOptions` 和 `/system/dict/data/options` 始终返回平铺选项。
  - TREE 字典可通过显式树形接口返回父子结构。
  - Resource Registry 可声明 `shape` 和 `parentValue`。
  - 后台 TREE 字典使用树形表格维护，FLAT 字典继续使用列表维护。
  - 非法父节点、循环引用和删除有子节点节点失败。
- 处理范围：
  - `mango-system` API、Entity、Service、Resource Handler、migration 和测试。
  - `@mango/system` 字典类型、数据 API 类型和管理页面。
  - `apps/mango-admin` 开发中心字典组件使用帮助示例和真实联调入口。
  - system README 和本标准交付记录。
- 不处理范围：
  - 不迁移现有组织、区域、文件目录、业务域等独立树模型。
  - 不新增独立树形字典菜单。
  - 不改变旧 FLAT 字典资源和旧消费组件的平铺行为。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 旧 API 消费者 | 现有 FLAT 字典、旧资源声明 | `getOptions` 返回原有 `label/value` 平铺结果 | 现有错误语义不变 | 后端回归测试通过 |
| AC-002 | Resource Registry | 资源未声明 `shape` | 默认同步为 `FLAT` | 非法 shape 拒绝同步 | Resource 集成测试通过 |
| AC-003 | Resource Registry | TREE 资源包含合法 `parentValue` | 同步父子关系 | 父节点不存在或循环时失败 | Resource 集成测试通过 |
| AC-004 | 业务 API 消费者 | TREE 字典编码 | 显式树形接口返回嵌套节点 | FLAT 字典请求树形接口返回明确失败或空树语义 | API 契约和服务测试通过 |
| AC-005 | 后台管理员 | 选择 FLAT 字典 | 继续看到列表维护 | 请求错误显示失败状态 | 前端类型和页面测试通过 |
| AC-006 | 后台管理员 | 选择 TREE 字典 | 看到树形表格，可新增根节点和子节点 | 自引用、循环父节点和有子节点删除被阻止 | 页面真实联调验收 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001/AC-002 | 新增 `DictShape`，旧字典和旧资源默认 `FLAT`；旧 `getOptions` 保持平铺返回 | system api/core/resource | 回滚应用版本时保留新增可空/默认字段 |
| TD-002 | AC-003 | `sys_dict_type.shape` 默认 `FLAT`；`sys_dict_data.parent_value` 可空；资源项使用 `parentValue` | system migration/entity/resource handler | 删除未使用 TREE 数据后回退应用；不修改历史 migration |
| TD-003 | AC-003/AC-004 | 新增显式 `getTreeOptions` 与 `/system/dict/data/tree`；不让旧 options 接口返回结构变形 | system api/controller/service | 删除新接口，保留旧接口 |
| TD-004 | AC-003 | 服务层校验同字典 value 唯一、父节点存在、禁止循环、删除父节点前检查子节点 | system core service | 业务校验代码可随版本回退 |
| TD-005 | AC-005/AC-006 | 当前字典管理页面按 `shape` 切换：FLAT 表格，TREE 树形表格；字典类型仍为列表 | `mango-ui/packages/system` | 前端回退到旧列表视图 |
| TD-006 | AC-001/AC-005 | `DictSelect` 等旧消费者继续消费平铺 options；新增树形消费者显式调用 tree API | system API and frontend package | 不修改旧调用方 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-001 | TD-001/TD-002 | 1 | system api/core/migration | 类型、字段、migration 和映射完成 |
| TASK-002 | TD-003/TD-004 | 2 | DictApi、Controller、DictService | 旧接口兼容，新接口和校验完成 |
| TASK-003 | TD-002/TD-003 | 3 | SystemDictResourceHandler、资源契约测试 | FLAT/TREE 资源同步完成 |
| TASK-004 | TD-005/TD-006 | 4 | `mango-ui/packages/system/src/api/dict.ts`、`views/dict/index.vue` | 后台按形态显示并维护真实数据 |
| TASK-005 | TD-001~TD-006 | 5 | 后端测试、前端测试、README | 回归和能力说明完成 |
| TASK-006 | AC-005/AC-006 | 6 | `apps/mango-admin` 开发中心字典组件示例、E2E | 能从开发中心打开示例并验证真实 FLAT/TREE 查询 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001 | 后端单元/集成测试 | `mvn -pl :mango-system-core -am -DskipITs test` | PASS | 71 tests, 0 failures；覆盖旧 options、默认 FLAT 和 TREE 嵌套查询 |
| AC-002 | Resource 集成测试 | 同上 `SystemDictResourceHandlerIntegrationTest` | PASS | 6 tests, 0 failures；未声明 shape 默认 FLAT |
| AC-003 | Resource 集成测试 | 同上 `SystemDictResourceHandlerIntegrationTest` | PASS | 覆盖 parentValue、重复 value、非法父子关系校验 |
| AC-004 | API/服务测试 | 同上 `DictServiceImplTest` 与后端编译 | PASS | 新增 `/system/dict/data/tree`，旧 `/options` 保持平铺 |
| AC-005 | 前端测试/构建 | `pnpm --filter @mango/system test && pnpm --filter @mango/system build` | PASS | 2 test files / 4 tests passed，Vite build passed |
| AC-006 | 数据库迁移验收；字典管理页面 | MySQL 临时库执行 V1/V2；浏览器走查 TREE 类型、根节点、子节点和删除保护 | PASS | migration 实测通过；浏览器实测新增 TREE 类型 → 新增根节点 → 行内新增子节点 → 编辑时父节点候选排除自身及后代 → 删除有子节点根节点被拒绝并提示「请先删除该节点下的子节点」；截图 `.runtime/issue-1128/verify-tree-manage.png` |
| AC-008 | 公共组件测试 | `pnpm --filter @mango/common test` | PASS | 38 files / 386 tests passed；新增 `DictSelect` 14 个用例和 `api/dict` 3 个缓存用例，覆盖 FLAT/TREE、路径回显、半选事件去重、checkStrictly、numberValue、reload、空/错/清空、节点路径读取与缓存失效 |
| AC-009 | 字典管理 E2E | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7777 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:5555 pnpm exec playwright test e2e/specs/dict-management.spec.ts --project=chromium` | PASS / SKIP | 平台管理员新增、编辑、删除字典类型和数据通过；第二机构权限用例在本环境因无演示机构且平台账号无机构创建权限而 skip，并输出原因 |
| AC-007 | 开发中心字典组件示例 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7777 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:5555 pnpm exec playwright test e2e/specs/dict-component-demo.spec.ts --project=chromium` | PASS | 真实登录、FLAT 选项、TREE 根/子节点查询通过；无 console/page error/request failure；截图：`.runtime/issue-1128/dict-component.png` |

## 7. 例外与剩余风险

- 已使用临时 MySQL 库实测执行 V1/V2，并验证 `shape=TREE`、`parent_value` 根/子节点落库；未对业务现有数据库执行 migration。
- 已启动真实后端与前端（mango CLI 后端 `18016`，前端 `7777` 代理至后端），完成开发中心示例与系统字典管理页面联调。
- 字典管理页删除受保护节点时前端会把业务校验错误输出到 `console.error`，属既有日志习惯，未改变用户可见提示。
- 是否允许选择非叶子节点暂按 TREE 节点均可维护、消费端自行选择的最小契约处理；如业务消费者要求叶子节点限制，需要另行扩展字典类型策略。
- 字典选项在 `@mango/common` 按编码共享缓存并合并并发请求；字典管理页保存后会自动清理对应缓存，已通过浏览器实测（管理页新增选项后示例页无需刷新即可见）。其它入口修改字典时需调用 `clearDictOptionCache` / `clearDictTreeCache` 或 `reload(true)`。
- 字典管理 E2E 的第二机构用例依赖演示租户 `company_a` 或机构创建权限；本环境两者均无，用例自动 skip 并标注原因，不影响平台字典维护闭环验证。
