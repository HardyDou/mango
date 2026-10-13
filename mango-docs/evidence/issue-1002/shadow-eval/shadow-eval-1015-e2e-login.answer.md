1. **复用判断（REDLINE-REUSE-010）**
   - 复用 Mango 现有 `@mango/auth` 的 `LoginView`、`useMangoLoginFlow`、`LoginTenantOption` 及 `/auth/login-institutions` 接口。
   - 复用 Admin Shell 现有三个 E2E 登录函数：
     - `mango-ui/apps/mango-admin-shell/e2e/specs/cms-management.spec.ts`
     - `mango-ui/apps/mango-admin-shell/e2e/specs/cms-operations-buttons.spec.ts`
     - `mango-ui/apps/mango-admin-shell/e2e/specs/runtime-composition.spec.ts`
   - 使用 Playwright 已有的 `waitForResponse`、`expect`、角色定位和请求体断言；不新增依赖或结构。
   - 单机构断言自动选中且不展示选择器；多机构按既有 `tenantCode` 显式选择，禁止继续按机构名称选择。

2. **术语（REDLINE-TERMS-030）**
   - 不引入新的公共类型、API、配置、权限码、事件、页面 key 或概念名。
   - 继续使用既有术语 `tenantCode`、`tenantId`、`LoginTenantOption`；不创造 `institutionCode` 等同义词。
   - 不涉及术语冲突，无需 `ASK`。

3. **最小实现（REDLINE-MINIMAL-020）**
   - **改动边界**：仅调整上述三个 Admin Shell E2E 登录辅助函数。读取并解析真实机构接口响应：
     - 只有一个机构：断言 `.tenant-select` 不存在，并直接登录；
     - 多个机构：断言选择器存在，按机构码定位并选择目标机构；
     - 监听 `/auth/login` 请求，校验提交的机构码/机构 ID 与预期选择一致。
   - **当前消费者**：三个 spec 文件中的既有 `login(page)` 函数及其调用用例。
   - **验证入口**：运行 Admin Shell Playwright 定向 E2E，并执行 `pnpm -C mango-ui e2e-selectors:check`。若现有真实 E2E 数据无法同时覆盖单机构和多机构场景，应先 `ASK` 补充真实测试数据，不使用硬编码或 mock 冒充验收。

4. **简单任务问卷（REDLINE-QUEST-050）**
   - 不适用。不生成固定问卷或额外术语表。

5. **过度设计（REDLINE-OVERDESIGN-060）**
   - 无无消费者抽象、单一实现接口、仅内部使用的公共类型、无读取逻辑的配置或新依赖。
   - 不修改认证生产代码，不新增 API、权限或事件；保持改动限于现有 E2E 消费者和真实登录链路校验。
