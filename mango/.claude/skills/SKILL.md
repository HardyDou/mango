---
name: mango-gen-menu
description: "当用户要求新增页面/模块，需要插入菜单时执行此 skill。根据页面信息生成菜单 JSON 并追加到后端数据库或前端配置文件。"
---

# Mango Menu Generator

当 AI Agent 为 mango-web 新增页面/模块时，使用此 skill 快速生成菜单配置。

## 输入

用户提供：
- **页面名称**（如：用户管理）
- **路由路径**（如：/system/user）
- **组件路径**（如：@/views/system/user/index.vue）
- **父菜单**（如：系统管理）
- **菜单元数据**（icon, sort, 是否需要验证码等）

## 输出

生成两种菜单配置：

### 1. 后端 SQL/JSON（推荐）

生成 `INSERT INTO sys_menu (...) VALUES (...)` SQL 或 JSON：

```json
{
  "parentId": 1,
  "menuType": 2,
  "menuName": "用户管理",
  "menuCode": "system:user",
  "path": "/system/user",
  "component": "@/views/system/user/index.vue",
  "icon": "User",
  "sort": 1,
  "status": 1,
  "visible": 1,
  "keepAlive": 0,
  "embedded": 0,
  "permissions": "system:user:add,system:user:edit,system:user:delete"
}
```

### 2. 前端菜单配置（如果使用静态 JSON）

追加到 `mango-web/src/config/menu.json`：

```json
{
  "path": "/system/user",
  "name": "User",
  "meta": {
    "title": "用户管理",
    "icon": "User"
  },
  "component": "@/views/system/user/index.vue"
}
```

## 执行步骤

1. **分析页面信息**
   - 确认路由路径和组件路径
   - 确定父菜单（查现有菜单树）
   - 计算 sort 值（同级最后一个 +1）

2. **生成菜单配置**
   - 生成 menuCode：`: ` 连接（如 `system:user`）
   - 选择合适的 icon（Element Plus icon name）
   - permissions 用 `menuCode` 作为基础，生成 CRUD 权限

3. **插入数据库**
   - 调用后端 API：`POST /menu/import`
   - 或执行生成的 SQL

4. **验证**
   - 确认菜单已插入：`GET /menu/list`
   - 确认前端能正确渲染

## 菜单类型说明

| menuType | 说明 | 示例 |
|----------|------|------|
| 1 | 目录 | 系统管理 |
| 2 | 菜单 | 用户管理 |
| 3 | 按钮 | 添加用户 |

## 验证码配置（可选）

如果页面需要验证码：

```json
{
  "meta": {
    "captcha": {
      "type": "ARITHMETIC",
      "required": true
    }
  }
}
```

## 权限标识规范

```
{model}:{module}:{action}
```

示例：
- `system:user:view` - 查看
- `system:user:add` - 新增
- `system:user:edit` - 修改
- `system:user:delete` - 删除
