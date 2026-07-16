# 业务域历史债务治理验收证据

## 1. 验收范围

- 页面：平台能力 → 业务域，路由 `/#/data/domain`。
- 接口：`/domain/domains` CRUD、状态、树、编码详情和启用树接口。
- 权限：租户 1 的 `admin` 通过 Demo 资源中的 `ROLE_ADMIN` 进入真实菜单和接口链路。
- 数据：全新隔离库 `mango_dev_mango_domain_debt_192`；E2E 使用 `E2E_DOMAIN_<timestamp>`，结束后无未删除测试记录。
- 部署形态：Mango 单体后端与 `mango-admin` 源码模式。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30192/#/data/domain`。
- 后端地址：`http://127.0.0.1:18192`。
- 数据库或租户：`mango_dev_mango_domain_debt_192`，租户 ID `1`。
- 测试账号：`admin`；未记录密码、令牌或密钥。
- 浏览器：Playwright Chromium。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | 登录、用户菜单、`/#/data/domain` | 真实入口 | 租户 1 的 `admin` | 登录响应成功，菜单包含 `data:domain`，父菜单为 `data` | 页面显示业务域表格、新增按钮和通用域 | 无 console error、pageerror、请求失败或 4xx/5xx | `.mango/run/logs/mango-backend.log` | PASS |
| TASK-002 | TC-001 | `POST /domain/domains` | 新增顶级业务域 | `E2E_DOMAIN_<timestamp>` | HTTP 与业务响应成功，数据库和列表出现新记录 | 新增弹窗字段可填写，保存成功消息和列表回显正确 | POST 为 200；未捕获 console error、pageerror、requestfailed 或 4xx/5xx | `mango-ui/apps/mango-admin/test-results/**/domain-management-final.png` | PASS |
| TASK-003 | TC-001 | `POST /domain/domains` | 新增下级业务域 | `<parent>_PAY` | 最终编码按父域和本层编码生成，父子关系正确 | “新增下级”入口、最终编码禁用回显和列表树正确 | POST 为 200；浏览器异常采集数组为空 | `mango-ui/apps/mango-admin/test-results/**/domain-management-final.png` | PASS |
| TASK-004 | TC-001 | `PUT /domain/domains`、`PUT /domain/domains/status` | 编辑与启停 | 顶级 E2E 业务域 | 名称更新；停用后启用树不返回，重新启用后恢复 | 编辑弹窗编码禁用；状态按钮和成功消息正确切换 | PUT 为 200；浏览器异常采集数组为空 | `mango-ui/apps/mango-admin/test-results/**/domain-management-final.png` | PASS |
| TASK-005 | TC-001 | `DELETE /domain/domains` | 删除子域和父域 | 本用例创建的两条记录 | 两次删除成功；活动测试记录数为 0 | 删除确认框、成功消息和列表移除正确 | DELETE 为 200；未出现接口 4xx/5xx 或请求失败 | `mango-ui/apps/mango-admin/test-results/**/domain-management-final.png` | PASS |
| TASK-006 | TC-001 | `/domain/domains/enabled-tree` | 启用域下拉契约 | 正式 `COMMON` 与 E2E 域 | `COMMON` 始终存在，E2E 域严格随状态变化 | 页面状态与接口结果一致 | GET 为 200 且业务成功；浏览器异常采集数组为空 | Playwright HTML 输出、`.mango/run/logs/mango-backend.log` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| mango-domain | 平台能力 → 业务域 | 菜单与树列表 | CRUD、启停、删除保护 | 表格、弹窗、禁用字段、提示消息正常 | `mango-ui/apps/mango-admin/test-results/**/domain-management-final.png` | PASS |

后端同一测试入口在生产改造前为 8 条通过、2 条契约保护测试失败，失败分别锁定 API 分页参数缺少级联校验和 Feign 查询对象缺少 `SpringQueryMap`。改造后同一入口 10/10 通过：

```bash
mvn -f mango/pom.xml \
  -pl :mango-domain-api,:mango-domain-core,:mango-domain-starter-remote,:mango-domain-starter \
  test
```

补充验证结果：

- Domain 定向架构扫描：104 个阻断问题降至 0；dependency、ArchUnit、PMD、blocking 均为 0。
- changed/no-new-violations：新增问题 0；未修改文件中的 5 个存量基线问题未增加。
- 当前 Domain 生产者与 9 个直接/能力消费者在同一 Maven reactor 编译：13/13 模块成功。
- 全新库 Flyway：baseline 与 `V1__init_domain.sql` 均成功；`data:domain` 实际挂载在 `data` 下。
- 浏览器执行：1/1 通过，11.8 秒；结束后 `deleted=0` 的 E2E 记录为 0。

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| Firefox 与 WebKit | 日常定向验收按规范使用 Chromium | 不影响本次业务逻辑、接口和 Chromium 管理端结论；跨浏览器差异留给夜间套件 | 主干夜间或发布前执行多浏览器回归 | 当前任务采用既定 Payment 策略的定向 Chromium E2E |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | Domain API/Controller/Feign/CRUD 基线与真实页面闭环 | 本报告、`mango-domain/README.md`、`domain-management.spec.ts` | 上述 Maven 命令；Playwright 执行 `domain-management.spec.ts --project=chromium` | 隔离新库、租户 1、动态 `E2E_DOMAIN_` 数据并清理 | 先检查 Demo 角色是否显式开启，再查看浏览器错误、请求状态和后端日志 | PASS |
