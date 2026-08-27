# 系统管理日志日期查询修复交付记录

## 1. 元数据

- 任务 ID：system-log-date-query
- 交付模式：STANDARD
- 需求影响：L2 - 登录日志、操作日志等服务入口的日期筛选当前绑定失败，并可能漏查结束日数据
- 方案风险：L2 - 修改共享 Web 参数转换、公共前端工具和多个业务 API 参数组装
- 最终风险：L2
- 工作区决策：REUSE

## 2. 目标与范围

- 目标：统一兼容日期字符串到 `LocalDateTime` 的请求绑定，并让所有已识别的日期范围 API 在发送前包含完整结束日。
- 成功条件：`yyyy-MM-dd` 和 `yyyy-MM-dd HH:mm:ss` 均可绑定；日期范围开始日为 `00:00:00`、结束日为 `23:59:59`；完整日期时间和 `LocalDate` 业务字段不被改写。
- 处理范围：`mango-infra-web-starter` 日期转换；`@mango/common` 公共日期范围工具；系统日志、通知、任务执行实例、工作流业务申请以及旧 `mango-admin` 日志 API。
- 不处理范围：仅展示时间的页面、后端使用 `LocalDate` 的日历页面、公告保存时间、数据库结构、日志权限和日志保留策略。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | Spring MVC 查询参数 | `LocalDateTime` 字段接收 `yyyy-MM-dd` | 转换为当天 `00:00:00` | 非法日期仍按标准转换错误处理 | 转换单测通过 |
| AC-002 | 日期范围 API（系统日志、通知、任务、工作流） | 页面或调用方返回两个日期 | 请求边界为 `00:00:00` 和 `23:59:59` | 完整时间输入和非日期范围字段不被改写 | API 参数回归通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001 | 公共 Web MVC 注册 `String -> LocalDateTime` 输入转换，保留既有输出格式 | `mango-infra-web-starter` | 回退该转换注册 |
| TD-002 | AC-002 | 公共前端工具按默认或显式字段名补齐日期范围边界，完整时间原样保留 | `mango-ui/packages/common/utils/date-range.ts` 及各业务 API | 回退各 API 的参数归一化调用 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | WebAutoConfiguration、WebAutoConfigurationTest | 日期和日期时间均可转换 |
| IM-002 | TD-002 | 2 | common、system、notice、job、workflow API 及前端回归测试 | 默认和自定义日期字段均使用正确边界 |
| IM-003 | TD-001/TD-002 | 3 | 启动服务并执行真实请求验证 | 服务健康且无日期绑定错误 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001 | 公共转换和 Controller 绑定测试 | `mvn -f mango/pom.xml -pl :mango-infra-web-starter -Dtest=WebAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`；`mvn -f mango/pom.xml -pl :mango-system-starter -am -Dtest=SystemLogDateBindingTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | 公共转换 4/4、系统日志绑定 2/2；日期、空格日期时间和 ISO 日期时间均可绑定 |
| AC-002 | 多包 API 参数测试、构建和真实服务请求 | 在 `mango-ui` 执行 `pnpm --filter @mango/common test`、`@mango/system test`、`@mango/notice test`、`@mango/job test`、工作流日期定向测试；分别执行五个包 `build`；登录后请求 `/system/log/login/my/list?startTime=2026-08-27&endTime=2026-08-27` | PASS | common 317/317、system 4/4、notice 51/51、job 10/10、workflow 日期 2/2 通过；五个包构建通过；系统日志 API 断言开始日为 `00:00:00`、结束日为 `23:59:59`；真实请求返回 HTTP 200 |

## 7. 运行环境与剩余风险

- 后端运行于 `http://127.0.0.1:18105`，健康检查为 `UP`；前端运行于 `http://127.0.0.1:30105`；任务数据库为 `mango_dev_mango_fix_system_log_date_105`。
- 当前本地权限基线访问系统日志管理接口返回 HTTP 403，因此未取得管理页面完整浏览器 E2E 证据；该限制不影响 Controller 绑定、前端请求参数和已登录个人登录日志真实请求的验证结论。
- 本机 Firefox、WebKit 浏览器未安装，未执行这两个浏览器项目。
- 全局 Mango CLI `1.0.94` 与当前仓库的 `processMode` 不兼容；本次使用仓库内 CLI 启动并完成验证。
- 扫描结论：系统登录日志、操作日志是当前确认的纯日期到 `LocalDateTime` 页面问题；任务执行实例和工作流业务申请是无日期控件但 API 可接收日期-only 的潜在入口，已统一处理；通知现有页面已使用完整日期时间，公共 API 同时具备兼容处理。

## 8. 能力说明

- 已更新 `@mango/common`、`@mango/system`、`@mango/notice`、`@mango/job`、`@mango/workflow` 和 `mango-infra-web` README，说明日期-only 查询边界兼容行为。
- 已更新系统基础数据排障和工作流业务审批指南；未新增菜单、权限、租户或数据库初始化能力。
