# Mango Infra Sensitive

## 1. 概览
`mango-infra-sensitive` 提供响应字段脱敏和敏感词引擎集成。它通过 Jackson module 识别 DTO 字段或 getter 上的 `@Sensitive` 注解，在序列化输出时遮蔽敏感值，不修改对象内存中的原始字段。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| API 响应中的手机号、证件号、邮箱、银行卡、密码、密钥、IPv4、车牌、URL query 参数需要脱敏 | Maven 依赖 / starter / Java API |
| JSON 字符串中指定 key 的值需要递归脱敏 | Maven 依赖 / starter / Java API |
| 管理端在特定权限下允许查看原文，其余场景统一脱敏 | Maven 依赖 / starter / Java API |
| 需要通过 ISensitiveWordProvider 扩展敏感词 allow/deny 词库 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不替代权限判断和租户隔离。
- 不替代数据库加密、密钥管理和传输层加密。
- 不自动处理日志输出；日志打印对象前仍要避免输出原文。
- 不负责敏感词词库管理页面和审核流程。

## 4. 模块入口
- `mango-infra-sensitive-api`：`@Sensitive`、`SensitiveType`、masking SPI、敏感词 provider SPI、临时关闭脱敏上下文。
- `mango-infra-sensitive-core`：脱敏算法、JSON key 脱敏、Jackson serializer、敏感词 customizer。
- `mango-infra-sensitive-starter`：注册 Jackson module、masking service、runtime initializer、敏感词引擎。

## 5. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.sensitive</groupId>
    <artifactId>mango-infra-sensitive-starter</artifactId>
</dependency>
```

DTO 字段脱敏：

```java
public class UserProfileVO {
    @Sensitive(type = SensitiveType.MOBILE_PHONE)
    private String mobile;

    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;

    @Sensitive(type = SensitiveType.JSON, keys = {"token", "appSecret"}, fuzzy = true)
    private String rawPayload;
}
```

临时读取原文：

```java
String json = SensitiveMaskingContext.getWithoutMasking(() -> objectMapper.writeValueAsString(vo));
```

## 6. 配置说明
配置前缀：`mango.sensitive`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `masking.raw-authority` | `no_mask` | 允许查看原文的权限标识，供 `ISensitiveRawAccessProvider` 判断。 |
| `word.enabled` | `true` | 是否启用敏感词引擎 Bean。 |
| `word.ignore-case` | `true` | 敏感词匹配是否忽略大小写。 |
| `word.ignore-width` | `true` | 是否忽略全角半角差异。 |
| `word.ignore-num-style` | `true` | 是否忽略数字写法差异。 |
| `word.ignore-chinese-style` | `true` | 是否忽略中文写法差异。 |
| `word.ignore-english-style` | `true` | 是否忽略英文写法差异。 |
| `word.ignore-repeat` | `true` | 是否忽略重复字符干扰。 |
| `word.enable-num-check` | `false` | 是否启用连续数字检查。 |
| `word.enable-email-check` | `false` | 是否启用邮箱检查。 |
| `word.enable-url-check` | `true` | 是否启用 URL 检查。 |
| `word.num-check-len` | `8` | 连续数字检查长度。 |
| `word.error-msg` | `您的输入包含敏感词，请重新输入` | 敏感词命中提示文案。 |

示例：

```yaml
mango:
  sensitive:
    masking:
      raw-authority: sensitive:raw:view
    word:
      enabled: true
      ignore-case: true
      enable-url-check: true
```

## 7. API 与扩展
- `@Sensitive`：字段或 getter 注解，支持 `type`、`prefixNoMaskLen`、`suffixNoMaskLen`、`maskStr`、`keys`、`fuzzy`。
- `SensitiveType`：内置 CUSTOM、CUSTOMER、CHINESE_NAME、ID_CARD、FIXED_PHONE、MOBILE_PHONE、ADDRESS、EMAIL、BANK_CARD、PASSWORD、KEY、IPV4、CAR_LICENSE、QUERY_PARAM、JSON。
- `ISensitiveMaskingService`：判断当前字段是否需要脱敏。
- `ISensitiveRawAccessProvider`：业务扩展当前调用者是否可看原文。
- `ISensitiveWordProvider`：提供敏感词 allow words 和 deny words。
- `SensitiveMaskingContext`：在受控范围内临时关闭脱敏。
- `SensitiveJacksonModule`：Jackson 集成入口。

`ISensitiveRawAccessProvider` 示例：

```java
@Component
public class PermissionRawAccessProvider implements ISensitiveRawAccessProvider {
    @Override
    public boolean canViewRaw(String authority) {
        return currentUserHas(authority);
    }
}
```

## 8. 数据与初始化
无数据库 migration、无 Runner、无业务初始化数据。`SensitiveAutoConfiguration` 会在启动时通过 `InitializingBean` 把 `ISensitiveMaskingService` 写入 `SensitiveMaskingRuntime`，该初始化必须保持幂等。

## 9. 管理入口
本模块不创建菜单和权限。`masking.raw-authority` 只是权限标识字符串，是否拥有该权限由业务提供的 `ISensitiveRawAccessProvider` 判断。租户隔离仍由业务查询和授权模块负责。

## 10. 快速开始
1. 接入 starter。
2. 在响应 DTO 的敏感字段上加 `@Sensitive`，不要只在实体类上依赖隐式处理。
3. 需要原文查看时，实现 `ISensitiveRawAccessProvider` 并配置 `masking.raw-authority`。
4. 需要敏感词时，实现 `ISensitiveWordProvider` 提供词库。
5. 写接口响应序列化测试和权限差异测试。

## 11. 问题排查
- 字段没脱敏：检查字段类型是否为字符串或 getter 输出，Jackson 是否注册了 `SensitiveJacksonModule`。
- 有权限仍脱敏：检查 `ISensitiveRawAccessProvider` 是否是 Spring Bean，判断的 authority 是否和配置一致。
- JSON 脱敏不生效：确认字段值是合法 JSON 字符串，`keys` 和 `fuzzy` 配置是否匹配。
- 日志仍输出原文：本模块只管 Jackson 响应序列化，日志输出要单独脱敏或避免打印原文。

## 12. 相关文档
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [能力地图](../../../mango-docs/capabilities/README.md)
