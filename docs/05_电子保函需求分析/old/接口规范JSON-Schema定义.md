# 电子保函系统 JSON Schema 定义

**版本**: V1.0
**更新日期**: 2026-03-13

---

## 1. 通用定义

### 1.1 通用响应包装

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "definitions": {
    "CommonResponse": {
      "type": "object",
      "required": ["code", "message", "timestamp"],
      "properties": {
        "code": {
          "type": "integer",
          "description": "错误码，0表示成功"
        },
        "message": {
          "type": "string",
          "description": "响应消息"
        },
        "data": {
          "type": ["object", "array", "null"],
          "description": "响应数据"
        },
        "timestamp": {
          "type": "integer",
          "description": "时间戳(毫秒)"
        },
        "traceId": {
          "type": "string",
          "description": "链路追踪ID"
        }
      }
    },
    "PageRequest": {
      "type": "object",
      "properties": {
        "pageNum": {
          "type": "integer",
          "minimum": 1,
          "default": 1,
          "description": "页码"
        },
        "pageSize": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100,
          "default": 10,
          "description": "每页数量"
        }
      }
    },
    "PageResponse": {
      "type": "object",
      "required": ["list", "total", "pageNum", "pageSize"],
      "properties": {
        "list": {
          "type": "array",
          "description": "数据列表"
        },
        "total": {
          "type": "integer",
          "description": "总记录数"
        },
        "pageNum": {
          "type": "integer",
          "description": "当前页码"
        },
        "pageSize": {
          "type": "integer",
          "description": "每页数量"
        },
        "pages": {
          "type": "integer",
          "description": "总页数"
        }
      }
    }
  }
}
```

---

## 2. 用户服务 Schema

### 2.1 登录请求

```json
{
  "LoginRequest": {
    "type": "object",
    "required": ["username", "password"],
    "properties": {
      "username": {
        "type": "string",
        "minLength": 3,
        "maxLength": 50,
        "description": "用户名"
      },
      "password": {
        "type": "string",
        "minLength": 6,
        "maxLength": 20,
        "description": "密码"
      }
    }
  }
}
```

### 2.2 登录响应

```json
{
  "LoginResponse": {
    "type": "object",
    "required": ["accessToken", "tokenType", "expiresIn", "userId"],
    "properties": {
      "accessToken": {
        "type": "string",
        "description": "访问令牌"
      },
      "tokenType": {
        "type": "string",
        "enum": ["Bearer"],
        "description": "令牌类型"
      },
      "expiresIn": {
        "type": "integer",
        "description": "过期时间(秒)"
      },
      "userId": {
        "type": "integer",
        "description": "用户ID"
      },
      "username": {
        "type": "string",
        "description": "用户名"
      },
      "realName": {
        "type": "string",
        "description": "真实姓名"
      }
    }
  }
}
```

### 2.3 注册请求

```json
{
  "RegisterRequest": {
    "type": "object",
    "required": ["username", "password"],
    "properties": {
      "username": {
        "type": "string",
        "minLength": 3,
        "maxLength": 50,
        "description": "用户名"
      },
      "password": {
        "type": "string",
        "minLength": 6,
        "maxLength": 20,
        "description": "密码"
      },
      "realName": {
        "type": "string",
        "maxLength": 50,
        "description": "真实姓名"
      },
      "mobile": {
        "type": "string",
        "pattern": "^1[3-9]\\d{9}$",
        "description": "手机号"
      },
      "email": {
        "type": "string",
        "format": "email",
        "description": "邮箱"
      },
      "userType": {
        "type": "integer",
        "enum": [1, 2, 3, 4],
        "description": "用户类型: 1-投标人, 2-招标代理, 3-机构用户, 4-管理员"
      }
    }
  }
}
```

---

## 3. 保函服务 Schema

### 3.1 保函申请请求

```json
{
  "GuaranteeApplyRequest": {
    "type": "object",
    "required": ["guaranteeType", "guaranteeFormat", "projectName", "beneficiaryName", "applicantName", "amount"],
    "properties": {
      "guaranteeType": {
        "type": "integer",
        "enum": [1, 2, 3, 4],
        "description": "保函类型: 1-投标, 2-履约, 3-预付款, 4-质量"
      },
      "guaranteeFormat": {
        "type": "integer",
        "enum": [1, 2],
        "description": "保函格式: 1-独立, 2-连带"
      },
      "projectName": {
        "type": "string",
        "minLength": 1,
        "maxLength": 200,
        "description": "项目名称"
      },
      "projectNo": {
        "type": "string",
        "maxLength": 100,
        "description": "项目编号"
      },
      "beneficiaryName": {
        "type": "string",
        "minLength": 1,
        "maxLength": 200,
        "description": "受益人名称"
      },
      "applicantName": {
        "type": "string",
        "minLength": 1,
        "maxLength": 200,
        "description": "申请人名称"
      },
      "institutionId": {
        "type": "integer",
        "description": "承保机构ID"
      },
      "amount": {
        "type": "number",
        "minimum": 0,
        "exclusiveMinimum": true,
        "description": "保函金额"
      },
      "validityMonths": {
        "type": "integer",
        "minimum": 1,
        "maximum": 60,
        "description": "有效期(月)"
      },
      "startDate": {
        "type": "string",
        "format": "date",
        "description": "生效日期"
      },
      "endDate": {
        "type": "string",
        "format": "date",
        "description": "到期日期"
      }
    }
  }
}
```

### 3.2 保函申请响应

```json
{
  "GuaranteeApplyResponse": {
    "type": "object",
    "required": ["id", "guaranteeNo", "applyStatus"],
    "properties": {
      "id": {
        "type": "integer",
        "description": "保函申请ID"
      },
      "guaranteeNo": {
        "type": "string",
        "description": "保函编号"
      },
      "applyStatus": {
        "type": "integer",
        "description": "申请状态: 0-待提交, 1-待审核, 2-审核通过, 3-审核拒绝, 4-已出函, 5-已退保"
      },
      "guaranteeType": {
        "type": "integer",
        "description": "保函类型"
      },
      "guaranteeFormat": {
        "type": "integer",
        "description": "保函格式"
      },
      "projectName": {
        "type": "string",
        "description": "项目名称"
      },
      "amount": {
        "type": "number",
        "description": "保函金额"
      },
      "applyTime": {
        "type": "string",
        "format": "date-time",
        "description": "申请时间"
      }
    }
  }
}
```

### 3.3 保函详情响应

```json
{
  "GuaranteeDetailResponse": {
    "type": "object",
    "required": ["id", "guaranteeNo", "guaranteeType", "applyStatus"],
    "properties": {
      "id": {
        "type": "integer",
        "description": "保函ID"
      },
      "guaranteeNo": {
        "type": "string",
        "description": "保函编号"
      },
      "guaranteeType": {
        "type": "integer",
        "description": "保函类型"
      },
      "guaranteeFormat": {
        "type": "integer",
        "description": "保函格式"
      },
      "projectName": {
        "type": "string",
        "description": "项目名称"
      },
      "projectNo": {
        "type": "string",
        "description": "项目编号"
      },
      "beneficiaryName": {
        "type": "string",
        "description": "受益人名称"
      },
      "applicantName": {
        "type": "string",
        "description": "申请人名称"
      },
      "institutionName": {
        "type": "string",
        "description": "承保机构名称"
      },
      "amount": {
        "type": "number",
        "description": "保函金额"
      },
      "applyStatus": {
        "type": "integer",
        "description": "申请状态"
      },
      "startDate": {
        "type": "string",
        "format": "date",
        "description": "生效日期"
      },
      "endDate": {
        "type": "string",
        "format": "date",
        "description": "到期日期"
      },
      "issueDate": {
        "type": "string",
        "format": "date",
        "description": "出具日期"
      },
      "documentUrl": {
        "type": "string",
        "description": "保函文档URL"
      },
      "applyTime": {
        "type": "string",
        "format": "date-time",
        "description": "申请时间"
      },
      "auditTime": {
        "type": "string",
        "format": "date-time",
        "description": "审核时间"
      },
      "issueTime": {
        "type": "string",
        "format": "date-time",
        "description": "出函时间"
      }
    }
  }
}
```

### 3.4 保函审核请求

```json
{
  "GuaranteeAuditRequest": {
    "type": "object",
    "required": ["status"],
    "properties": {
      "status": {
        "type": "integer",
        "enum": [2, 3],
        "description": "审核状态: 2-审核通过, 3-审核拒绝"
      },
      "auditOpinion": {
        "type": "string",
        "maxLength": 500,
        "description": "审核意见"
      }
    }
  }
}
```

---

## 4. 机构服务 Schema

### 4.1 机构查询请求

```json
{
  "InstitutionQueryRequest": {
    "type": "object",
    "properties": {
      "pageNum": {
        "type": "integer",
        "minimum": 1,
        "default": 1
      },
      "pageSize": {
        "type": "integer",
        "minimum": 1,
        "maximum": 100,
        "default": 10
      },
      "institutionType": {
        "type": "integer",
        "enum": [1, 2, 3],
        "description": "机构类型: 1-银行, 2-保险, 3-担保公司"
      },
      "status": {
        "type": "integer",
        "enum": [0, 1],
        "description": "状态: 0-禁用, 1-启用"
      },
      "keyword": {
        "type": "string",
        "maxLength": 100,
        "description": "搜索关键词"
      }
    }
  }
}
```

### 4.2 机构详情响应

```json
{
  "InstitutionDetailResponse": {
    "type": "object",
    "required": ["id", "institutionName", "institutionType", "status"],
    "properties": {
      "id": {
        "type": "integer",
        "description": "机构ID"
      },
      "institutionName": {
        "type": "string",
        "description": "机构名称"
      },
      "institutionType": {
        "type": "integer",
        "description": "机构类型"
      },
      "licenseNo": {
        "type": "string",
        "description": "营业执照号"
      },
      "contactPerson": {
        "type": "string",
        "description": "联系人"
      },
      "contactMobile": {
        "type": "string",
        "description": "联系电话"
      },
      "contactEmail": {
        "type": "string",
        "description": "联系邮箱"
      },
      "address": {
        "type": "string",
        "description": "地址"
      },
      "status": {
        "type": "integer",
        "description": "状态"
      },
      "createTime": {
        "type": "string",
        "format": "date-time",
        "description": "创建时间"
      }
    }
  }
}
```

---

## 5. 订单服务 Schema

### 5.1 创建订单请求

```json
{
  "OrderCreateRequest": {
    "type": "object",
    "required": ["guaranteeApplyId", "institutionId", "amount"],
    "properties": {
      "guaranteeApplyId": {
        "type": "integer",
        "description": "保函申请ID"
      },
      "institutionId": {
        "type": "integer",
        "description": "机构ID"
      },
      "amount": {
        "type": "number",
        "minimum": 0,
        "exclusiveMinimum": true,
        "description": "订单金额"
      },
      "currency": {
        "type": "string",
        "default": "CNY",
        "enum": ["CNY", "USD", "EUR"],
        "description": "币种"
      },
      "payMethod": {
        "type": "string",
        "default": "online",
        "enum": ["online", "offline"],
        "description": "支付方式"
      }
    }
  }
}
```

### 5.2 订单详情响应

```json
{
  "OrderDetailResponse": {
    "type": "object",
    "required": ["id", "orderNo", "orderStatus"],
    "properties": {
      "id": {
        "type": "integer",
        "description": "订单ID"
      },
      "orderNo": {
        "type": "string",
        "description": "订单编号"
      },
      "guaranteeApplyId": {
        "type": "integer",
        "description": "保函申请ID"
      },
      "institutionId": {
        "type": "integer",
        "description": "机构ID"
      },
      "institutionName": {
        "type": "string",
        "description": "机构名称"
      },
      "amount": {
        "type": "number",
        "description": "订单金额"
      },
      "currency": {
        "type": "string",
        "description": "币种"
      },
      "payMethod": {
        "type": "string",
        "description": "支付方式"
      },
      "orderStatus": {
        "type": "integer",
        "description": "订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已退款"
      },
      "payTime": {
        "type": "string",
        "format": "date-time",
        "description": "支付时间"
      },
      "createTime": {
        "type": "string",
        "format": "date-time",
        "description": "创建时间"
      }
    }
  }
}
```

### 5.3 订单支付请求

```json
{
  "OrderPayRequest": {
    "type": "object",
    "properties": {
      "tradeNo": {
        "type": "string",
        "maxLength": 100,
        "description": "第三方交易号"
      }
    }
  }
}
```

---

## 6. 错误码定义 Schema

### 6.1 错误码枚举

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| 0 | 成功 | 200 |
| 1000 | 系统内部错误 | 500 |
| 1001 | 数据库异常 | 500 |
| 1002 | 缓存异常 | 500 |
| 1003 | 第三方服务调用失败 | 502 |
| 2000 | 认证失败 | 401 |
| 2001 | Token无效或过期 | 401 |
| 2002 | 权限不足 | 403 |
| 2003 | 用户被禁用 | 403 |
| 3000 | 业务处理失败 | 400 |
| 3001 | 保函不存在 | 404 |
| 3002 | 保函状态不允许此操作 | 400 |
| 3003 | 机构不存在 | 404 |
| 3004 | 机构未启用 | 400 |
| 3005 | 订单不存在 | 404 |
| 3006 | 订单状态不允许支付 | 400 |
| 3007 | 支付金额不匹配 | 400 |
| 3008 | 保函金额超过机构额度 | 400 |
| 3009 | 保函已存在 | 409 |
| 3010 | 申请人信息不匹配 | 400 |
| 3011 | 受益人信息不匹配 | 400 |
| 4000 | 参数校验失败 | 400 |
| 4001 | 必填参数缺失 | 400 |
| 4002 | 参数格式错误 | 400 |
| 4003 | 参数值超出范围 | 400 |
| 4004 | 参数值不支持 | 400 |

### 6.2 错误响应 Schema

```json
{
  "ErrorResponse": {
    "type": "object",
    "required": ["code", "message", "timestamp"],
    "properties": {
      "code": {
        "type": "integer",
        "description": "错误码"
      },
      "message": {
        "type": "string",
        "description": "错误消息"
      },
      "data": {
        "type": "null",
        "description": "错误时data为null"
      },
      "timestamp": {
        "type": "integer",
        "description": "时间戳"
      },
      "traceId": {
        "type": "string",
        "description": "链路追踪ID"
      },
      "details": {
        "type": "array",
        "description": "详细错误信息",
        "items": {
          "type": "object",
          "properties": {
            "field": {
              "type": "string",
              "description": "错误字段"
            },
            "message": {
              "type": "string",
              "description": "字段错误详情"
            }
          }
        }
      }
    }
  }
}
```

---

## 7. 文件服务 Schema

### 7.1 文件上传响应

```json
{
  "FileUploadResponse": {
    "type": "object",
    "required": ["id", "fileName", "fileUrl"],
    "properties": {
      "id": {
        "type": "integer",
        "description": "文件ID"
      },
      "fileName": {
        "type": "string",
        "description": "文件名"
      },
      "fileSize": {
        "type": "integer",
        "description": "文件大小(字节)"
      },
      "fileType": {
        "type": "string",
        "description": "文件MIME类型"
      },
      "fileUrl": {
        "type": "string",
        "description": "文件访问URL"
      },
      "bizType": {
        "type": "string",
        "description": "业务类型"
      },
      "bizId": {
        "type": "integer",
        "description": "业务ID"
      },
      "uploadTime": {
        "type": "string",
        "format": "date-time",
        "description": "上传时间"
      }
    }
  }
}
```

---

## 8. 枚举值参考

### 8.1 保函类型 (guaranteeType)

| 值 | 说明 |
|----|------|
| 1 | 投标保函 |
| 2 | 履约保函 |
| 3 | 预付款保函 |
| 4 | 质量保函 |

### 8.2 保函格式 (guaranteeFormat)

| 值 | 说明 |
|----|------|
| 1 | 独立保函 |
| 2 | 连带保证 |

### 8.3 申请状态 (applyStatus)

| 值 | 说明 |
|----|------|
| 0 | 待提交 |
| 1 | 待审核 |
| 2 | 审核通过 |
| 3 | 审核拒绝 |
| 4 | 已出函 |
| 5 | 已退保 |

### 8.4 机构类型 (institutionType)

| 值 | 说明 |
|----|------|
| 1 | 银行 |
| 2 | 保险 |
| 3 | 担保公司 |

### 8.5 用户类型 (userType)

| 值 | 说明 |
|----|------|
| 1 | 投标人 |
| 2 | 招标代理 |
| 3 | 机构用户 |
| 4 | 管理员 |

### 8.6 订单状态 (orderStatus)

| 值 | 说明 |
|----|------|
| 0 | 待支付 |
| 1 | 已支付 |
| 2 | 已取消 |
| 3 | 已退款 |

---

*文档版本历史*
| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|---------|
| V1.0 | 2026-03-13 | CTO | 初始版本，定义JSON Schema |
