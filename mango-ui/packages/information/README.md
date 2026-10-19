# @mango/information

Mango 信息采集管理前端包，提供表单管理、提交记录、托管表单和类型化 API。后台页面通过 `@mango/admin-extension` 注册，公开托管表单不使用服务端 SK。

## 页面

| 页面     | 页面 key                        | 说明                                                                |
| -------- | ------------------------------- | ------------------------------------------------------------------- |
| 表单管理 | `information/forms/index`       | 表单列表，行操作只保留详情、配置、提交记录、停用、删除              |
| 表单详情 | `information/forms/detail`      | 表单摘要；接入方式分左右两栏：左页面（地址 + 效果预览 + iframe 嵌入代码）、右接口（接口地址 + 专属 SK + cURL/Postman/Java/JavaScript/Python 示例 + 字段配置） |
| 表单设计 | `information/forms/config`      | 复用平台表单设计器 `form-create-designer`（与工作流表单设计器同源） |
| 提交记录 | `information/submissions/index` | 独立菜单入口，按表单筛选、动态列与处理抽屉                          |
| 托管表单 | `information/hosted/index`      | 匿名公开表单，无需登录                                              |

表单定义以 form-create 规则 JSON 存储，历史 `{fieldCode, fieldName, fieldType}` 形状会被自动升级。
