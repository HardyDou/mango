# 业务接入场景手册

## 1. 定位

本文面向基于 Mango 开发业务系统的开发者，用场景方式串起模块 README、能力地图和常见排障入口。

这里记录接入路径和检查点，不承载长期规范；正式交付规则以 PMO preflight 输出和 `mango-pmo/rules/**` 为准。

## 2. 使用方式

1. 先按业务目标选择场景。
2. 按阅读顺序打开模块 README，确认后端 starter、前端包、配置、菜单和验证入口。
3. 按场景检查点完成业务代码接入。
4. 用模块 README 的验证命令和场景验收点一起验证。

## 3. 场景索引

| 场景 | 适合问题 | 主要能力 |
|------|----------|----------|
| [文件上传表单](./file-upload-form.md) | 业务表单上传附件、回显、预览、删除失败 | File、Fileproc、File Preview、Frontend File |
| [业务审批接入](./workflow-business-approval.md) | 业务单据发起审批、处理审批结果、查看流程记录 | Workflow、Workflow Frontend、Workflow Example |
| [菜单页面打不开排障](./rbac-menu-page-troubleshooting.md) | 登录后菜单空白、404、页面组件找不到 | Authorization、RBAC、Admin Shell |
| [按钮权限不显示排障](./permission-button-troubleshooting.md) | 菜单可见但新增、编辑、删除按钮不显示 | Authorization、Access、RBAC Frontend |
| [租户字典配置为空排障](./tenant-dict-config-empty.md) | 业务下拉、字典、配置或基础数据为空 | Identity、Org、System、Seed |

## 4. 关联入口

- [Mango 能力地图](../../capabilities/README.md)
- [业务项目开发指南](../../designs/business-project-development-guide.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [文档资产归档边界](../../../mango-pmo/rules/06-document-assets.md)
