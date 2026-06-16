# Mango Infra Crypto

## 1. 概览
`mango-infra-crypto` 提供 Mango 后端可复用的密码基础能力。当前自动装配的是国密 SM4 加解密、SM3 摘要和 SM2 签名验签；AES、RSA、SHA-256、HMAC-SM3 等实现保留为显式工具能力，不会默认声明为 Spring Bean。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务字段需要应用层加密、解密或摘要 | Maven 依赖 / starter / Java API |
| 与外部系统对接需要 SM2 签名、SM2 验签、SM3 摘要或 SM4 对称加密 | Maven 依赖 / starter / Java API |
| 后端模块需要统一注入 ICryptoService、IDigester 或 ISignService | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不负责 KMS、HSM、证书生命周期、密钥轮换、密钥托管和合规审计。
- 不负责登录认证、传输层 TLS 或业务协议安全设计。
- 不负责脱敏展示；脱敏能力在 sensitive 或业务模块内处理。

## 4. 模块入口
本模块只提供算法封装和 Spring Boot 自动配置。密钥来源、密钥分发、密钥轮换、数据分级、字段落库策略和异常审计由调用方或安全基础设施负责。

## 5. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.crypto</groupId>
    <artifactId>mango-infra-crypto</artifactId>
</dependency>
```

自动装配入口为 `CryptoAutoConfiguration`。

常用注入方式：

```java
@RequiredArgsConstructor
public class CustomerSecretService {
    private final ICryptoService cryptoService;
    private final Sm3CryptoService sm3CryptoService;

    public String encrypt(String plainText) {
        return cryptoService.encrypt(plainText);
    }
}
```

SM2 签名服务只有配置了私钥后才注册：

```java
private final ISignService signService;
```

## 6. 配置说明
配置前缀：`mango.crypto`。Spring Boot YAML 使用 kebab-case。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用 crypto 自动配置。 |
| `sm4.secret-key` | 无 | SM4 128 位密钥，支持 Base64 或十六进制编码；生产环境必须配置。 |
| `sm4.mode` | `CBC` | SM4 加密模式，当前实现支持 CBC 或 ECB。 |
| `sm4.padding` | `PKCS5Padding` | SM4 填充方式，支持 PKCS5Padding、PKCS7Padding、ZeroPadding、NoPadding。 |
| `sm2.private-key` | 无 | Base64 编码 PKCS#8 SM2 私钥；配置后注册 `ISignService`。 |
| `sm2.public-key` | 无 | Base64 编码 SM2 公钥，用于验签。 |
| `sm2.user-id` | `1234567812345678` | SM2 签名用户 ID。 |
| `sm4-key` | 无 | 兼容旧配置，会迁移到 SM4 secret key；不建议新配置继续使用。 |
| `sm4-iv` | 无 | 兼容旧配置；当前 SM4 CBC 实现使用密文前置 IV，不再读取固定 IV。 |

示例：

```yaml
mango:
  crypto:
    enabled: true
    sm4:
      secret-key: 00112233445566778899aabbccddeeff
      mode: CBC
      padding: PKCS5Padding
    sm2:
      private-key: ${MANGO_SM2_PRIVATE_KEY}
      public-key: ${MANGO_SM2_PUBLIC_KEY}
      user-id: 1234567812345678
```

注意：历史 `sm4-key` 如果填入 SM2 示例公钥值，启动会直接失败，提示迁移到 SM4 16 字节密钥。

## 7. API 与扩展
- `ICryptoService`：对称加解密接口，默认 Bean 是 `Sm4CryptoService`。
- `IDigester`：摘要接口，`Sm3CryptoService` 提供 SM3 摘要。
- `ISignService`：签名验签接口，配置 SM2 私钥后注册 `Sm2SignService`。
- `IAsymmetricCryptoService`：非对称加解密契约，RSA 实现为显式工具能力。
- `IKeyedDigester`：带密钥摘要契约，HMAC-SM3 实现为显式工具能力。
- 显式实现类：`AesCipher`、`RsaCipher`、`RsaSigner`、`Sha256Digester`、`HmacSm3Digester`。

调用方如果需要替换默认 SM4 或 SM2 实现，可以声明自己的 `ICryptoService` 或 `ISignService` Bean，自动配置会因 `ConditionalOnMissingBean` 让位。

## 8. 数据与初始化
无数据库 migration、无 Runner、无 Initializer、无初始化数据。密钥不要写入仓库配置文件，应通过环境变量、配置中心或密钥管理服务注入。

## 9. 管理入口
本模块不创建菜单和权限，不感知租户。多租户密钥、按租户加密策略和权限边界由业务模块设计。

## 10. 快速开始
1. 接入 `mango-infra-crypto`。
2. 在环境配置中提供 SM4 secret key；需要签名时同时提供 SM2 private/public key。
3. 业务服务注入 `ICryptoService` 处理字段加解密，注入 `Sm3CryptoService` 处理摘要，注入 `ISignService` 处理签名验签。
4. 写测试覆盖加密可逆、签名可验、错误密钥失败和密钥不落日志。

## 11. 问题排查
- 启动后没有 `ISignService`：检查是否配置了 `mango.crypto.sm2.private-key`。
- SM4 解密失败：检查密钥编码、模式、padding 是否一致，历史固定 IV 不再生效。
- 生产密钥不要提交到 Git，也不要放在 README、测试日志或异常信息中。

## 12. 相关文档
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
