# 电子保函系统 API 接口文档

**版本**: V1.0
**更新日期**: 2026-03-13

---

## 1. 接口概述

### 1.1 Base URL

```
开发环境: http://localhost:8080
```

### 1.2 通用响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1709280000000,
  "traceId": "xxx"
}
```

### 1.3 错误码定义

| 错误码 | 说明 |
|-------|------|
| 0 | 成功 |
| 1000-1999 | 系统错误 |
| 2000-2999 | 认证授权错误 |
| 3000-3999 | 业务错误 |
| 4000-4999 | 参数校验错误 |

### 1.4 认证方式

除登录/注册接口外，所有接口需要在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

---

## 2. 用户服务 API

### 2.1 用户登录

**接口地址**: `POST /api/user/login`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**请求示例**:
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "username": "testuser",
    "realName": "测试用户"
  }
}
```

### 2.2 用户注册

**接口地址**: `POST /api/user/register`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| realName | String | 否 | 真实姓名 |
| mobile | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| userType | Integer | 否 | 用户类型: 1-投标人, 2-招标代理, 3-机构用户, 4-管理员 |

### 2.3 用户登出

**接口地址**: `POST /api/user/logout`

**请求头**: 需要 Authorization

**响应**: 返回成功

### 2.4 获取用户信息

**接口地址**: `GET /api/user/info`

**请求头**: 需要 Authorization

**响应**: 返回用户信息

---

## 3. 保函服务 API

### 3.1 保函申请

**接口地址**: `POST /api/guarantee/apply`

**请求头**: 需要 Authorization

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| guaranteeType | Integer | 是 | 保函类型: 1-投标, 2-履约, 3-预付款, 4-质量 |
| guaranteeFormat | Integer | 是 | 保函格式: 1-独立, 2-连带 |
| projectName | String | 是 | 项目名称 |
| projectNo | String | 否 | 项目编号 |
| beneficiaryName | String | 是 | 受益人名称 |
| applicantName | String | 是 | 申请人名称 |
| institutionId | Long | 否 | 承保机构ID |
| amount | BigDecimal | 是 | 保函金额 |
| validityMonths | Integer | 否 | 有效期(月) |
| startDate | Date | 否 | 生效日期 |
| endDate | Date | 否 | 到期日期 |

### 3.2 保函列表查询

**接口地址**: `GET /api/guarantee/list`

**请求头**: 需要 Authorization

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |
| guaranteeType | Integer | 否 | 保函类型 |
| applyStatus | Integer | 否 | 申请状态 |

### 3.3 保函详情

**接口地址**: `GET /api/guarantee/{id}`

**请求头**: 需要 Authorization

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 保函ID |

### 3.4 保函审核

**接口地址**: `POST /api/guarantee/{id}/audit`

**请求头**: 需要 Authorization

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | Integer | 是 | 审核状态: 2-审核通过, 3-审核拒绝 |
| auditOpinion | String | 否 | 审核意见 |

### 3.5 保函出函

**接口地址**: `POST /api/guarantee/{id}/issue`

**请求头**: 需要 Authorization

### 3.6 保函退保

**接口地址**: `POST /api/guarantee/{id}/refund`

**请求头**: 需要 Authorization

---

## 4. 机构服务 API

### 4.1 机构列表查询

**接口地址**: `GET /api/institution/list`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认10 |
| institutionType | Integer | 否 | 机构类型: 1-银行, 2-保险, 3-担保公司 |
| status | Integer | 否 | 状态 |

### 4.2 获取启用的机构

**接口地址**: `GET /api/institution/enabled`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| institutionType | Integer | 否 | 机构类型 |

### 4.3 机构详情

**接口地址**: `GET /api/institution/{id}`

### 4.4 创建机构

**接口地址**: `POST /api/institution`

### 4.5 更新机构

**接口地址**: `PUT /api/institution`

### 4.6 获取机构配置

**接口地址**: `GET /api/institution/{id}/config`

### 4.7 更新机构配置

**接口地址**: `PUT /api/institution/{id}/config`

---

## 5. 订单服务 API

### 5.1 创建订单

**接口地址**: `POST /api/order`

**请求头**: 需要 Authorization

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| guaranteeApplyId | Long | 是 | 保函申请ID |
| institutionId | Long | 是 | 机构ID |
| amount | BigDecimal | 是 | 订单金额 |
| currency | String | 否 | 币种，默认CNY |
| payMethod | String | 否 | 支付方式: online/offline，默认online |

### 5.2 订单详情

**接口地址**: `GET /api/order/{id}`

### 5.3 用户订单列表

**接口地址**: `GET /api/order/list`

**请求头**: 需要 Authorization

### 5.4 订单支付

**接口地址**: `POST /api/order/{id}/pay`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tradeNo | String | 否 | 第三方交易号 |

### 5.5 取消订单

**接口地址**: `POST /api/order/{id}/cancel`

### 5.6 退款

**接口地址**: `POST /api/order/{id}/refund`

---

## 6. 出函服务 API

### 6.1 生成保函文件

**接口地址**: `POST /api/issue/{guaranteeId}/generate`

### 6.2 获取保函文档

**接口地址**: `GET /api/issue/{guaranteeId}/document`

### 6.3 获取文档下载URL

**接口地址**: `GET /api/issue/{guaranteeId}/download`

### 6.4 签章文档

**接口地址**: `POST /api/issue/{guaranteeId}/sign`

---

## 7. 文件服务 API

### 7.1 文件上传

**接口地址**: `POST /api/file/upload`

**请求头**: 需要 Authorization

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 上传文件 |
| bizType | String | 否 | 业务类型 |
| bizId | Long | 否 | 业务ID |

### 7.2 文件详情

**接口地址**: `GET /api/file/{id}`

### 7.3 获取文件下载URL

**接口地址**: `GET /api/file/{id}/url`

### 7.4 文件下载

**接口地址**: `GET /api/file/{id}/download`

### 7.5 查询业务文件列表

**接口地址**: `GET /api/file/biz`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bizType | String | 是 | 业务类型 |
| bizId | Long | 是 | 业务ID |

### 7.6 删除文件

**接口地址**: `DELETE /api/file/{id}`

---

## 8. 错误响应示例

```json
{
  "code": 3000,
  "message": "业务错误提示",
  "data": null,
  "timestamp": 1709280000000,
  "traceId": "xxx"
}
```

---

*文档版本历史*
| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|---------|
| V1.0 | 2026-03-13 | 后端开发工程师 | 初始版本 |
