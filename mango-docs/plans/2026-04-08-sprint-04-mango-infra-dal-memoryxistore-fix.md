# Sprint 04: MemoryXivStore TTL=0 Bug 修复

- 起始日期：2026-04-08
- 状态：待执行
- 所属任务：T4
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## Bug 描述

`MemoryXivStore.put(key, value, expireSeconds)` 当 `expireSeconds = 0` 时：
- Entry 的 `expireTime = Instant.now().plusSeconds(0)` → `Instant.now()` → 立即过期
- 但 Entry 不会立即清理，而是等后台清理线程（默认 1 分钟间隔）才移除
- 结果：`exists()` 返回 `false`（已过期），但 `get()` 在 1 分钟内仍可能读到旧值（如果后台清理还没运行）

**期望行为**：`expireSeconds = 0` 时应该**立即清理**，不等后台线程。

---

## 当前代码

```java
@Override
public boolean put(String key, String value, long expireSeconds) {
    validateKey(key);
    KvEntry prev = map.putIfAbsent(key, new KvEntry(value, Instant.now().plusSeconds(expireSeconds)));
    return prev == null;
}
```

**问题**：`expireSeconds = 0` → `Instant.now().plusSeconds(0)` → `Instant.now()` → Entry 立即过期但仍留在内存。

---

## 修复方案

```java
@Override
public boolean put(String key, String value, long expireSeconds) {
    validateKey(key);
    if (expireSeconds <= 0) {
        // TTL=0 或负数：立即删除，不存储
        map.remove(key);
        return false;
    }
    KvEntry prev = map.putIfAbsent(key, new KvEntry(value, Instant.now().plusSeconds(expireSeconds)));
    return prev == null;
}
```

---

## 实施步骤

- [ ] 1. 修复 `MemoryXivStore.put()` 方法，TTL <= 0 时立即删除
- [ ] 2. 补充单元测试覆盖 TTL=0 场景
- [ ] 3. 确保 `exists()` 和 `get()` 在 Entry 过期后返回 false/null
- [ ] 4. 运行 `mvn clean verify`

---

## 参考

- T1 commit：`c0b7df41`（MemoryXivStore 所在模块）
- 现有测试：`mango-infra-dal/mango-infra-dal-core/src/test/java/io/mango/dal/core/MemoryXivStoreTest.java`
