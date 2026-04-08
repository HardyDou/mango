# Sprint 05: mango-infra-crypto 新建

- 起始日期：2026-04-08
- 状态：⚠️ 已合并到 sprint-06
- 所属任务：T5（已合并到 T6）
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## 背景

`mango-common/crypto/` 目录下有框架耦合代码（依赖 BouncyCastle / Spring / `@Component`），需移入 `mango-infra-crypto` 新模块。

> 参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 1.6 框架分层原则 + Section 三 `mango-infra-crypto`

---

## 一、现有 crypto 目录分析

**目录**：`mango-common/src/main/java/io/mango/common/crypto/`

### 1.1 文件清单（16 个 Java 文件）

| 文件路径 | 类型 | 依赖 | 处理 |
|----------|------|------|------|
| `base/Base64Utils.java` | 工具类 | JDK 零依赖 | **留 common** |
| `support/SymmetricCipher.java` | 接口 | 零依赖 | **留 common** |
| `support/AsymmetricCipher.java` | 接口 | 零依赖 | **留 common** |
| `support/Signer.java` | 接口 | 零依赖 | **留 common** |
| `support/Digester.java` | 接口 | 零依赖 | **留 common** |
| `support/CryptoFactory.java` | `@Component` | `@Autowired Sm4Cipher/AesCipher` | **移入 infra-crypto-core** |
| `support/KeyManager.java` | `@Component` | `@Value` 配置注入 | **移入 infra-crypto-starter** |
| `symmetric/Sm4Cipher.java` | `@Component` | BouncyCastle + Spring | **移入 infra-crypto-core** |
| `symmetric/AesCipher.java` | `@Component` | Spring `@Value` | **移入 infra-crypto-core** |
| `asymmetric/Sm2Cipher.java` | `@Component` | BouncyCastle + Spring | **移入 infra-crypto-core** |
| `asymmetric/Sm2Signer.java` | `@Component` | BouncyCastle + Spring | **移入 infra-crypto-core** |
| `asymmetric/RsaCipher.java` | `@Component` | Spring `@Value` | **移入 infra-crypto-core** |
| `asymmetric/RsaSigner.java` | `@Component` | Spring `@Value` | **移入 infra-crypto-core** |
| `digest/Sm3Digester.java` | `@Component` | BouncyCastle + Spring | **移入 infra-crypto-core** |
| `digest/HmacSm3Digester.java` | `@Component` | BouncyCastle + Spring | **移入 infra-crypto-core** |
| `digest/Sha256Digester.java` | `@Component` | JDK 纯算法 | **移入 infra-crypto-core** |
| `digest/Sha512Digester.java` | `@Component` | JDK 纯算法 | **移入 infra-crypto-core** |

### 1.2 依赖分类

| 依赖类型 | 文件 | 决策 |
|----------|------|------|
| **零依赖（JDK only）** | Base64Utils, SymmetricCipher, AsymmetricCipher, Signer, Digester, Sha256Digester, Sha512Digester | 留 `mango-common` |
| **BouncyCastle** | Sm4Cipher, Sm2Cipher, Sm2Signer, Sm3Digester, HmacSm3Digester | 移入 `infra-crypto` |
| **Spring `@Value`** | AesCipher, RsaCipher, RsaSigner | 移入 `infra-crypto` |
| **Spring `@Autowired`** | CryptoFactory | 移入 `infra-crypto` |

> **Review 修正**：Sha256Digester / Sha512Digester 是纯 JDK `MessageDigest`，无 BC/Spring 依赖，按 Section 1.6 原则留 `mango-common`。Sm3Digester / HmacSm3Digester 依赖 BouncyCastle，移入 `infra-crypto-core`。

### 1.3 `mango-common/pom.xml` 变更

**移除依赖**（移入 `infra-crypto` 后 common 不再需要）：
```xml
<!-- 删除这两项 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-crypto</artifactId>
</dependency>
```

---

