# 标准交付记录

Issue #1134 验证码行为统一

## 1. 元数据

- 任务 ID：Issue #1134
- 交付模式：STANDARD
- 需求影响：L2 - 影响登录和表单场景使用的公共验证码 API、前端组件和兼容消费者。
- 方案风险：L2 - 收敛后端生成/校验路径并改变默认题型、字符集和干扰线配置，涉及 API、Feign、前端组件和跨模块兼容。
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/mango-issue-1134`
- 分支：`feat/issue-1134-captcha-unify`
- 评审选择：AGENT_ONLY；未指定外部同行评审，保留后续外部评审入口。

## 2. 目标与范围

- 目标：推荐使用 `GET /captcha/arithmetic`，并让它与兼容入口 `GET /captcha/graphic` 共享图形验证码策略。
- 成功条件：两个入口共享生成、配置、干扰线和校验行为；默认使用英文字母字符集和 1 条干扰线；旧图形消费者继续可用。
- 处理范围：captcha API、core service/generator、starter Controller、Feign、Common API/组件、Admin 示例、E2E 契约和能力 README。
- 不处理范围：不删除 `/captcha/graphic`，不新增接口，不改变短信、邮件、滑块和点选验证码协议，不发布 Maven/npm 制品。

## 3. 可观察系统要求

| ID | 参与者或入口 | 预期行为 | 验收标准 |
|---|---|---|---|
| AC-001 | `GET /captcha/arithmetic` | 按统一图形策略生成图片，响应保留 `ARITHMETIC` 类型兼容标识 | 默认 `graphicType=ALPHANUMERIC`，正确答案可一次性校验 |
| AC-002 | `GET /captcha/graphic` | 保留已有请求协议和消费者兼容性 | 默认策略改为英文字母验证码，并标记新接入不建议使用 |
| AC-003 | `POST /captcha/verify` | 两个入口生成的会话均按服务端保存答案校验 | 正确答案成功并删除会话，错误答案失败 |
| AC-004 | 前端公共组件 | `/captcha/arithmetic` 组件显示普通图片验证码输入提示 | 不再显示计算题/计算结果文案 |
| AC-005 | 配置 | 图形入口共享字符集、尺寸、难度和干扰线配置 | 默认字符集为 `ABCDEFGHJKLMNPQRSTUVWXYZ`，干扰线默认 1 条 |

## 4. 技术决定

| ID | 决定 | 影响路径 | 回滚方式 |
|---|---|---|---|
| TD-001 | `CaptchaService.generateArithmetic()` 复用统一图形会话生成逻辑，保留 `ARITHMETIC` 响应和会话类型 | `mango-captcha-core` | 恢复 arithmetic 独立分支 |
| TD-002 | `/captcha/graphic` 继续保留，并通过 API、Controller、Feign 和前端注释标记为兼容入口 | captcha API/starter/remote/Common | 取消废弃标记不影响接口路径 |
| TD-003 | 默认图形策略为 `ALPHANUMERIC` 兼容类型、英文字母字符集和 1 条干扰线 | `GraphicCaptchaProperties`、README | 修改配置默认值即可恢复旧策略 |
| TD-004 | 前端 `ArithmeticCaptcha` 和示例页面改为普通图形验证码文案；同步 E2E 断言 | Common、Admin Shell、Admin E2E | 回退文案和测试断言 |

## 5. 实施清单

| 顺序 | 改动 | 完成条件 |
|---:|---|---|
| 1 | 统一后端生成、会话存储和校验逻辑 | arithmetic 与 graphic 服务测试通过 |
| 2 | 更新公共 API、Controller、Feign 和兼容标识 | API/HTTP/Feign 契约测试通过 |
| 3 | 更新 Common 组件、Admin 示例、E2E 断言和 README | 前端测试、构建和文档审计通过 |
| 4 | 执行模块验证并提交 PR | 最终提交头验证通过，远端 PR 回读一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令 | 结果 |
|---|---|---|---|
| AC-001/AC-003 | M10/M11/M12 | `mvn -f mango/mango-platform/mango-captcha/pom.xml verify` | PASS；core 40 项、starter 17 项、remote 3 项测试通过 |
| AC-001/AC-003 | M10 | `CaptchaServiceTest` | PASS；统一生成、ARITHMETIC 会话兼容校验和一次性删除覆盖 |
| AC-002/AC-005 | M10/M12 | `GraphicCaptchaGeneratorTest`、`CaptchaControllerIntegrationTest` | PASS；默认题型、字符集、干扰线配置和 HTTP 响应覆盖 |
| AC-004 | M09 | `pnpm --filter @mango/common test` | PASS；36 个测试文件、369 个测试通过 |
| AC-004 | M09 | `pnpm --filter @mango/common build`、`pnpm --filter @mango/admin-shell build` | PASS |
| 全部 | M09 | `git diff --check` | PASS |

## 7. 例外与剩余风险

- M13 浏览器真实登录链路未在本次本地交付中执行；当前工作区数据库不存在，未启动真实后端和浏览器验收环境。Admin E2E 契约已同步更新，远端 CI 仍需执行真实入口验证。
- 未执行 Maven/npm 制品发布和仓库回查；本 PR 不包含版本发布。
- `/captcha/graphic` 仍保留兼容入口，退出条件是现有消费者完成迁移并另行批准删除任务。

## 8. 回退

回退本 PR 即恢复 arithmetic 独立生成路径、旧默认图形配置和前端算术题文案；不需要数据库迁移或数据回填。
