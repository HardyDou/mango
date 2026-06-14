# Mango Infra Crypto

## 1. 能力定位

`mango-infra-crypto` 提供 Mango 内可复用的基础密码能力，自动装配 SM2、SM3、SM4，另提供 AES、RSA、SHA-256 和 HMAC-SM3 显式工具实现。主要使用者是需要显式加解密、摘要、签名和验签的后端模块。

## 2. 适用场景

- 应用内字段加密、解密、摘要、签名和验签。
- 需要使用国密 SM2/SM3/SM4 的业务能力。
- 需要在 Spring Boot 应用中自动装配国密基础服务，或显式使用 AES/RSA/HMAC 等工具实现类。

## 3. 不适用场景

- 不负责密钥生命周期、密钥轮换、证书管理、KMS/HSM 集成或合规审计。
- 不替代业务协议级安全设计。
- 不默认承诺所有算法都适合生产敏感数据场景，敏感场景需要单独安全评审。

## 4. 模块边界

本模块提供算法实现和 Spring Boot 自动配置。密钥来源、密钥托管、数据分级、脱敏和跨系统签名协议由业务模块或安全基础设施负责。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.crypto</groupId>
    <artifactId>mango-infra-crypto</artifactId>
</dependency>
```

自动配置入口为 `CryptoAutoConfiguration`，由 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。

## 6. 配置项

配置前缀：`mango.crypto`。

已发现字段包括：

- `enabled`
- `sm4.secretKey`
- `sm4-key`
- `sm4-iv`
- `sm4.mode`
- `sm4.padding`
- `sm2.privateKey`
- `sm2.publicKey`
- `sm2.userId`

## 7. 对外接口 / 扩展点

- `ICryptoService`
- `IAsymmetricCryptoService`
- `IDigester`
- `IKeyedDigester`
- `ISignService`
- 实现包括 `Sm4CryptoService`、`Sm3CryptoService`、`Sm2SignService`、`AesCipher`、`RsaCipher`、`RsaSigner`、`HmacSm3Digester`。
- 自动装配默认提供 SM4、SM3、SM2 相关 Bean；AES、RSA、HMAC-SM3 作为显式实现类使用。

## 8. 数据库 / 初始化数据

未发现数据库 migration 或初始化数据。

## 9. 菜单 / 权限 / 租户

本模块不提供菜单、权限资源或租户数据。调用方应在业务模块中处理权限和租户边界。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-crypto -am test
```

测试入口位于 `mango-infra-crypto/src/test/java/io/mango/infra/crypto/**`。

## 11. 业务接入最小闭环

业务模块接入时先配置 `mango.crypto.enabled=true`，并提供 SM4 secret key、SM2 private/public key 和 user id。Spring Boot 配置使用 kebab-case，例如 `mango.crypto.sm4.secret-key`、`mango.crypto.sm2.private-key`、`mango.crypto.sm2.public-key`。

自动注入场景优先使用 SM4、SM3、SM2 相关接口；AES、RSA、HMAC-SM3 和 SHA-256 作为显式实现类或工具能力使用。验收断言覆盖：自动装配 Bean 存在，SM4 加解密可逆，SM3 摘要稳定，SM2 签名可验签，密钥缺失时启动或调用失败符合预期。

## 12. 常见问题

- 密钥为空或格式错误会导致加解密、签名失败，先检查 `mango.crypto` 配置。
- HMAC-SM3 需要显式 key，不应当作无密钥摘要使用。
- 涉及敏感数据时，需要评审密钥托管和轮换方案。

## 13. 关联 PMO 规则

- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
