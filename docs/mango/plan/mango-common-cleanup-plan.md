# T8 Plan: mango-common 纯粹化

## Context

根据 Sprint-00 §1.6 Layer分层原则：
- **Layer 0** `mango-common`：零运行时依赖，只含 JDK + 纯注解 JAR
- **Layer 1** `mango-infra-*`：中间件依赖（Redis/Spring AOP/TTL/BouncyCastle 等）

Sprint-00 §2 明确了 `mango-common` 的职责边界：
- ✅ 应该保留：BasePO/BaseVO/PageVO、R/BizCode/Require、校验注解、@Perm/@Log、JacksonUtils、SPI 接口
- ❌ 不应包含：加密实现、权限 AOP 逻辑、上下文实现

## 用户决策记录

1. **RSA 迁至 infra-crypto** — `RsaCipher`/`RsaSigner` 迁移到 `mango-infra-crypto`
2. **工具类保留** — `Base64Utils` 留在 `mango-common`（无运行时依赖）
3. **同等能力删除** — infra 已有的实现，删除 common 中的重复代码

## 当前状态

### 已完成迁移（但源文件残留）

| 模块 | 目标位置 | 状态 |
|------|---------|------|
| T5: SM 系列加密 | `mango-infra-crypto` | ✅ 实现已创建，**但 common 源文件未删除** |
| T7: Context | `mango-infra-context` | ✅ TenantContextHolder/TraceContextHolder 已迁，TokenContextHolder 留在 common |

### mango-infra-crypto 已有内容

```
mango-infra-crypto/
├── impl/
│   ├── ICryptoService.java      ← SM4 对称加密接口
│   ├── ISignService.java       ← SM2 签名接口
│   └── sm/
│       ├── Sm2SignService.java  (full impl)
│       ├── Sm3CryptoService.java (hash only)
│       └── Sm4CryptoService.java (CBC mode)
└── starter/
    ├── CryptoAutoConfiguration.java
    └── CryptoProperties.java
```

## U1 审计结果：接口覆盖矩阵

| 算法 | infra-crypto | common/crypto | 动作 |
|------|-------------|---------------|------|
| SM4 对称加密 | `Sm4CryptoService` (CBC, full) | `Sm4Cipher` (GCM, stub) | **删除 Sm4Cipher** |
| SM2 签名 | `Sm2SignService` (full) | `Sm2Signer` (stub) | **删除 Sm2Signer** |
| SM2 加解密 | 无 | `Sm2Cipher` (stub) | **删除 Sm2Cipher** |
| SM3 哈希 | `Sm3CryptoService` (hash only) | `Sm3Digester` (implements Digester) | **删除 Sm3Digester** |
| AES 对称加密 | 无 | `AesCipher` (GCM) | **迁至 infra-crypto** |
| RSA 签名 | 无 | `RsaSigner` | **迁至 infra-crypto** |
| RSA 加解密 | 无 | `RsaCipher` | **迁至 infra-crypto** |
| SHA-256 | 无 | `Sha256Digester` | **迁至 infra-crypto** |
| HMAC-SM3 | 无 | `HmacSm3Digester` | **迁至 infra-crypto** |
| Base64 | 无 | `Base64Utils` | **保留** — 仅 JDK |
| SM4 工厂 | 直接注入 | `CryptoFactory` | **删除** — 依赖 Sm4Cipher |

## 待删/待迁清单

### 必须删除（8 个文件）

| 文件 | 原因 |
|------|------|
| `Sm4Cipher.java` | infra Sm4CryptoService 已完整实现，模式不同不算重复 |
| `Sm2Cipher.java` | stub，infra 无 SM2 加解密只有签名 |
| `Sm2Signer.java` | stub，infra Sm2SignService 已完整实现 |
| `Sm3Digester.java` | stub，infra Sm3CryptoService 已有 hash |
| `CryptoFactory.java` | 依赖 Sm4Cipher，删除后无用 |
| `Sha512Digester.java` | 未使用 |
| `KeyManager.java` | 未使用 |
| `SymmetricCipher.java` | 删除 Sm4Cipher 后无用，AES 迁走后接口需重新定义 |

### 迁至 infra-crypto（8 个文件 + 新接口）

| common 路径 | infra-crypto 路径 | 说明 |
|------------|-----------------|------|
| `AesCipher.java` | `impl/aes/AesCipher.java` | AES/GCM 实现 |
| `Sha256Digester.java` | `impl/digest/Sha256Digester.java` | SHA-256 摘要 |
| `HmacSm3Digester.java` | `impl/digest/HmacSm3Digester.java` | HMAC-SM3 摘要 |
| `Digester.java` | `impl/IDigester.java` (新接口) | 摘要接口 |
| `AsymmetricCipher.java` | `impl/IAsymmetricCryptoService.java` (新接口) | 非对称加密接口 |
| `Signer.java` | 复用 `ISignService` | 签名接口已有 |
| `RsaSigner.java` | `impl/rsa/RsaSigner.java` | RSA 签名实现 |
| `RsaCipher.java` | `impl/rsa/RsaCipher.java` | RSA 加解密实现 |

### 保留在 common（1 个文件）

| 文件 | 原因 |
|------|------|
| `Base64Utils.java` | 仅 JDK 依赖，无运行时依赖 |

## Implementation Units

- [x] **Unit 1: 审计 mango-infra-crypto 接口完整性** ✅

  **结果：** 接口覆盖矩阵已完成（见上表）

- [x] **Unit 2: 清理 mango-common/crypto 目录** ✅

  **结果：**

  - **删除 16 个文件**（8 个需删除 + 8 个迁至 infra）：
    - 删除：Sm4Cipher, Sm2Cipher, Sm2Signer, Sm3Digester, CryptoFactory, SymmetricCipher, Sha512Digester, KeyManager
    - 迁出：AesCipher, Sha256Digester, HmacSm3Digester, RsaSigner, RsaCipher, Digester, AsymmetricCipher, Signer
  - **新建 7 个文件** 到 `mango-infra-crypto/impl/`：
    - `IDigester.java`（新接口）
    - `IAsymmetricCryptoService.java`（新接口）
    - `aes/AesCipher.java`
    - `digest/Sha256Digester.java`
    - `digest/HmacSm3Digester.java`
    - `rsa/RsaSigner.java`
    - `rsa/RsaCipher.java`
  - **保留** `Base64Utils.java` 在 `mango-common/crypto/base/`
  - **空目录已清理**（asymmetric/digest/support/symmetric 均已删除）

  **注意**：迁移文件改用 JDK `java.util.Base64`，不再依赖 `mango-common`（符合 Layer 0/1 分层原则）

  **Verification:**
  - ✅ `mvn clean compile -pl mango-common` 通过
  - ✅ `mvn clean compile -pl mango-infra/mango-infra-crypto` 通过
  - ⚠️ business 模块编译问题（mango-auth-core 缺少 IKvStore）属 pre-existing，与 T8 无关

- [x] **Unit 3: 验证 business 模块无 crypto 依赖** ✅

  **Result:** `grep -r "import io.mango.common.crypto" --include="*.java"` 无结果，所有 business 模块均无 common/crypto 依赖

## NOT in Scope

- T8 新增内容（@Sensitive/@Version/@Encrypt、枚举、SPI 新接口）— 作为独立任务
- `mango-auth` 模块的安全清理 — 用户表示自己处理
- `mango-infra-security` 的完善 — 取决于 auth 模块处理结果
