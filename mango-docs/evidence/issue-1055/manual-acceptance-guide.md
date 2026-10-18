# Issue #1055 信息采集中心 人工验收指引

> 用于产品负责人 / 技术负责人按 BRD、SRS 和原型逐页面、逐功能复核。自动化证据见同目录 `acceptance-evidence.md`，本文件只提供手工复核入口。

## 1. 环境信息

| 项目 | 地址或值 |
|---|---|
| 管理端 | `http://127.0.0.1:30084` |
| 后端 | `http://127.0.0.1:18084` |
| 托管表单（匿名） | `http://127.0.0.1:30084/#/information/hosted?formCode=customer-contact` |
| 表单管理（唯一菜单） | `http://127.0.0.1:30084/#/information/forms` |
| 表单详情 | `http://127.0.0.1:30084/#/information/forms/<formId>/detail` |
| 表单配置 | `http://127.0.0.1:30084/#/information/forms/<formId>/config` |
| 接入方式 | `http://127.0.0.1:30084/#/information/forms/<formId>/hosted` |
| 提交记录 | `http://127.0.0.1:30084/#/information/forms/<formId>/submissions` |
| 数据库 | `mango_dev_mango_information_collection_084`，`127.0.0.1:3306`，用户 `root`，空密码 |
| 登录账号 | `admin` / `admin123`，机构选择「芒果集团」 |
| 租户 | `tenantId=1` / `tenantCode=default` |

当前测试表单：`customer-contact` / 「客户联系表」/ 已发布 / 3 个字段，`formId=2111647649335427074`。

原型对照：`mango-docs/designs/issue-1055-information-collection/prototype.html`；逐页截图见 `screenshots/01`~`09`。

分享给访客的地址形如 `http://127.0.0.1:30084/#/information/hosted?formCode=customer-contact`，**无需登录即可提交**；接入页「复制页面地址」复制的就是该绝对地址。

## 2. 表单管理

| 编号 | 操作 | 预期结果 |
|---|---|---|
| A1 | 打开表单管理，观察列表 | 侧边菜单「信息采集中心」下只有「表单管理」；列与原型一致：表单名称 / 表单编码 / 备注 / 状态 / 提交量 / 创建日期 / 更新日期 / 创建人 / 操作 |
| A2 | 点击「详情」 | 进入表单详情页：返回列表页返回栏 + 接入方式/提交记录/配置表单按钮；3 列描述表（表单名称/编码/备注/提交量/创建日期/更新日期）；字段配置表（字段标签/编码/类型/必填/提示文字/排序）；接入方式两张卡片 |
| A3 | 点击「配置」 | 进入可视化配置页：左侧「添加控件」工具箱（基础字段/选择字段/日期与附件/展示组件），中间表单预览画布，右侧「表单设置」「字段属性」「当前字段」面板和接入方式提示卡 |
| A4 | 不填字段标签或制造重复字段编码后保存 | 前端提示具体原因，不发起保存 |
| A5 | 新建表单时使用已存在编码 `customer-contact` | 后端拒绝并提示「信息采集表单编码已存在」（错误码 105501） |
| A6 | 删除「客户联系表」 | 因为已有提交记录，提示「已有提交记录的表单不可删除」（错误码 105505） |
| A7 | 新增表单并选择「停用」，再匿名打开托管地址 | 停用表单不可公开访问，提示表单未发布 |
| A8 | 点击某行「接入」 | 进入接入方式页：左侧浏览器框内预览托管表单，右侧使用方式说明与「浏览器页面/托管页面」「服务端 API」两张卡片，可复制地址、复制 SK、重新生成密钥 |

## 3. 提交记录

| 编号 | 操作 | 预期结果 |
|---|---|---|
| B1 | 打开提交记录，观察列表 | 工具栏左侧「返回详情页」+ 表单名称与编码，右侧「N 条待处理」与「导出记录」；列为提交编号 / 提交时间 / 状态 / 来源页面 + 当前表单动态采集项列 + 操作 |
| B10 | 换一张字段不同的表单进入提交记录 | 列表列随该表单字段定义变化，只显示本表单采集项；展示型控件（说明/分割线）不生成列；选择型控件显示选项标签 |
| B2 | 观察「导出当前筛选」按钮初始状态 | 有数据时可点击 |
| B3 | 点击「详情」 | 右侧抽屉展示提交详情：标题为第一个采集项取值、时间 · 来源 · 匿名提交、状态、脱敏后的字段卡片、个人信息说明、处理备注与处理状态 |
| B4 | 在抽屉内选择「已完成」并点击「保存并更新」 | 抽屉关闭，列表状态变为「已完成」 |
| B5 | 在抽屉内选择「已关闭」后保存 | 状态变为「已关闭」，再次打开抽屉时「保存并更新」不可用，接口返回已关闭错误 |
| B6 | 按状态筛选「待处理」再查询 | 列表只返回待处理记录，分页总数同步变化 |
| B7 | 按关键字筛选提交编号片段 | 列表按提交编号匹配 |
| B8 | 点击「导出记录」 | 下载 CSV，列包含提交编号、提交时间、状态、来源页面和当前表单采集项；手机号形如 `136****00`，不含完整号码 |
| B9 | 观察列表与抽屉中的手机号 | 列表和抽屉同样脱敏为 `136****00`，与导出规则一致 |

## 4. 托管表单（匿名访客）

| 编号 | 操作 | 预期结果 |
|---|---|---|
| C1 | 用无痕窗口直接打开接入页复制的分享地址（不登录） | 直接渲染带 `MANGO · ENTERPRISE` 品牌行的「客户联系表」表单，不跳转登录页，不出现管理端菜单外壳 |
| C2 | 只填姓名后提交 | 手机号触发必填校验，不提交 |
| C3 | 填写姓名、手机号、需求描述后提交 | 弹出「提交成功」结果弹窗，提示语取自表单配置 |
| C4 | 提交后回到提交记录列表刷新 | 新增一条「待处理」记录，来源页面为托管表单 URL |
| C5 | 刷新已提交页面或重复点击提交 | 幂等键命中，不产生重复记录 |
| C6 | 打开 `/#/information/hosted?formCode=不存在` | 显示「表单暂不可用」和原因，不出现空白页 |

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