## 二、4 层结构

```
mango-infra-crypto/
├── pom.xml                                    ← 聚合父 pom
├── mango-infra-crypto-api/                   ← 接口定义（零依赖）
│   ├── pom.xml
│   └── src/main/java/io/mango/infra/crypto/api/
│       ├── ISymmetricCipher.java              ← 从 SymmetricCipher 重命名
│       ├── IAsymmetricCipher.java            ← 从 AsymmetricCipher 重命名
│       ├── ISigner.java                      ← 从 Signer 移入
│       ├── IDigester.java                    ← 从 Digester 移入
│       └── ICryptoFactory.java               ← 新建接口
│
├── mango-infra-crypto-core/                  ← 核心实现（含 @Component）
│   ├── pom.xml
│   └── src/main/java/io/mango/infra/crypto/core/
│       ├── symmetric/
│       │   ├── Sm4Cipher.java                ← 从 common 移入（改包名）
│       │   └── AesCipher.java                ← 从 common 移入（改包名）
│       ├── asymmetric/
│       │   ├── Sm2Cipher.java                ← 从 common 移入（改包名）
│       │   ├── Sm2Signer.java                ← 从 common 移入（改包名）
│       │   ├── RsaCipher.java                ← 从 common 移入（改包名）
│       │   └── RsaSigner.java                ← 从 common 移入（改包名）
│       ├── digest/
│       │   ├── Sm3Digester.java              ← 从 common 移入（改包名）
│       │   └── HmacSm3Digester.java          ← 从 common 移入（改包名）
│       ├── CryptoFactoryImpl.java            ← 实现 ICryptoFactory（移入并改包名）
│       └── CryptoKeyUtils.java               ← 新建 — KeyManager.deriveKey() 纯 JDK 逻辑
│
├── mango-infra-crypto-starter/                ← SPI 注入（@Configuration）
│   ├── pom.xml
│   └── src/main/java/io/mango/infra/crypto/starter/
│       ├── CryptoProperties.java             ← @ConfigurationProperties（含 @Value 注入）
│       ├── CryptoAutoConfiguration.java       ← @Configuration + @Bean
│       └── META-INF/spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── (无 starter-remote — crypto 无远程调用场景)
```

---

## 三、关键设计决策

### 3.1 包命名

| 模块 | 包路径 |
|------|--------|
| API | `io.mango.infra.crypto.api` |
| Core | `io.mango.infra.crypto.core` |
| Starter | `io.mango.infra.crypto.starter` |

> 遵循 `mango-infra-dal` 的包命名规范：`io.mango.{infra|dal}.{module}.{api|core|starter}`

### 3.2 接口重命名

| 原名（common） | 新名（infra-crypto-api） |
|----------------|-------------------------|
| `SymmetricCipher` | `ISymmetricCipher` |
| `AsymmetricCipher` | `IAsymmetricCipher` |
| `Signer` | `ISigner` |
| `Digester` | `IDigester` |

> 统一加 `I` 前缀，符合 Java 接口命名规范（`I` + 功能名）

### 3.3 SPI 注入机制

使用 `@ConditionalOnProperty` 控制是否启用：

```java
@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
@ConditionalOnProperty(prefix = "mango.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CryptoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ISymmetricCipher.class)
    public ISymmetricCipher sm4Cipher(CryptoProperties props) {
        return new Sm4Cipher(props.getSm4());
    }

    @Bean
    @ConditionalOnMissingBean(ISymmetricCipher.class)
    public ISymmetricCipher aesCipher(CryptoProperties props) {
        return new AesCipher(props.getAes());
    }

    // ... 其他 bean
}
```

### 3.4 配置属性（CryptoProperties）

