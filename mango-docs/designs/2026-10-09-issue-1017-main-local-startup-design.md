# Issue #1017 Mango App 本地 main 启动设计

## 结论

本地无参数运行 Mango App 的 `main` 方法时，复用 `mango dev start` 的本地生命周期语义；生产环境显式 `bootstrap`/`runtime` 入口保持不变。

## 当前事实

- App 入口调用 `MangoApplication.run(primarySource, args)`。
- 当前入口要求第一个参数为 `bootstrap` 或 `runtime`。
- `mango dev start` 负责工作区数据库校验、generation 选择、cold bootstrap、回执读取和 runtime 参数注入。
- Persistence 在未配置 `mango.bootstrap.mode` 时支持兼容直启，但该路径不能满足本任务的 Bootstrap、Resource、generation 和 Runtime lease 合同。
- 现有工作区使用 `.mango/workspace.json` 和 `.mango/dev-workspace.env` 保存端口、数据库和 Maven revision。

## 目标

1. IDEA 直接运行任意 Mango App 的 `main` 方法时，不要求手工配置 Mango 生命周期参数。
2. 本地 main 启动复用当前工作区配置和已有数据库。
3. 已有数据库只执行必要的增量 migration，不执行 cold baseline。
4. 本地启动的 Bootstrap、Resource、fingerprint、receipt 和 Runtime lease 行为与 `mango dev start` 等价。
5. 单体、微服务由各 App 自身集成决定，不新增拓扑选择。
6. 显式 `bootstrap`/`runtime` 参数继续使用现有生产生命周期路径。

## 不处理

- 不改变生产 Bootstrap/Runtime 状态机和数据库表结构。
- 不把生产 Runtime 改成启动时执行 Flyway。
- 不把单体依赖拆成微服务依赖。
- 不新增前端启动能力。

## 方案

### 1. MangoApplication 双入口

- 参数首项为 `bootstrap` 或 `runtime`：沿用现有显式生命周期入口。
- 无参数或仅包含普通 Spring 参数：进入本地托管启动入口。
- 本地托管入口先启动一个非 Web 的 Bootstrap context，再关闭该 context，随后使用生成的本地 release tuple 启动 Runtime context。
- 本地 Bootstrap 使用 `apply + cold` 语义，已有 stable generation 且 fingerprint 未变化时复用现有 generation，不重复执行步骤。

### 2. 本地工作区配置

本地入口从当前进程环境优先读取配置；未显式提供时向当前目录及父目录查找 `.mango/dev-workspace.env`。工作区文件只补充缺失环境变量，不覆盖 IDEA 或 shell 显式配置。

本地入口将 `MANGO_DB_HOST`、`MANGO_DB_PORT`、`MANGO_DB_NAME`、`MANGO_DB_USERNAME`、`MANGO_DB_PASSWORD` 映射为 Spring datasource 配置，并保留 app `application.yml` 的其它配置。仅允许自动创建名称匹配 `mango_dev_*` 的本地数据库。

### 3. Generation 与 release tuple

本地 Bootstrap 在 Spring context 获得 `DataSource` 后计算当前 manifest fingerprint：

- stable fingerprint 相同：复用 stable generation，即使数据库中存在未完成 candidate；
- 空控制表：使用 generation 1；
- fingerprint 变化或没有可复用的 stable generation：使用当前最高 generation 加 1。

本地入口严格校验 datasource URL 指向当前 workspace 的 `mango_dev_*` 数据库。environment key、release id 和 revision 使用 workspace id 与 App 的 lifecycle key 形成稳定值；没有 lifecycle key 时回退到 Spring application name。CLI 通过 `MANGO_LOCAL_LIFECYCLE_KEY` 将 manifest 中的 lifecycle key 传给 Java，保证两种入口使用同一命名合同。

### 4. Runtime 启动

Bootstrap 完成并写入 receipt 后，本地入口把同一 environment、release、revision、generation 和 fingerprint 注入 Runtime context。Runtime 继续执行现有 receipt 校验和 lease 注册，不执行 Flyway 或 Bootstrap step。

## 失败边界

- 未初始化工作区、数据库不是 `mango_dev_*`、数据库连接失败或端口被占用时，启动失败并给出具体原因。
- 已有数据库 migration、资源声明或 manifest fingerprint 不一致时，先执行本地 Bootstrap；Bootstrap 失败则不启动 Runtime。
- 不允许通过本地启动入口自动操作非工作区数据库。
- 生产显式入口不使用本地自动配置和本地 generation 推导。

## 影响范围

- `mango-infra-bootstrap-starter`：本地启动编排、工作区配置和 release tuple。
- `mango-infra-bootstrap-core`：仅在需要时提供现有控制状态读取能力，不改变状态机。
- `mango-app`：App main 的本地启动行为。
- `mango-ui/packages/mango-cli`：与 Java 本地入口统一 environment key 和行为合同。
- README、测试和 Issue #1017 交付记录。

## 验证范围

- `MangoApplication` 显式 bootstrap/runtime 回归测试。
- 本地 main 无参数的 starter 集成测试，覆盖空库和已有 stable generation。
- CLI 本地生命周期 identity 和参数合同测试。
- 单体 App Maven 模块编译与定向测试。
- 真实 Mango 工作区启动验证：已有数据库、Flyway 增量、health 和 Runtime lease。

## 决策复核

- 事实与用户视角：本地 IDEA main 必须等价于 `mango dev start`，不能退化为普通直启。
- 技术与风险视角：本地快捷入口只能隐藏生命周期参数，不能绕过 Bootstrap/Runtime 校验；非工作区数据库必须 fail closed。
- 验证与交付视角：必须同时验证 Java main、CLI、已有数据库和空库路径。
- 评审选择：未指定外部同行评审，按 `AGENT_ONLY` 继续；Issue、设计、测试结果和 PR 保留复核事实。
