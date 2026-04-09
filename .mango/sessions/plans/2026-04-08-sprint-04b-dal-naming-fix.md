# Sprint 04b: DAL 模块命名规范化

- 起始日期：2026-04-08
- 状态：待执行
- 范围：Sprint 04 (MemoryXivStore TTL=0 Bug) + Sprint 04b (XivStore→KvStore 命名) 合并执行
- 问题 1：`XivStore` 命名无意义（Xiv 不是标准缩写），统一改为 `KvStore`
- 问题 2：`MemoryXivStore.put()` 和 `DbXivStore.put()` 当 TTL=0 时行为错误（记录立即过期但不删除）

---

## 问题描述

当前实现类命名 `XivStore`（RedisXivStore / DbXivStore / MemoryXivStore）没有实际含义：
- `Xiv` 既不是英文缩写
- 与 `IKvStore` 接口命名不一致
- AI Agent 理解成本高

**应统一为**：`RedisKvStore` / `DbKvStore` / `MemoryKvStore`

---

## 需要重命名的文件/类

| 当前名称 | 目标名称 | 文件路径 |
|---------|---------|---------|
| `RedisXivStore` | `RedisKvStore` | `mango-infra-dal/mango-infra-dal-core/.../RedisXivStore.java` |
| `DbXivStore` | `DbKvStore` | `mango-infra-dal/mango-infra-dal-core/.../DbXivStore.java` |
| `MemoryXivStore` | `MemoryKvStore` | `mango-infra-dal/mango-infra-dal-core/.../MemoryXivStore.java` |
| `Xiv` 包路径 | `kv` 包路径 | `mango-infra-dal/.../xiv/` → `kv/` |

---

## 需同步更新的引用

全项目搜索 `XivStore` / `Xiv`：
- import 语句
- `@ConditionalOnMissingBean` 中的类名
- `@ConditionalOnProperty` 配置
- `DalStoreTypeEnum` 中的枚举值描述
- 测试类名
- README 文档

---

## 约束

- **不改接口名**：`IKvStore` 接口名保持不变（`K` = Key-Value，语义清晰）
- **TTL=0 语义**：所有实现类统一行为 — expireSeconds <= 0 时立即删除，不存储
- **RedisXivStore 不改**：已经 throw exception，行为可接受（Redis 语义约束）
- **SPI 注入配置**中的类名同步更新
- **测试类**随实现类一起重命名

---

## 实施步骤

- [x] 1. **TTL=0 Bug Fix — MemoryXivStore**
  - `MemoryXivStore.put()`: `expireSeconds <= 0` 时 `map.remove(key)` 并 return false
  - 补充单元测试覆盖 TTL=0 场景

- [x] 2. **TTL=0 Bug Fix — DbXivStore**
  - `DbXivStore.put()`: `expireSeconds <= 0` 时执行 DELETE（不 INSERT）并 return false
  - 补充单元测试：
    - `put_zeroTtl_shouldReturnFalseAndNotInsert()`
    - `put_negativeTtl_shouldReturnFalseAndDeleteOnly()`
    - `put_zeroTtl_existingKey_shouldReturnFalseAndDelete()`

- [x] 3. **重命名实现类**：`XivStore` → `KvStore`
  - `MemoryXivStore.java` → `MemoryKvStore.java`
  - `DbXivStore.java` → `DbKvStore.java`
  - `RedisXivStore.java` → `RedisKvStore.java`

- [x] 4. **重命名包路径**：无 xiv/ 子包（实现类直接在 io.mango.dal.core），无需操作

- [x] 5. **更新所有 import 引用**

- [x] 6. **更新 `@ConditionalOnMissingBean` 中的类名**

- [x] 7. **更新 `DalStoreProperties` 注释**（XivStore → KvStore）
  - **同步更新 `DalStoreTypeEnum`（遗漏项，测试发现）**

- [x] 8. **重命名测试类**
  - `MemoryXivStoreTest.java` → `MemoryKvStoreTest.java`
  - `DbXivStoreTest.java` → `DbKvStoreTest.java`
  - `RedisXivStoreTest.java` → `RedisKvStoreTest.java`
  - `DbXivStoreH2IntegrationTest.java` → `DbKvStoreH2IntegrationTest.java`

- [x] 9. **更新 README 文档**

- [x] 10. **运行 `mvn clean verify`** — BUILD SUCCESS (229 tests, 0 failures)

---

## 文件命名检查

```bash
# 执行前检查
grep -r "Xiv" mango/mango-infra/mango-infra-dal --include="*.java" -l

# 执行后检查（应为空）
grep -r "Xiv" mango/mango-infra/mango-infra-dal --include="*.java" -l
```

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 1 | CLEAR | 1 issue: DbXivStore TTL=0 bug omitted from original plan, added to scope |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | no UI scope |
| DX Review | `/plan-devex-review` | Developer experience gaps | 0 | — | no DX scope |

**CODEX:** N/A (auth failed, fell back to Claude subagent)

**CROSS-MODEL:** Claude subagent raised 3 issues: (1) package inconsistency with IKvStore — not valid (interface is in dal-api, not xiv package), (2) RedisXivStore inconsistent behavior — documented difference (Redis semantics require throw), (3) downstream consumers not checked — verified: only 13 files, all within dal module, no external consumers.

**UNRESOLVED:** 0

**VERDICT:** ENG CLEARED — ready to implement