```yaml
mango:
  crypto:
    enabled: true                    # 是否启用 crypto 模块
    sm4:
      key: ${MANGO_CRYPTO_SM4_KEY:}  # SM4 密钥（来自环境变量）
      iv: ${MANGO_CRYPTO_SM4_IV:}    # SM4 IV（来自环境变量）
    aes:
      key: ${MANGO_CRYPTO_AES_KEY:}  # AES 密钥（来自环境变量）
      iv: ${MANGO_CRYPTO_AES_IV:}    # AES IV（来自环境变量）
    sm2:
      private-key: ${MANGO_CRYPTO_SM2_PRIVATE_KEY:}
      public-key: ${MANGO_CRYPTO_SM2_PUBLIC_KEY:}
    rsa:
      private-key: ${MANGO_CRYPTO_RSA_PRIVATE_KEY:}
      public-key: ${MANGO_CRYPTO_RSA_PUBLIC_KEY:}
    hmac-sm3:
      key: ${MANGO_CRYPTO_HMAC_SM3_KEY:}
```

> **Review 修正**：原示例 `key: "${mango.crypto.sm4-key}"` 为循环引用（字段 `mango.crypto.sm4.key` 绑定自身）。改用环境变量 `${ENV_VAR:default}` 语法，正确展示从环境变量读取密钥。

### 3.5 `mango-common` 保留文件（零依赖）

```
mango-common/
└── src/main/java/io/mango/common/crypto/
    ├── base/
    │   └── Base64Utils.java          ← 零依赖 JDK 工具类
    ├── support/                      ← 接口定义（供业务层使用）
    │   ├── ISymmetricCipher.java    ← 重命名自 SymmetricCipher
    │   ├── IAsymmetricCipher.java   ← 重命名自 AsymmetricCipher
    │   ├── ISigner.java             ← 重命名自 Signer
    │   └── IDigester.java          ← 重命名自 Digester
    └── digest/                       ← 纯 JDK digest（零依赖）
        ├── Sha256Digester.java      ← 保持不动
        └── Sha512Digester.java      ← 保持不动
```

> **Review 修正**：Sha256Digester / Sha512Digester 留在 common（纯 JDK 无任何外部依赖）。移出的仅限 Sm3Digester / HmacSm3Digester（依赖 BC）。

---

## 四、实施步骤

### 步骤 1：创建 `mango-infra-crypto/` 4 层目录结构

```
mango-infra-crypto/
├── pom.xml
├── mango-infra-crypto-api/
│   ├── pom.xml
│   └── src/main/java/io/mango/infra/crypto/api/
├── mango-infra-crypto-core/
│   ├── pom.xml
│   └── src/main/java/io/mango/infra/crypto/core/
├── mango-infra-crypto-starter/
│   ├── pom.xml
│   └── src/main/java/io/mango/infra/crypto/starter/
└── (无 starter-remote)
```

**参考**：`mango-infra-dal/pom.xml` 结构

---

### 步骤 2：创建 `mango-infra-crypto-api` 模块

**接口文件（从 common 移入并重命名）**：

| 文件 | 操作 |
|------|------|
| `ISymmetricCipher.java` | 从 `SymmetricCipher.java` 重命名 + 改包名 |
| `IAsymmetricCipher.java` | 从 `AsymmetricCipher.java` 重命名 + 改包名 |
| `ISigner.java` | 从 `Signer.java` 移入 + 重命名 |
| `IDigester.java` | 从 `Digester.java` 移入 + 重命名 |
| `ICryptoFactory.java` | **新建** — 定义 `getSymmetricCipher(String algorithm)` 等工厂方法 |

**pom.xml 依赖**：仅 `mango-common`（用于传递接口）

---

### 步骤 3：创建 `mango-infra-crypto-core` 模块

**实现类（从 common 移入）**：

