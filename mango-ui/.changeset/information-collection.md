---
'@mango/admin': minor
'@mango/admin-extension': patch
'@mango/information': minor
'@mango/cli': minor
---

新增信息采集能力，提供表单设计、配置、公开填写、提交处理、文件上传和提交记录管理，并接入 Admin 聚合入口与 CLI 模块投影。

- `@mango/information`：发布信息采集前端包及其管理页面、运行时组件和样式。
- `@mango/admin`：将信息采集模块纳入 Admin 聚合包。
- `@mango/admin-extension`：让源码直连开发模式使用稳定的开发版本标识，避免页面注册表因构建期版本变量缺失而返回 404。
- `@mango/cli`：将信息采集模块和版本矩阵纳入生成项目。

业务项目升级后应同步升级 Mango Maven/npm/CLI 完整批次，并按信息采集模块 README 配置菜单、权限、租户和文件存储验证。
