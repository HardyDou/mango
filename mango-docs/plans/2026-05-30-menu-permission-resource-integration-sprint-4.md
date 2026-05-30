# Mango Admin Runtime 产品化 Sprint 4 设计说明

## 1. 目标

Sprint 4 交付菜单、权限和资源自动集成能力，让 `@mango/admin` 在 full/custom preset 下默认复用后端菜单，同时可合并 capability 菜单和业务菜单，避免业务 starter 手写 Mango 内置菜单。

本阶段必须证明：

- full preset 继续显示原 Mango 完整菜单。
- custom preset 只补齐并注册所选能力及其依赖能力的菜单。
- 业务菜单可以追加到菜单树。
- 菜单冲突、权限过滤和来源报告可被程序化验证。
- E2E 必须保留截图、layout-report、菜单/页面抽查和人工截图识别结论。

## 2. 范围

### 2.1 本阶段范围

- 在 Admin Shell 增加菜单合并工具：后端菜单优先、capability 菜单补充、业务菜单追加。
- 在 Admin Shell 菜单配置中增加 `capabilityMenus`、`businessMenus`、`permissions`、`mergeStrategy`。
- `@mango/admin` 根据 preset resolution 自动把 capability menus 传给 Shell。
- 增加菜单合并报告，记录来源、冲突、隐藏项和过滤项。
- 扩展 E2E 抽查一级菜单和子页面。

### 2.2 非本阶段范围

- 不做 local/micro/mixed 运行时模式统一，该事项属于 Sprint 5。
- 不做 create-mango-app 初始化体验改造，该事项属于 Sprint 6。
- 不改变后端菜单接口契约。
- 不用前端静态菜单替代后端入库菜单。

## 3. 设计决策

### 3.1 菜单来源优先级

菜单来源优先级固定为：

1. 后端菜单：权威来源，保留原路径、排序、父子结构和权限。
2. capability 菜单：只补充后端缺失的 capability 页面菜单。
3. 业务菜单：追加业务项目自己的菜单，不允许覆盖 Mango 内置菜单。

### 3.2 冲突规则

- `menuCode` 相同：后端优先；capability/business 同 code 只补齐缺失字段，不覆盖后端结构。
- `path` 相同但 `menuCode` 不同：记录冲突诊断，保留已有菜单。
- 业务菜单与 Mango 内置菜单冲突：记录冲突诊断，不覆盖内置菜单。

### 3.3 权限过滤

- 如果传入 `permissions` 集合，菜单声明的权限均不命中时隐藏该菜单。
- 目录在子菜单全部被过滤后隐藏。
- 权限过滤只影响前端显示，后端接口仍必须做权限校验。

## 4. 测试策略

### 4.1 新特性测试

- Admin Shell 单测覆盖后端优先、capability 补充、业务追加、冲突报告、权限过滤。
- `@mango/admin` 单测覆盖 preset resolution 自动注入 capability menus。
- custom preset E2E 验证菜单抽查和解析报告。

### 4.2 回归测试

- `pnpm -F @mango/admin-shell test`
- `pnpm -F @mango/admin test`
- `pnpm package:check`
- `pnpm package:build`
- `pnpm admin:full-preset-e2e`
- `pnpm admin:custom-preset-e2e`

## 5. 完成标准

- starter 不需要声明 Mango 内置菜单。
- full/custom 菜单、页面、权限三者一致。
- full/custom E2E 均通过截图识别、layout-report、一级菜单和子页面抽查。
- Sprint 4 台账全部 DONE，delivery contract check 通过。
