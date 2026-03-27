# 供应商管理模块 PRD

## 1. 用户故事

作为 **采购人员**
我想要 **管理供应商信息**
以便 **维护稳定的供应链**

---

## 2. 功能描述

### 2.1 功能列表

| 功能 | 说明 |
|------|------|
| 供应商列表 | 分页展示供应商，支持搜索 |
| 供应商详情 | 查看供应商详细信息 |
| 新增供应商 | 录入新供应商信息 |
| 编辑供应商 | 修改供应商信息 |
| 删除供应商 | 软删除供应商 |

### 2.2 核心业务流程

```
新增供应商 → 审核通过 → 启用状态 → 可被采购订单引用
```

---

## 3. 字段设计

### 3.1 供应商主表 (supplier)

| 字段 | 类型 | 长度 | 必填 | 校验规则 |
|------|------|------|------|---------|
| id | Long | - | 是 | 主键自增 |
| name | String | 3-100 | 是 | 非空，唯一 |
| code | String | 10 | 是 | 唯一编码 |
| contact | String | 50 | 是 | 联系人姓名 |
| mobile | String | 11 | 是 | 手机号格式 |
| email | String | 100 | 否 | 邮箱格式 |
| address | String | 200 | 否 | 详细地址 |
| status | Integer | 1 | 是 | 0=禁用 1=启用 |
| deleted | Integer | 1 | 是 | 0=未删 1=已删 |
| created_at | DateTime | - | 是 | 创建时间 |
| updated_at | DateTime | - | 是 | 更新时间 |

---

## 4. API 设计

### 4.1 供应商列表

**请求**
```
GET /supplier/supplier/list
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 页大小，默认10 |
| name | String | 否 | 供应商名称（模糊搜索） |
| status | Integer | 否 | 状态筛选 |

**响应**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "北京科技有限公司",
        "code": "SUP001",
        "contact": "张三",
        "mobile": "13800138000",
        "status": 1
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  }
}
```

### 4.2 新增供应商

**请求**
```
POST /supplier/supplier
```

**请求体**
```json
{
  "name": "北京科技有限公司",
  "code": "SUP001",
  "contact": "张三",
  "mobile": "13800138000",
  "email": "zhangsan@example.com",
  "address": "北京市朝阳区xxx"
}
```

**响应**
```json
{
  "code": 0,
  "message": "新增成功",
  "data": {
    "id": 1
  }
}
```

---

## 5. 数据库设计

### 5.1 表结构

```sql
CREATE TABLE `supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '供应商名称',
  `code` varchar(10) NOT NULL COMMENT '供应商编码',
  `contact` varchar(50) NOT NULL COMMENT '联系人',
  `mobile` varchar(11) NOT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记 0未删 1已删',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';
```

---

## 6. UI/UX 规范

### 6.1 页面列表

| 页面 | 路由 | 说明 |
|------|------|------|
| 供应商列表 | /supplier/list | 主列表页 |
| 供应商详情 | /supplier/detail/:id | 详情页 |
| 供应商表单 | /supplier/form | 新增/编辑 |

### 6.2 组件使用

| 字段 | 组件 | 说明 |
|------|------|------|
| 列表 | MTable | 需支持分页、搜索 |
| 状态 | MTag | success=启用 danger=禁用 |
| 操作 | MButton | primary=主按钮 |
| 表单 | MForm | 配合 MInput |
| 弹窗 | MDialog | 确认操作 |

### 6.3 交互规范

- 删除操作需 MDialog 确认
- 表单校验失败显示 MTag(danger) 错误信息
- 成功操作后 Toast 轻提示

---

## 7. 业务流程

### 7.1 新增供应商

```
1. 点击"新增供应商"按钮
2. 填写供应商表单
3. 点击"保存"按钮
4. 后端校验:
   - 编码唯一性校验
   - 手机号格式校验
5. 保存成功 → 返回列表页
6. 失败 → 显示错误信息
```

### 7.2 删除供应商（软删除）

```
1. 点击"删除"按钮
2. MDialog 确认: "确定删除该供应商？"
3. 点击"确定"
4. 后端执行软删除 (deleted=1)
5. 刷新列表
```

---

## 8. 边界情况

| 场景 | 处理方式 |
|------|---------|
| 供应商编码已存在 | 返回错误: "供应商编码已存在" |
| 手机号格式错误 | 前端实时校验，提示格式要求 |
| 删除已被订单引用的供应商 | 返回错误: "该供应商已被订单引用，无法删除" |
| 网络异常 | Toast 显示"网络异常，请重试" |
| 列表为空 | 显示空状态插图 |

---

## 9. 非功能需求（可选）

- 响应时间: < 200ms
- 并发支持: ≥ 100 TPS
