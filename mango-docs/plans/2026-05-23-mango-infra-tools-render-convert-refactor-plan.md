# mango-infra-tools render/convert 重构计划

更新时间：2026-05-23

## 背景

当前 `mango-infra-tools-doc` 同时承载了文档转换、PDF 处理和一部分通用文档工具能力，边界偏宽。与此同时，`/Users/hardy/Work/file-online-preview` 中的 kkFileView 服务包含了成熟的格式转换链路，本次只迁入其中的“格式转换能力”。

本次重构的目标是：

- 将 `mango-infra-tools-doc` 重命名为 `mango-infra-tools-render`
- 新增 `mango-infra-tools-convert`
- 仅将 kkFileView 中“格式转换相关代码”落到 `mango-infra-tools-convert`
- 让 `mango-file` 成为这两类能力的业务依赖方，不让这两个模块感知 `fileId`、租户、权限、文件关联关系

## 目标架构

```text
mango-infra-tools
  ├── mango-infra-tools-render
  │   ├── render-api
  │   ├── render-core
  │   └── render-starter
  └── mango-infra-tools-convert
      ├── convert-api
      ├── convert-core
      └── convert-starter
```

## 职责边界

### mango-infra-tools-render

职责：

- 保留原 `mango-infra-tools-doc` 的命名收口结果
- 承接现有与模板、文档输出相关的基础能力
- 作为旧文档工具模块的稳定替代名，避免继续扩散 `doc` 命名
- `api` 负责对外稳定契约，`core` 负责实现，`starter` 负责装配

不负责：

- 文件存储
- `fileId` 映射
- 租户/权限校验
- 原文件与预览产物的关联持久化
- kkFileView 的格式转换实现

### mango-infra-tools-convert

职责：

- 提供格式转换内核
- 承载 kkFileView 中可复用的格式转换实现
- 按源格式/目标格式选择具体转换器
- 提供统一的转换请求、转换结果和转换器 SPI
- 管理转换缓存、失败结果、重试和异步执行基础能力
- `api` 负责对外稳定契约，`core` 负责实现，`starter` 负责装配

不负责：

- 文件业务逻辑
- `fileId` 映射
- 租户/权限校验
- 原文件和派生文件关系存储
- 预览页面

## 需要提供的能力

### render 模块 API

- `render-api` 作为业务代码的编译依赖
- `render-core` 提供实现
- `render-starter` 提供 Spring 装配和部署入口
- 维持现有 `mango-infra-tools-doc` 暴露出来的契约兼容

### convert 模块 API

- `convert-api` 作为业务代码的编译依赖
- `DocumentConvertService`
- `DocumentConvertRequest`
- `DocumentConvertResult`
- `DocumentConverter`
- `DocumentFormat`
- `DocumentFormatPair`
- `ConverterRegistry`
- `supportedConversions()`
- `convert(request)`
- `canConvert(source, target)`

### file 服务依赖方式

- `mango-file` 编译期依赖 `render-api` 和 `convert-api`
- `mango-file` 部署期依赖 `render-starter` 和 `convert-starter`
- `mango-file` 负责拉取源文件、校验权限、落盘/对象存储、保存派生关系
- `mango-file` 调用 `render` 和 `convert` 生成预览/转换产物
- 产物对应关系只保存在 `mango-file`，不放在 infra 工具模块里

## 需要满足的特性

### render

- 保持现有能力入口稳定
- 只做命名收口，不引入 kkFileView 迁移内容
- 与模板/文档输出场景兼容
- 错误明确，禁止吞异常
- 业务代码只依赖 `render-api`

### convert

- 支持多格式转换扩展
- 支持 same-format 直出
- 只接收 kkFileView 迁移来的格式转换内核
- 支持缓存抽象，允许本地缓存或共享缓存实现
- 支持并发处理和超时控制
- 支持 JDK 21 运行时的并发能力
- 支持独立部署，也支持被 `mango-file` 直接依赖
- 业务代码只依赖 `convert-api`

### 通用约束

- `module.properties` 只放在 starter
- `module-name`、`module-path` 保持稳定且唯一
- core/starter 边界清晰
- 不在 infra 模块里放租户、权限、文件关联业务
- 所有 IO 尽量保持流式

## 迁移范围

### 从现有 `mango-infra-tools-doc` 保留到 render 的内容

- 现有模块骨架
- 现有文档工具 API
- 与模板/文档输出相关的公共类型

### 从 kkFileView 迁入 convert 的内容

- 服务端格式转换链路
- 转换器注册与选择逻辑
- Office / PDF / 图片等转换基础能力
- 必要的缓存与状态管理
- 不迁入 kkFileView 的完整 Web 预览前端
- 不迁入 Windows/Linux 部署脚本
- 不迁入仅服务 kkFileView 自身的演示页面

## 重构步骤

| 阶段 | 内容 | 验收 |
|---|---|---|
| P0 | 将 `mango-infra-tools-doc` 改名为 `mango-infra-tools-render`，同步更新 parent pom、包名、测试包名、module.properties、自动装配声明 | 代码引用全部指向 render，新模块名可正常解析 |
| P0 | 建立 `mango-infra-tools-render-api / core / starter` 骨架 | 业务代码可依赖 api，运行时可依赖 starter |
| P0 | 建立 `mango-infra-tools-convert-api / core / starter` 骨架 | 业务代码可依赖 api，运行时可依赖 starter |
| P1 | 抽出转换 API 与转换器注册机制 | render 与 convert 的 public API 不再混用 |
| P1 | 迁入 kkFileView 格式转换实现 | 至少支持当前需要的文档转换路径 |
| P1 | 更新 `mango-template` 的依赖边界 | 模板侧只依赖需要的能力模块，不再绑定旧 doc 名称 |
| P2 | 完善缓存、异步、失败恢复 | 可观测、可重试、可清理 |
| P2 | 接入 `mango-file` 的预览/转换调用链 | 文件服务可调用 render/convert 形成完整预览流程 |

## 风险与控制

- 迁移时最容易出问题的是坐标、包名、测试资源路径和 starter 元数据，必须一次性对齐。
- kkFileView 的实现里可能混有 Web 前端和服务端转换逻辑，迁移时只收转换内核。
- 如果 render 和 convert 之间存在共享类型，要先把公共协议抽到独立 API，再拆实现。

## 完成标准

- `mango-infra-tools-render` 和 `mango-infra-tools-convert` 的模块边界清楚
- 现有 `doc` 命名全部收敛到 `render`
- kkFileView 的格式转换能力已迁入 `convert`
- `mango-file` 可独立组合 render/convert 完成预览链路
- 构建和关键测试通过
