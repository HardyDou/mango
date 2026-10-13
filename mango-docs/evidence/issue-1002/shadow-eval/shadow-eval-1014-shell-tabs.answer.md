我已读取核心文件，现在按要求逐条输出方案：

## 1. 复用判断（REDLINE-REUSE-010）

**应复用的现有能力**：
- `runtimeHost.ts` 中已有 `restoreExternalTab`、`detachExternalTab`、`mountedTabs` 缓存机制
- `resolvePageType` 判断页面类型的逻辑
- `mountMicroMenu` 微应用挂载流程
- `isMountAllowed` 序列号验证机制

**不需要新增结构**：
- 不需要新增抽象、接口或配置
- 不需要新增依赖
- 修复范围限定在 `runtimeHost.ts` 的 `mountMenu` 函数内部逻辑调整

**根因定位**：
从代码第 149-155 行，`restoreExternalTab(tabKey)` 恢复节点后，第 151 行 `if (pageType === 'LOCAL_ROUTE')` 立即清空 `externalRoot`，导致已恢复的微应用 DOM 被错误清除。保护逻辑在第 155 行，但检查时机晚于清空操作。

**稳定复现条件**待确认：
- Workflow 与 RBAC 都配置为 `mode: 'micro'`
- 两者都开启 keepAlive
- 切换时 `resolvePageType` 计算是否存在不稳定性（依赖 `moduleConfig` 加载时机）

## 2. 术语（REDLINE-TERMS-030）

**不适用**。

本次修复不引入新的公共类型、API、配置、权限码、事件或概念名。仅调整现有 `mountMenu` 内部控制流程，复用现有术语 `pageType`、`keepAlive`、`mountedTabs`、`externalRoot`。

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界**：
- 文件：`mango-ui/packages/admin-shell/src/runtime/runtimeHost.ts`
- 函数：`mountMenu` 函数内第 145-160 行逻辑
- 调整：将 `externalRoot?.replaceChildren()` 的执行时机移至 `restoreExternalTab` 之前，或增加对已缓存微应用页签的保护判断

**当前消费者**：
- `ShellRuntimeOutlet.vue` 的 `renderCurrentRoute` → `mountShellMenu` → `mountMenu`
- `refreshTab` 强制刷新页签流程

**验证入口**：
1. E2E 测试：`apps/mango-admin-shell/e2e/specs/runtime-composition.spec.ts` 中补充"混合模式页签切换保留内容"用例
2. 手动验证：Workflow → RBAC → Workflow → RBAC 多次切换，检查 RBAC 按钮是否立即可见
3. 开发者工具：`runtimeDebug=1` 观察 `externalRoot` 子节点数量变化
4. 日志验证：`recordMicroAppDebug` 日志中 `before-unmount` 与 `mount` 顺序

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不适用**。

本任务为 Bug 修复，改动范围明确（单文件单函数），不涉及新增公共概念、API 协议或需要跨团队对齐的术语决定。无需生成问卷或术语表。

## 5. 过度设计（REDLINE-OVERDESIGN-060）

**无过度设计**：
- 不新增抽象、接口或配置项
- 不引入新依赖
- 不新增公共类型
- 修复方案直接调整现有控制流程分支顺序或条件判断

**实施方案**（三选一，按简单到复杂）：

**方案 A**（最小改动）：将第 151 行 `replaceChildren` 移至 `restoreExternalTab` 之前
```typescript
if (activeTabKey && activeTabKey !== tabKey) {
  await deactivateTab(activeTabKey);
}
activeTabKey = tabKey;
if (pageType === 'LOCAL_ROUTE') {
  externalRoot?.replaceChildren(); // 先清空
}
restoreExternalTab(tabKey); // 再恢复（只有 MICRO 才有节点）
```

**方案 B**（增强保护）：在 `restoreExternalTab` 前检查缓存类型
```typescript
const existingCache = mountedTabs.get(tabKey);
if (existingCache?.keepAlive && existingCache.externalNodes?.length && pageType === 'MICRO_ROUTE') {
  restoreExternalTab(tabKey);
  return; // keepAlive 微应用直接返回
}
if (pageType === 'LOCAL_ROUTE') {
  externalRoot?.replaceChildren();
}
```

**方案 C**（根因治理）：解决 `resolvePageType` 计算不稳定性（如果存在）
- 需要先确认 `moduleConfig` 加载时机是否影响判断结果
- 可能需要在 `mountedTabs` 中缓存首次挂载时的 `pageType`

推荐**方案 A**：调整清空与恢复的顺序，最小改动且逻辑清晰。
