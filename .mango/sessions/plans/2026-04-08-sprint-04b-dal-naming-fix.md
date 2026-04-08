# Sprint 04b: DAL 模块命名规范化

- 起始日期：2026-04-08
- 状态：待执行
- 问题：`XivStore` 命名无意义（Xiv 不是标准缩写），统一改为 `KvStore`

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
- **只改实现类名 + 包路径**
- **SPI 注入配置**中的类名同步更新
- **测试类**随实现类一起重命名

---

## 实施步骤

- [ ] 1. 重命名实现类：`XivStore` → `KvStore`
- [ ] 2. 重命名包路径：`.../xiv/` → `.../kv/`
- [ ] 3. 更新所有 import 引用
- [ ] 4. 更新 `@ConditionalOnMissingBean` 中的类名
- [ ] 5. 更新 `DalStoreTypeEnum` 注释
- [ ] 6. 重命名测试类
- [ ] 7. 更新 README 文档
- [ ] 8. 运行 `mvn clean verify` 确保编译通过

---

## 文件命名检查

```bash
# 执行前检查
grep -r "Xiv" mango/mango-infra/mango-infra-dal --include="*.java" -l

# 执行后检查（应为空）
grep -r "Xiv" mango/mango-infra/mango-infra-dal --include="*.java" -l
```