| 文件 | 依赖 | 操作 |
|------|------|------|
| `Sm4Cipher.java` | BC + Spring | 移入，改包名 `@Component("sm4Cipher")` |
| `AesCipher.java` | Spring `@Value` | 移入，改包名 |
| `Sm2Cipher.java` | BC + Spring | 移入，改包名 |
| `Sm2Signer.java` | BC + Spring | 移入，改包名 |
| `RsaCipher.java` | Spring `@Value` | 移入，改包名 |
| `RsaSigner.java` | Spring `@Value` | 移入，改包名 |
| `Sm3Digester.java` | BC + Spring | 移入，改包名 |
| `HmacSm3Digester.java` | BC + Spring | 移入，改包名 |
| `CryptoFactoryImpl.java` | `@Autowired` | 从 `CryptoFactory.java` 移入并改名 |
| `CryptoKeyUtils.java` | JDK | **新建** — 从 KeyManager 移入 `deriveKey()` 等纯 JDK 逻辑 |

> **Review 修正**：Sha256Digester / Sha512Digester 留在 `mango-common`（纯 JDK 无依赖），不随本次移动。CryptoKeyUtils 为 KeyManager 中 `deriveKey()` 等纯 JDK 逻辑，专为本次迁移新建。

**pom.xml 依赖**：
```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-crypto-api</artifactId>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

> **Review 修正**：移除 `<scope>provided</scope>`。`@Value`、`@PostConstruct`、`@Component` 来自 `spring-boot-starter`，运行时需要存在于 classpath，`provided` 会导致运行时找不到这些注解。

---

### 步骤 4：创建 `mango-infra-crypto-starter` 模块

**配置类**：

| 文件 | 说明 |
|------|------|
| `CryptoProperties.java` | `@ConfigurationProperties(prefix = "mango.crypto")` — 密钥配置（移入 `@Value` 字段） |
| `CryptoAutoConfiguration.java` | `@AutoConfiguration` + `@Bean` 注入所有实现 |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Spring Boot 3.x 自动配置 |

> **KeyManager 归宿**：`KeyManager` 中的配置注入（`@Value`）部分合并入 `CryptoProperties`，纯 JDK 的 `deriveKey()` 方法移入 `infra-crypto-core/CryptoKeyUtils.java`。

**pom.xml 依赖**：
```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-crypto-api</artifactId>
</dependency>
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-crypto-core</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
</dependency>
```

---

### 步骤 5：修正 `mango-common/pom.xml` 及 crypto 目录

**移除** BouncyCastle 和 Hutool 依赖：
```xml
<!-- 删除 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78</version>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-crypto</artifactId>
    <version>5.8.26</version>
