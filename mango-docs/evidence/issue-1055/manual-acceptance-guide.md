# Issue #1055 信息采集中心 人工验收指引

> 用于产品负责人 / 技术负责人按 BRD、SRS 和原型逐页面、逐功能复核。自动化证据见同目录 `acceptance-evidence.md`，本文件只提供手工复核入口。

## 1. 环境信息

| 项目 | 地址或值 |
|---|---|
| 管理端 | `http://127.0.0.1:30084` |
| 后端 | `http://127.0.0.1:18084` |
| 托管表单（匿名） | `http://127.0.0.1:30084/#/information/hosted?formCode=customer-contact` |
| 表单管理 | `http://127.0.0.1:30084/#/information/forms` |
| 提交记录 | `http://127.0.0.1:30084/#/information/submissions` |
| 数据库 | `mango_dev_mango_information_collection_084`，`127.0.0.1:3306`，用户 `root`，空密码 |
| 登录账号 | `admin` / `admin123`，机构选择「芒果集团」 |
| 租户 | `tenantId=1` / `tenantCode=default` |

当前测试表单：`customer-contact` / 「客户联系表」/ 已发布 / 3 个字段，`formId=2111647649335427074`。

## 2. 表单管理

| 编号 | 操作 | 预期结果 |
|---|---|---|
| A1 | 打开表单管理，观察列表 | 侧边菜单「信息采集中心 → 表单管理」高亮；列表显示「客户联系表 / customer-contact / 字段数 3 / 提交数 / 状态 已发布 / 更新时间」 |
| A2 | 点击「详情」 | 弹窗展示表单名称、编码、托管入口（可点击）、服务端 SK（只读，可复制）、格式化后的字段定义 |
| A3 | 点击「配置」 | 弹窗展示表单编码（编辑时禁用）、名称、说明、字段定义 JSON、成功提示、发布开关 |
| A4 | 把字段定义改成非法 JSON 后保存 | 前端提示「字段定义必须是合法 JSON」，不发起保存 |
| A5 | 新建表单时使用已存在编码 `customer-contact` | 后端拒绝并提示「信息采集表单编码已存在」（错误码 105501） |
| A6 | 删除「客户联系表」 | 因为已有提交记录，提示「已有提交记录的表单不可删除」（错误码 105505） |
| A7 | 新建一张表单、保存为草稿，然后匿名打开其托管地址 | 草稿表单不可公开访问，提示表单未发布 |

## 3. 提交记录

| 编号 | 操作 | 预期结果 |
|---|---|---|
| B1 | 打开提交记录，观察列表 | 固定列：提交编号、提交时间、状态；中间为当前表单动态列（姓名、手机号、需求描述）；末尾来源页面和操作列 |
| B2 | 观察「导出当前筛选」按钮初始状态 | 有数据时可点击 |
| B3 | 点击「详情」 | 弹窗展示提交编号、状态、来源页面、格式化后的提交内容、处理备注 |
| B4 | 对一条「待处理」记录点击「完成」 | 确认后状态变为「已完成」 |
| B5 | 对同一条记录点击「关闭」 | 确认后状态变为「已关闭」，且该行不再显示「完成 / 关闭」按钮 |
| B6 | 按状态筛选「待处理」再查询 | 列表只返回待处理记录，分页总数同步变化 |
| B7 | 按关键字筛选提交编号片段 | 列表按提交编号匹配 |
| B8 | 点击「导出当前筛选」 | 下载 CSV，列包含提交编号、提交时间、状态、来源页面和当前表单采集项；手机号形如 `138****00`，不含完整号码 |

## 4. 托管表单（匿名访客）

| 编号 | 操作 | 预期结果 |
|---|---|---|
| C1 | 用无痕窗口直接打开托管表单地址（不登录） | 直接渲染「客户联系表」表单，不跳转登录页，不出现管理端菜单外壳 |
| C2 | 只填姓名后提交 | 手机号触发必填校验，不提交 |
| C3 | 填写姓名、手机号、需求描述后提交 | 展示「提交成功」结果页，提示语取自表单配置 |
| C4 | 提交后回到提交记录列表刷新 | 新增一条「待处理」记录，来源页面为托管表单 URL |
| C5 | 刷新已提交页面或重复点击提交 | 幂等键命中，不产生重复记录 |

## 5. 服务端 API 接入

当前表单专属 SK：`pWE5WUNzMU7E6Y4jn0wtueyXx6vX5tH8P`（可在表单详情复制核对）。

```bash
# 正确 SK 提交
curl -s -X POST http://127.0.0.1:18084/information/open/forms/submit \
  -H 'Content-Type: application/json' \
  -d '{"formCode":"customer-contact","values":{"name":"服务端测试","phone":"13500135000","need":"人工验收"},"sourcePage":"manual-check","serverSecret":"pWE5WUNzMU7E6Y4jn0wtueyXx6vX5tH8P"}'

# 错误 SK 应被拒绝（105507）
curl -s -X POST http://127.0.0.1:18084/information/open/forms/submit \
  -H 'Content-Type: application/json' \
  -d '{"formCode":"customer-contact","values":{"name":"x","phone":"1"},"serverSecret":"wrong"}'
```

| 编号 | 操作 | 预期结果 |
|---|---|---|
| D1 | 使用正确 SK 提交 | 返回 `submissionNo` 与成功提示语 |
| D2 | 使用错误 SK 提交 | 返回 `105507 服务端密钥无效`，且不产生记录 |
| D3 | 匿名 `GET /information/open/forms/detail?formCode=customer-contact` | 返回表单名称、说明、字段定义和成功提示，不包含 SK |
| D4 | 匿名调用后台接口 `GET /information/forms/page` | 返回 HTTP 401，后台接口不对匿名开放 |

## 6. 数据核对

```bash
mysql -h127.0.0.1 -P3306 -uroot mango_dev_mango_information_collection_084 -e "
SELECT submission_no, tenant_id, status, source_page FROM information_submission ORDER BY id DESC LIMIT 5;
SELECT submission_id, tenant_id, LEFT(definition_json, 60) FROM information_submission_snapshot ORDER BY id DESC LIMIT 5;"
```

| 编号 | 检查点 | 预期结果 |
|---|---|---|
| E1 | 提交记录 `tenant_id` | 均为 `1`，由服务端写入，请求体无法指定租户 |
| E2 | 提交时字段快照 | 每条提交对应一条快照，保存当时字段定义 |
| E3 | 修改表单字段名称后回看旧记录 | 旧记录仍按提交时快照的编码和名称展示，列不错位 |

## 7. 已知限制

- 产品与技术负责人的正式人工审批尚未完成；本指引用于该审批的复核输入。
- 文件上传按 BRD/SRS 为 P1，本次未实现，表单只支持文本、手机号、下拉和多行文本。
- 托管表单当前注册在 `mango-admin` 宿主应用内；独立对外的托管站点承载应用待确认。
- 服务重启命令：`node mango-ui/packages/mango-cli/src/index.mjs dev start backend`、`... dev start mango-admin`；停止用 `dev stop`。
