---
documentType: delivery-l2
deliveryLevel: L2
pageBudget: 1
---

# JSON 参数错误返回字段路径

## 目标与范围

当前参数类型错误只返回“JSON 解析失败”，调用方无法定位字段。目标是返回首个错误字段路径；只改统一异常转换，不改变成功响应和业务校验错误。

## 用户故事

1. US-001 前置为调用方提交合法 JSON 结构但某字段类型错误；接口开发者提交请求并读取错误响应；系统返回首个错误字段路径和原因；无法提取路径时返回固定解析错误码，不返回堆栈。

## 系统满足方式

1. SR-001 -> US-001：统一异常处理器从解析异常提取字段链，返回 `field` 和 `message`；无字段链时 `field` 为空，错误码保持 `COMMON_JSON_INVALID`。

## 参考规范与代码

- 规范：`rules/backend/03-api.md@1.4.0`；采用：错误响应边界和协议模型要求。
- 代码：`mango-core/.../GlobalExceptionHandler.java@8f2c1ad`；采用：扩展现有 JSON 异常分支。

## 技术改动

1. TD-001 -> SR-001：修改 `GlobalExceptionHandler#handleHttpMessageNotReadable`，从 `JsonMappingException#getPath` 拼接 `items[0].price`；保留现有 HTTP 状态、错误码和日志脱敏。

## 验证与风险

1. VAL-001 -> SR-001：执行异常处理器定向测试；断言嵌套数组错误返回 `items[0].price`，无路径异常返回空 `field` 且不含类名；失败则阻止合并。
- 回滚：恢复该处理方法并删除新增字段赋值。
- 剩余风险：第三方 JSON 解析器未提供字段链时只能返回通用错误。
