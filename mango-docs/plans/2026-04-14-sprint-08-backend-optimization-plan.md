# Sprint 08: 后端架构优化与清理计划 (Backend Optimization Plan)

## 1. 背景与现状 (Context)

经过对 `mango` 后端项目（自底向上从 Layer 0 `mango-common` 到 Layer 2 `mango-platform`）的深度代码评审，发现当前架构（SPI + Starter 4层拆分）设计极具前瞻性，但在实际落地和历史重构中存在以下遗留问题：
- **概念转换不彻底**：`DAL` 概念向 `KV` 转换未闭环，导致部分核心模块（如 auth, security）中残留错误的包导入（`io.mango.dal.api.*`）。
- **包路径违例**：`mango-infra-kv` 模块的物理目录结构与 `package` 声明不一致（缺少 `kv` 层级）。
- **底层依赖泄漏**：`mango-admin-app` 作为组装层，存在基础设施底层组件的直接依赖（直接依赖 Redis/MyBatis-Plus）。

## 2. 优化计划 (Phased Plan)

### 阶段一：修复致命编译问题与包路径重构 (Critical Fixes)
**目标：消除 `DAL` 遗留概念，修复包声明与物理路径不一致导致的编译隐患。**

1. **修复 `mango-auth-core` 的旧包名依赖**
   - 涉及文件：`ReplayGuard.java`, `IdempotencyGuard.java`
   - 动作：将 `import io.mango.dal.api.IKvStore;` 替换为 `import io.mango.infra.kv.api.IKvStore;`。
2. **修复 `mango-infra-security-core` 的旧包名依赖**
   - 涉及文件：`JjwtTokenServiceImpl.java`, `JjwtTokenServiceImplBlacklistTest.java`
   - 动作：将 `import io.mango.dal.api.IKvStore;` 替换为 `import io.mango.infra.kv.api.IKvStore;`。
3. **对齐 `mango-infra-kv` 的物理路径与包声明**
   - 动作：将 `mango-infra-kv-api` 等模块中 `src/main/java/io/mango/infra/api/` 的文件移动至 `.../io/mango/infra/kv/api/`。同步修改 `io.mango.infra.starter` 为 `io.mango.infra.kv.starter`。
4. **统一 Configuration 命名**
   - 动作：将 `mango-infra-kv-starter` 中的 `DalStoreAutoConfiguration` 重命名为 `KvStoreAutoConfiguration`，`DalStoreProperties` 改为 `KvStoreProperties`。

### 阶段二：规范依赖注入与抽象 (Abstraction Enforcement)
**目标：防止基础设施层的具体实现泄漏到业务应用组装层。**

1. **清理 `mango-admin-app` 的底层依赖泄漏**
   - 动作：从 `mango-admin-app/pom.xml` 中移除 `spring-boot-starter-data-redis` 和 `mybatis-plus-boot-starter`。强制通过 `mango-infra-kv-starter` 和 `mango-infra-db-starter` 进行能力注入。
2. **审查 TestConfig 的 Redis 依赖**
   - 动作：将 `mango-captcha-starter` 测试类中的 `StringRedisTemplate` 依赖移除，改为使用 `MemoryKvStore` 进行 Mock 测试。

### 阶段三：项目架构演进与文档对齐 (Documentation & Evolution)
**目标：降低认知负担，为后续新模块开发建立“唯一真理”。**

1. **全局替换文档中的 `DAL` 术语**
   - 动作：修改 `CLAUDE.md`, `README.md`, `mango-architecture-design.md`，将所有的 `mango-infra-dal` 更新为 `mango-infra-kv`。

---

## 3. 代码 Review、审计与验收清单 (Audit & Acceptance Checklist)

开发在执行完本 Sprint 后，需对照以下清单进行审计和验收：

### 🎯 阶段一：包路径与导入审计
- [ ] **代码搜索审计**：全局搜索 `io.mango.dal.api`，确保匹配结果为 `0`。
- [ ] **物理路径审计**：检查 `mango-infra-kv` 所有子模块，确认 `src/main/java/io/mango/infra/kv/` 目录结构已正确建立，且所有 `.java` 文件都在此路径下。
- [ ] **类名审计**：全局搜索 `DalStoreAutoConfiguration`，确保已全部更名或替换为 `KvStoreAutoConfiguration`。

### 🛡️ 阶段二：依赖隔离审计
- [ ] **POM 审查**：检查 `mango-admin-app/pom.xml`，确认已不存在对 `spring-boot-starter-data-redis`、`mybatis-plus-boot-starter` 等具体底层中间件的直接依赖。
- [ ] **测试审查**：确保 `mango-captcha` 测试上下文不依赖真实的 Redis 组件。

### 📄 阶段三：文档一致性验收
- [ ] **文档搜索审计**：在 `*.md` 文件中搜索 `mango-infra-dal`（除历史记录/归档计划外），确保已全部替换为 `mango-infra-kv`。

### 🔧 最终质量验收 (Final Quality Gate)
- [ ] **全量编译通过**：在根目录下执行 `mvn clean compile` 成功，无任何包找不到的报错。
- [ ] **单元测试通过**：在根目录下执行 `mvn test` 成功。
- [ ] **架构规范验证**：执行 `mvn mango:check` (如果可用) 确保没有违反新的分层约束。
