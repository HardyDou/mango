# Mango Platform - 平台能力模块规范

@../../mango-pmo/rules/backend/10-dev-flow.md
@../../mango-pmo/rules/backend/01-code.md
@../../mango-pmo/rules/backend/05-module.md
@../../mango-pmo/rules/backend/08-test.md

进入 `mango-platform` 后，先按 `../../AGENTS.md` 执行 PMO preflight：

```bash
node ../../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango/mango-platform/**"
```

读取 preflight 输出中 `Must read` 的每一个文件原文后，再开始设计、编码或验证。

## 模块定位

`mango-platform` 是 Mango 脚手架的**平台能力模块**，定位介于基础设施层（infra）和业务模块（biz）之间。

## 能力范围

| 能力 | 说明 |
|------|------|
| `mango-auth` | 认证（登录/登出/Token） |
| `mango-identity` | 身份（账号/资料/认证用户事实） |
| `mango-authorization` | 授权（角色/菜单/权限码） |
| `mango-org` | 组织架构（部门/岗位） |
| `mango-system` | 系统配置/字典/参数/区域/国际化 |
| `mango-captcha` | 验证码 |
| `mango-biz-notification` | 消息中心 |

## 设计原则

1. **SPI + Starter** - 每个能力独立子模块，支持单体/微服务部署切换
2. **禁止跨域 SQL** - Mapper 不得 JOIN 其他域的表
3. **接口驱动** - 通过 `XxxApi` 接口暴露能力
4. **配置化 TTL** - 缓存超时必须配置化

## 子模块结构

标准业务能力通常包含 3 到 4 个子模块：

```
mango-xxx/
├── mango-xxx-api           # 接口定义（po/vo/dto/XxxApi）
├── mango-xxx-core          # 核心实现（entity/service/mapper）
├── mango-xxx-starter       # 本地调用（Controller 实现 XxxApi）
└── mango-xxx-starter-remote # 远程调用（FeignClient 继承 XxxApi）
```
