# 信息表单接入

## 1. 适用场景

业务需要收集访客或内部员工的填表数据（报名、问卷、反馈、资料补充），并且希望表单定义、托管页面、采集记录和导出由平台统一提供。

> Issue #1073 影响说明：选择型字段新增「字典数据源」（`optionSource: {type: "DICT", dictType}`），提交时服务端按静态 `options` 或字典启用项校验值域；老表单没有 `optionSource`，行为不变，也无需数据迁移。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
| ---- | ---- | ------ |
| 1 | [信息表单后端 README](../../../mango/mango-platform/mango-information/README.md) | 表单定义格式、公开提交、选项数据源、失败边界 |
| 2 | [@mango/information README](../../../mango-ui/packages/information/README.md) | 管理页面、托管页面、设计器与运行时规则 |
| 3 | [能力地图](../../capabilities/README.md) | 责任模块与最小能力组合 |

## 3. 接入检查点

| 环节 | 检查点 |
| ---- | ------ |
| 后端依赖 | 业务应用引入 `mango-information-starter`，由 Resource Registry 同步内置表单与公开 API |
| 表单定义 | `definitionJson` 使用 form-create 规则；带 `field` 的规则才产生采集值 |
| 选项值域 | 选择型字段要么给静态 `options`，要么声明 `optionSource` 绑定字典，两者不要混用 |
| 字典准备 | 使用字典数据源时先在同一租户维护字典类型与启用项，字段声明的 `dictType` 与字典类型编码一致 |
| 提交入口 | 托管页面由访客直接提交；服务端接入携带表单专属 `serverSecret` |
| 数据消费 | 采集记录动态列、详情与导出由平台生成，业务侧按 `formCode` 查询 |

## 4. 最小业务样例

服务端提交（`values` 里的选择型字段取值落在可选范围内，否则返回 `INVALID_SUBMISSION`）：

```http
POST /information/open/forms/submit
Content-Type: application/json

{"formCode":"feedback","values":{"level":"vip","channels":["tel","mail"]},"idempotencyKey":"visitor-20261123-001"}
```

字段绑定平台字典：

```json
{
  "type": "radio",
  "field": "level",
  "title": "客户等级",
  "options": [],
  "optionSource": { "type": "DICT", "dictType": "customer_level" }
}
```

前端渲染时不需要自己取字典：`@mango/information` 在托管页面、表单详情预览和采集记录列里按 `optionSource` 调 `@mango/system` 的字典接口取启用项。

## 5. 验证

- 用合法值提交成功，用不在可选范围内的值提交返回 `INVALID_SUBMISSION`，消息含字段名与非法值。
- 把字段绑定的字典停用或删除后提交，返回 `INVALID_SUBMISSION`，消息提示字典不存在或没有启用项。
- 老表单（定义里没有 `optionSource`）提交行为与升级前一致。
- 托管页面能按字典启用项渲染选项，无需业务自行请求字典接口。

## 6. 失败边界

- 字典是提交时实时读取的：字典停用、清空或删除后，原选项值会被拒绝，历史采集记录不受影响。
- 字段既没有静态 `options` 也没有声明字典时不校验值域，这是为兼容存量定义的既有行为。
- 前端取不到字典时，托管页面提示表单暂不可用，详情预览与采集记录给出告警并保留空选项；服务端仍然拒绝这类提交，不会静默放行。
- 字典数据源的选项值以字典启用项的 `value` 为准，修改字典项值不会回写历史记录。

## 7. 相关文档

- [信息表单后端 README](../../../mango/mango-platform/mango-information/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 8. 历史变更

> Issue #1073 影响说明：选择型字段新增 `optionSource` 字典数据源，提交时新增值域校验（静态 `options` 与字典启用项）；未声明 `optionSource` 的存量表单定义、接口路径与采集记录结构均不变，不需要数据库迁移。