</dependency>
```

**crypto 目录文件处理**：

| 原文件 | 新文件/归宿 | 说明 |
|--------|------------|------|
| `support/SymmetricCipher.java` | `support/ISymmetricCipher.java` | 改为纯接口 |
| `support/AsymmetricCipher.java` | `support/IAsymmetricCipher.java` | 改为纯接口 |
| `support/Signer.java` | `support/ISigner.java` | 改为纯接口 |
| `support/Digester.java` | `support/IDigester.java` | 改为纯接口 |
| `support/CryptoFactory.java` | **删除** | 移入 infra-crypto-core |
| `support/KeyManager.java` | **删除** | `@Value` 部分合并入 CryptoProperties；`deriveKey()` 移入 CryptoKeyUtils |
| `digest/Sha256Digester.java` | **保持不变** | 纯 JDK，留在 common |
| `digest/Sha512Digester.java` | **保持不变** | 纯 JDK，留在 common |
| `base/Base64Utils.java` | **保持不变** | 零依赖工具类，留在 common |

> **Review 修正**：Sha256Digester / Sha512Digester 为纯 JDK 无依赖，不随本次移动。KeyManager 拆分：配置注入部分并入 `CryptoProperties`，`deriveKey()` 纯 JDK 逻辑移入 `infra-crypto-core/CryptoKeyUtils`。

---

### 步骤 6：搜索全项目 import，修正包引用

**需搜索替换的包路径**：

| 原包路径 | 新包路径 |
|----------|----------|
| `io.mango.common.crypto.support.SymmetricCipher` | `io.mango.infra.crypto.api.ISymmetricCipher` |
| `io.mango.common.crypto.support.AsymmetricCipher` | `io.mango.infra.crypto.api.IAsymmetricCipher` |
| `io.mango.common.crypto.support.Signer` | `io.mango.infra.crypto.api.ISigner` |
| `io.mango.common.crypto.support.Digester` | `io.mango.infra.crypto.api.IDigester` |
| `io.mango.common.crypto.support.CryptoFactory` | `io.mango.infra.crypto.core.CryptoFactoryImpl` |
| `io.mango.common.crypto.symmetric.Sm4Cipher` | `io.mango.infra.crypto.core.symmetric.Sm4Cipher` |
| `io.mango.common.crypto.symmetric.AesCipher` | `io.mango.infra.crypto.core.symmetric.AesCipher` |
| `io.mango.common.crypto.asymmetric.*` | `io.mango.infra.crypto.core.asymmetric.*` |
| `io.mango.common.crypto.digest.Sm3Digester` | `io.mango.infra.crypto.core.digest.Sm3Digester` |
| `io.mango.common.crypto.digest.HmacSm3Digester` | `io.mango.infra.crypto.core.digest.HmacSm3Digester` |
| `io.mango.common.crypto.base.Base64Utils` | **保持不变**（零依赖） |
| `io.mango.common.crypto.digest.Sha256Digester` | **保持不变**（留在 common） |
| `io.mango.common.crypto.digest.Sha512Digester` | **保持不变**（留在 common） |
| `io.mango.common.crypto.support.KeyManager` | **已删除**，拆分为 `CryptoProperties`（配置）+ `CryptoKeyUtils`（派生） |

> 注：grep 结果仅 16 个文件，且全部在 `mango-common/src/main/java/io/mango/common/crypto/` 目录下，无外部调用方。

**搜索命令**：
```bash
cd mango
grep -r "io.mango.common.crypto" --include="*.java" -l
```

---

## 五、约束

| 约束 | 说明 |
|------|------|
| C1 | 移出后 `mango-common` 禁止再引入 BouncyCastle 依赖 |
| C2 | `mango-common/crypto/` 只保留接口定义和零依赖工具类 |
| C3 | `mango-infra-crypto` 使用 `@ConditionalOnProperty` SPI 注入 |
| C4 | 密钥配置通过环境变量注入，禁止硬编码 |
| C5 | `mango-infra-crypto-starter` 无远程调用场景，不建 `starter-remote` |

---

## 六、验收标准

### 代码结构
- [ ] `mango-infra-crypto/` 4 层结构完整（api / core / starter）
- [ ] `mango-infra-crypto-starter` 无 `starter-remote` 子模块
- [ ] `mango-common/pom.xml` 移除 BouncyCastle 和 Hutool 依赖
- [ ] `mango-common/crypto/` 只剩 5 个零依赖文件（ISymmetricCipher / IAsymmetricCipher / ISigner / IDigester + Base64Utils）
- [ ] Sha256Digester / Sha512Digester 留在 `mango-common`（纯 JDK）

### 行为保持（重构 = 行为不变）
- [ ] **CryptoModuleRefactorTest**（单元测试，mango-infra-crypto-core/src/test/）：
  - `Sm4CipherRoundtripTest` — encrypt/decrypt 往返 + 空输入 + 错误 IV 长度
  - `AesCipherRoundtripTest` — encrypt/decrypt 往返 + 空输入 + 篡改密文检测
  - `RsaSignerRoundtripTest` — sign/verify 往返 + 错误签名 + 格式错误公钥
  - `Sm3DigesterConsistencyTest` — 已知 SM3 摘要值比对（如 `abc` → `66c7f0c462b07...`）
  - `HmacSm3DigesterConsistencyTest` — 已知 HMAC-SM3 值比对
  - `CryptoFactoryImplTest` — getSymmetricCipher SM4/AES/非法算法异常
  - `CryptoKeyUtilsDeriveKeyTest` — deriveKey 基础 + null salt 边界
  - `UnsupportedOperationExceptionTest` — Sm2Cipher/Sm2Signer 所有 stub 方法抛出此异常
- [ ] **CryptoAutoConfigurationTest**（集成测试，mango-infra-crypto-starter/src/test/）：
  - `CryptoAutoConfigurationEnabledTest` — enabled=true + BC on classpath → 8 个 bean 全部注入
  - `CryptoAutoConfigurationDisabledTest` — enabled=false → 无 bean 创建
  - `CryptoAutoConfigurationMissingBeanTest` — @ConditionalOnMissingBean 防重复注入
  - `CryptoPropertiesValidationTest` — 缺少必需密钥时 IllegalStateException
- [ ] `mvn clean compile` 通过
- [ ] `mvn clean verify` 通过（含 checkstyle / spotbugs / pmd）

### 全项目引用
- [ ] 全项目 import 替换完成，无 `io.mango.common.crypto.support.CryptoFactory` 残留
- [ ] 全项目 import 替换完成，无 `io.mango.common.crypto.symmetric.Sm4Cipher` 等实现类残留
- [ ] `io.mango.common.crypto.base.Base64Utils` 保持不变（无需替换）
- [ ] `io.mango.common.crypto.support.{ISymmetricCipher,IAsymmetricCipher,ISigner,IDigester}` 从 `io.mango.infra.crypto.api` 正确引用

---

## 七、参考

- T1 commit：`c0b7df41`（`mango-infra-dal` 4 层结构）
- 架构计划：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 三
- 规范文件：`mango/.claude/rules/05-module.md`

---

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 1 | CLEAR | 5 issues → all fixed |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | no UI scope |
| DX Review | `/plan-devex-review` | Developer experience gaps | 0 | — | no DX scope |

**VERDICT:** ENG CLEARED — ready to implement

---

## 八、评审修复记录

| # | 问题 | 修复 |
|---|------|------|
| 1 | Sha256/Sha512Digester 归属矛盾（Section 1.2 vs Section 4） | 确认留 `mango-common`（纯 JDK），所有 section 统一 |
| 2 | `infra-crypto-core` pom.xml `<scope>provided</scope>` 错误 | 移除 `provided`，`spring-boot-starter` 需在运行时 classpath |
| 3 | CryptoProperties yaml 示例 `key: "${mango.crypto.sm4-key}"` 循环引用 | 改为 `key: ${MANGO_CRYPTO_SM4_KEY:}` |
| 4 | KeyManager.deriveKey() 掉缝隙（未出现在任何步骤） | 拆分为 `CryptoProperties`（@Value 配置）+ `CryptoKeyUtils`（deriveKey JDK 逻辑，新建） |
| 5 | 无重构测试（验收标准只有 compile/verify） | 新增 `CryptoModuleRefactorTest`（往返 + 异常 + 边界）+ `CryptoAutoConfigurationTest`（enabled/disabled/missingBean/validation） |

---

## 九、Worktree 并行化策略

| Lane | 步骤 | 关系 |
|------|------|------|
| Lane A | Step 1-4 | sequential（crypto 子模块间依赖） |
| Lane B | Step 5 | parallel to Lane A（独立于 step 1-4） |
| Lane C | Step 6 | after Lane A + B（依赖 import 替换） |

**执行顺序：** Launch Lane A + B 并行 → 合并 → Launch Lane C

**Conflict flags：** 无

---

## 十、NOT in Scope

1. Sm2Cipher/Sm2Signer 完整实现 — 当前是 `UnsupportedOperationException` stub，待后续 sprint
2. RsaCipher decrypt — 只有 encrypt，待后续 sprint
3. BC ClassNotFound CI 防呆 — 通过 CLAUDE.md 规范 + Review 约束（不接受 enforcer rule）
4. integration test with real BC — 端到端 BC 测试
