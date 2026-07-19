# 前端 HTTP 契约阶段证据（2026-07-19）

> 历史阶段证据：本文只记录当时的 FE0 传输契约批次。当前完成定义以采用边界决定和最终候选证据为准；业务灰度、故障注入和生产回滚从不属于前端代码规范毕业门禁。

## 1. 阶段结论

- 提交 `f0cd43b70` 在 FE0 `@mango/api-schema` 中定义了厂商无关的 `HttpClient`、`HttpRequest`、`HttpError` 和进度类型。
- 业务 API 可以只接收注入的 `HttpClient`，使用相对 endpoint 和标准 `AbortSignal`；契约不依赖 Axios、Vue、Element Plus、router、store、DOM 实现、环境变量或宿主全局对象。
- TypeScript 只作为开发依赖，发布产物没有运行时依赖；包具有独立严格 typecheck 和 build。
- 本阶段仅完成传输契约，不包含 Axios adapter、鉴权/刷新、运行时注入和真实服务联调，因此当时只能判定该阶段完成，不能代表完整规范状态。

## 2. 契约与反向验证

| 检查项          | 结果                                                   |
| --------------- | ------------------------------------------------------ |
| 正向 fixture    | 泛型响应、query、进度、`AbortSignal` 编译通过          |
| 反向 fixture    | 数值 `ApiId` 与非泛型 transport 实现被 TypeScript 拒绝 |
| 源码隔离        | 无 Axios/Vue/Element Plus/router/store/host 引用       |
| 包级 typecheck  | `@mango/api-schema` 零诊断                             |
| 包级 build      | 生成 `dist/index.js` 与 `dist/index.d.ts`              |
| 发布声明        | 仅 Mango 类型与 Web 标准 `AbortSignal`，无厂商类型泄漏 |
| package exports | 通过                                                   |
| 隔离业务消费者  | 28 个本地 tarball 安装、`vue-tsc`、生产 build 通过     |

隔离消费者验证使用 `pnpm package-consumer:typecheck -- --reuse-build`。它通过本地 tarball 安装公开包，不使用 workspace 链接或源码深层导入。`@mango/api-schema@1.0.2` tarball 包含 `dist/index.d.ts`、`dist/index.js`、`package.json` 和 `README.md`。

## 3. 固定工具链复验

| 项目         | 实际值                                                             |
| ------------ | ------------------------------------------------------------------ |
| 镜像         | `mango/frontend-quality:node22-pnpm11.14`                          |
| 镜像 SHA-256 | `2a04ce0242088af26fd0b147318842ae55ef09bcc2f917126347ff3ac0d2cf30` |
| 平台         | Linux arm64                                                        |
| Node         | 22.23.1                                                            |
| pnpm         | 11.14.0                                                            |
| 网络         | `--network none`                                                   |
| 安装         | frozen/offline；复用 753、下载 0                                   |

断网环境通过以下验证：

- `@mango/api-schema` typecheck、build、package exports；
- 全量静态门禁与架构、边界、组件合同；
- 质量脚本测试 52/52；
- workspace 测试 399/399。

本机隔离 tarball 消费者构建在 Node 26.5.0 上执行并通过；认证 Node 22 环境已覆盖包级声明生成、导出和全仓静态/测试，但尚未在断网容器内重复临时消费者安装，不能把两项证据混写为同一次运行。

## 4. 全仓质量事实

| 指标       |                              当前结果 |
| ---------- | ------------------------------------: |
| ESLint     |               232 error / 903 warning |
| Prettier   |                  583 个存量不一致文件 |
| Stylelint  |                 935 error / 0 warning |
| TypeScript | 25 个失败 workspace / 784 diagnostics |
| 前端边界   |                    21 个存量 identity |
| 架构例外   |                                 26 个 |
| 组件合同   |   195 个 legacy、0 个完成分类的 C3/C4 |

上述结果仅表示本次变更没有增加当时的基线债务。最终规范状态按当前采用边界决定和最终候选证据判断，不能依靠抬高基线、跳过检查或 `continue-on-error`。

## 5. 下一阶段门槛

1. 新增 FE1 Axios adapter，实现标准错误、取消、超时、进度、实例隔离和销毁，并证明公开 API 不泄漏 Axios 类型。
2. 由 host/runtime 创建实例并向业务 API 注入，业务页面和业务 API 不直接创建传输客户端。
3. 在真实单体和微前端拓扑验证鉴权、重复挂载/卸载、缓存、样式、副作用和失败恢复。
4. 为候选版本生成不可变制品并执行本地独立消费验证；Nexus candidate 回读由获得授权后的制品发布合同单独执行。
5. 具体业务应用如需灰度与上一稳定版本回退演练，由独立业务部署/运维合同保留指标、触发条件、命令和复验报告，不作为前端规范的下一门禁。
