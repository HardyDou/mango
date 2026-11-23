# Mango File Preview

## 1. 概览

`mango-file-preview` 是 Mango 的文件在线预览入口。业务系统已经拿到 `fileId` 后，可以用它生成短期预览链接，让内置 kkFileView 预览引擎在有效期内读取 Mango 文件中心的源文件。

它不负责上传、下载、存储配置和文件业务归属，这些属于 `mango-file`。文件是否存在、当前租户是否可见、文件是否可下载，仍由 `mango-file` 判定。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 创建预览链接 | `GET /file-preview/files/preview-link?fileId=...` 返回预览 URL、token、有效期与渲染类型 `renderMode`（`PDF`/`IMAGE`/`VIDEO`/`AUDIO`/`PAGE`/`CONVERTING`/`NOT_SUPPORTED`，前端据此选择渲染器） |
| 直接跳转预览页 | `GET /file-preview/files/preview?fileId=...` 生成 source token 后 forward 到预览引擎 |
| 短期公开入口 | `GET /file-preview/files/preview-entry?token=...` 使用已签发入口 token 进入预览 |
| 源文件读取 | `GET /file-preview/sources?token=...` 供预览引擎读取源文件流 |
| 上下文保存 | token 中保存签发时的 `MangoContextSnapshot`，读取源文件时恢复上下文 |
| 引擎资源注册 | 启动时注册 `/onlinePreview`、`/pdfjs/**`、`/xlsx/**` 等公开资源 |
| 独立 UI 阻断 | 默认拦截 kkFileView 首页和演示文件管理入口 |

> 取源通道的完整说明见 [2.1 取源通道](#21-取源通道)。一句话：`fileId` 是主路径（内嵌部署在进程内直取，独立部署引擎按票据远程直取），`url` 通道只作兼容兜底、不推荐，且必须显式配置 `mango.file-preview.source-base-url`，缺失时签发阶段直接报错，不会静默回退。

### 2.1 取源通道

预览引擎拿到源文件只有三种方式。业务侧只需要给 `fileId`，不用关心走哪条通道；通道由部署形态和运维开关决定。

| 通道 | 触发条件 | 身份校验点 | 失败语义 |
|------|----------|------------|----------|
| `fileId` 通道（主路径，同进程直取） | 预览引擎与 Mango 同进程，且 `mango.file-preview.internal-source-enabled=true`（默认）并装配了进程内取源实现 | 短期取源票据（`file-preview:source:*`，默认 24 小时）本身即凭据；引擎信任门 `FileDownloaders#verifySource` 只读校验票据存在且未过期 | 票据无效或过期 → 信任门返回 403，不进入引擎的解包、转换与落盘 |
| `fileId` 通道（主路径，远程直取） | 独立部署引擎装载 `mango-file-preview-starter-remote` 且 `mango.file-preview.remote.enabled=true` | 同上（票据 + 信任门）；引擎只把票据转发给 `GET /file-preview/sources`，票据有效性由该接口在取流时校验，不做额外的有效性探测 | 票据无效 → 取流被拒，引擎信任门返回 403；远程调用失败或正文为空 → 引擎下载器返回 `下载失败: <原因>`，不产出空文件 |
| `url` 通道（兼容兜底，不推荐） | 既没有进程内取源实现，也没有启用远程直取（`internal-source-enabled=false` 且 `mango.file-preview.remote.enabled` 未开启） | 引擎侧 `trust.host` 校验源地址 host + `/file-preview/sources?token=...` 自己校验票据 | 未配置 `mango.file-preview.source-base-url` → 签发阶段抛 `180004`；host 不在信任列表 → 引擎 403 `notTrustHost.html` |

两条 `fileId` 通道对业务完全等价：预览地址都是 `/onlinePreview?fileId=<票据>&fullfilename=<引擎文件名>`，都不需要容器能解析对外域名，大文件也不会经过「流出再落盘」的二次拷贝。

`/file-preview/sources` 在 `FilePreviewPermitPathBeanPostProcessor#PERMIT_PATHS` 中，因此不经 `AuthFilter`（与 `/file-preview/generated` 相同）。它的安全性由「一次性短期票据 + 票据绑定调用方上下文 + 取流时校验」共同承担；`mango.file-preview.remote.*` 不引入第二种令牌，也不改动这份白名单。

### 2.2 格式与渲染方式

| 格式 | 渲染方式 | 是否需转换 | 已知限制 |
|------|----------|------------|----------|
| `doc`、`docx` | 平台异步转换为 PDF，PDF.js 读取 `/file-preview/files/preview-artifact` | 是（LibreOffice/JODConverter，显式关闭 `mango.fileproc.convert.office-to-pdf-enabled` 时回退 Aspose） | 依赖 Aspose 授权与字体；缺字体时转换不报错但可能出现缺字或替换字形；复杂排版与 Office 原生渲染有差异 |
| `ppt`、`pptx` | 同 `doc`/`docx`：转 PDF 后由 PDF.js 展示（无逐页动画与嵌入媒体） | 是 | 同上；动画、切换效果、嵌入音视频不保留 |
| `xls`、`xlsx`、`csv` | `fileId` 通道下一律由平台异步转换为 PDF，PDF.js 读取 `/file-preview/files/preview-artifact`；只有 `url` 通道且 `officePreviewType != "html"`、`officeTypeWeb=web` 时，`xlsx`/`csv` 才由引擎用 Luckysheet 渲染前端表格（`officeweb.ftl`），`xls` 没有该路径 | `fileId` 通道下是（LibreOffice/JODConverter） | `fileId` 通道没有对外 URL，前端表格的 `getCorsFile` 回源会被信任门拒绝，因此该通道固定回落 PDF 转换；表格可读但没有公式编辑与筛选能力 |
| `pdf` | PDF.js 直接读取 `/file-preview/files/preview-content` | 否 | 加密 PDF 需要密码；超大 PDF 首屏依赖分页加载 |
| 图片（`jpg`、`png`、`gif`、`webp`、`bmp`、`svg` 等） | 引擎 `/picturesPreview` 直接展示（`svg` 走 `svg.ftl`） | 否 | `heic`/`heif` 走 `heic.ftl`、`avif` 归入图片通道，均依赖浏览器解码能力；浏览器不支持时需前端转码或降级为下载 |
| `mp4` | 引擎 `media.ftl` 以 HTML5 `<video>` 播放 | 否 | 仅 H.264 视频 + AAC 音频的组合在各浏览器稳定可播；其它编码（H.265/HEVC、AV1、ProRes 等）可能只有声音或直接黑屏 |
| `zip`（及 `rar`、`7z` 等压缩包） | 引擎压缩包页 `compress.ftl`，内部文件通过 `/compressed-file` 读取 | 否（不进入平台 PDF 转换队列） | 加密压缩包需要密码；解包落在 `file-dir` 之外的解包目录（默认同级 `_extracted`），占用该目录磁盘空间 |

### 2.3 能力边界

- `xlsx`/`csv` 的 Luckysheet 前端渲染（`officeweb.ftl`）只在 `url` 通道可用：引擎 `mango-file-preview-engine/src/main/java/cn/keking/service/impl/OfficeFilePreviewImpl.java#handleWebPreview` 要求 `officePreviewType != "html"` 且 `officeTypeWeb == "web"`，回源依赖 `getCorsFile?urlPath=<base64 源地址>`，源地址 host 必须在信任列表内。
- `fileId` 通道由平台侧先定渲染方式：`FilePreviewServiceImpl#previewMode` 在进程内直取生效（`useInternalSource()` 为真）时把 `xlsx`/`csv` 判为 `PDF_CONVERSION`（`ENGINE_SHEET_EXTENSIONS`），页面直接进入 PDF.js 读 `/file-preview/files/preview-artifact`，根本不进入引擎的表格页面，因此不受上面的引擎开关影响。这与 `renderMode` 的 `CONVERTING` 取值一致：`fileId` 通道下表格先转换再展示。转换队列与渲染分派共用同一份判定（`FilePreviewMode#of(extension, internalSourceAvailable)`），因此 `renderMode` 为 `CONVERTING` 时任务必定进入 PDF 转换并产出 `preview-artifact`，两者不会再分叉。

### 2.4 限制清单

以下限制是能力本身的边界，部署与业务侧必须显式知晓，平台不会静默降级成「看起来成功」：

| 限制 | 影响 | 处理建议 |
|------|------|----------|
| `mp4` 仅 H.264 + AAC 组合稳定 | 其它编码可能黑屏或只有声音 | 上传前转码，或业务侧对该类文件直接提供下载 |
| 加密压缩包需要密码 | 无密码时压缩包页提示输入密码，无法列出内部文件 | 业务侧保存密码并通过页面 `filePassword` 传入；不要上传无法提供密码的加密包 |
| 解包占用解包目录磁盘 | 大压缩包解包后的文件落在 `ConfigConstants.getDecompressDir()`（默认是 `file-dir` 的同级目录 `_extracted`，可用 `decompress.dir` 覆盖），占用与包内文件总量相当的空间 | 关注该目录容量，必要时调大或定期清理 |
| 解包目录可能落在容器可写层 | `file-dir` 由引擎侧 `file.dir` 或环境变量 `KK_FILE_DIR` 决定（Mango 侧没有 `mango.file-preview.file-dir` 属性），`_extracted` 是它的**同级**目录：若 `file-dir` 指向挂载卷的根，同级 `_extracted` 就落到卷外。容器重建后解包缓存丢失（重新打开压缩包即重建，不影响功能），根文件系统只读时解包直接失败 | 把卷挂到父目录（卷挂 `/data/preview`、`KK_FILE_DIR=/data/preview/file`），或显式设 `decompress.dir=/data/preview/_extracted`；回滚到旧版本后会把解包写回 `file-dir` 下，新的 `_extracted` 成为孤儿目录可直接删除 |
| `docx`/`ppt` 依赖 Aspose 授权与字体 | 无授权时转换失败；缺字体时转换不报错但可能缺字 | 配置 Aspose license；`fonts-folders` 只追加字体目录，不覆盖默认，缺字体只记录 ERROR 日志、不阻断预览 |
| `heic`/`avif` 依赖浏览器解码 | 浏览器不支持时图片区域空白 | 前端转码为 `jpg`/`webp`，或对这些格式直接降级为下载 |

### 2.5 页面内子资源统一前缀

浏览器加载预览页时**不带** `Authorization` 头，因此页面里的图片、`<video>` 源和文档产物必须是匿名可取的同源地址。引擎落盘文件统一挂在一个可枚举前缀下：

| 页面内子资源 | 地址形态 | 匿名取用依据 |
|--------------|----------|--------------|
| 图片（`picture.ftl`） | `http://<host>/preview-file/<落盘文件名>` | `PERMIT_PATHS` 放行 `/preview-file/**` |
| 音视频（`media.ftl`） | `http://<host>/preview-file/<落盘文件名>` | 同上 |
| 压缩包内条目（`compress.ftl`） | `http://<host>/compressed-file?kkCompressfileKey=<解包键>&kkCompressfilepath=<包内相对路径>&fullfilename=<文件名>` | 服务端 `CompressFileEntryService` 校验越界后流式返回，不依赖静态映射 |
| 文档/表格产物 | `http://<host>/file-preview/files/preview-artifact?token=<票据>`（PDF 直出为 `preview-content`） | token 校验 |

- `/preview-file/**` 由 `cn.keking.config.WebConfig` 映射到引擎落盘目录 `ConfigConstants.getFileDir()`，对外地址由 `FileHandlerService#getPublicPath` 统一拼装（`FileHandlerService.PUBLIC_FILE_PATH_PREFIX`）。
- 压缩包解包目录**不在** `file-dir` 内：`CompressFileReader#unRar` 把包内容解到 `ConfigConstants.getDecompressDir()`（默认 `file-dir` 同级目录 `_extracted`，`decompress.dir` 可覆盖），该目录不参与 `/preview-file/**` 与根路径 `/**` 两条静态映射，因此不再有 `/preview-file/<解包目录>/<条目>` 这类匿名可达的条目地址。
- 压缩包条目统一走 `GET /compressed-file`（`FileController`）：`kkCompressfileKey` 是解包键（`bundle.zip_`，嵌套包再加 `_decompression` 前缀），`kkCompressfilepath` 是包内相对路径；`CompressFileEntryService#resolveEntryPath` 先做非法字符、`kkCompressfilepath` 必须位于 `kkCompressfileKey/` 之下、解析后必须仍在解包根内三层校验，再 `Files.newInputStream` 流式返回，越界返回 400、条目不存在返回 404。
- `FilePreviewPermitPathBeanPostProcessor#PERMIT_PATHS` 里**只有** `/preview-file/**` 这一个根级文件前缀；禁止改成 `/**`、`/file-*`、`/**/*.mp4` 这类宽匹配，否则整个引擎落盘目录都会变成匿名可读。放行清单由 `FilePreviewEngineResourceRegistrarTest#permitPaths_allowOnlyTheSinglePublicFilePrefixForEngineFiles` 锁定：以 `/preview-file` 开头的条目必须恰好一条。
- `previewUrl`/`preview-entry` 返回的地址带网关前缀 `/api`（网关据此路由到 `mango-file-preview`）。单进程直连（不走网关，直接用 `http://127.0.0.1:<port>` 访问应用）时需剥掉 `/api` 前缀，否则 404。


### 独立部署的路由表

独立部署（应用 5555 + 独立引擎 8012）下，浏览器看到的地址分两类 origin，排障时先确认落在哪一边：

| 渲染模式 | 出页方 | 地址 | 判据 |
|---|---|---|---|
| `NATIVE_PDF` | **应用** | `/api/file-preview/files/preview-entry?token=…`（PDF.js 页，**无 Location**） | 原生 PDF 由应用出页，引擎不参与 |
| `PDF_CONVERTING` | **应用** | 同上；产物 `/api/file-preview/files/preview-artifact?token=…` | Office 转换在应用侧完成，产物落在 `ConfigConstants.getFileDir()` |
| `IMAGE`、`VIDEO`、`AUDIO` | **引擎** | `/api/onlinePreview?fileId=…`，子资源 `/preview-file/**` | 302 的 `Location` 带 `engine-public-base-url` 的 origin |
| `PAGE`（压缩包、引擎级表格等） | **引擎** | 同上 | 同上 |

独立部署时请把预览引擎的工作目录或 `mango.file-preview.file-dir` 固定为绝对路径；默认相对 `./file` 随启动目录变化，换目录重启会导致已落盘的转换产物/取源文件找不到（表现为 `/preview-file/**` 404）。

两类地址形态：

| 部署 | `previewUrl` 形态 |
|---|---|
| 内嵌（未配 `engine-public-base-url`） | `/api/onlinePreview?fileId=…`（经应用网关，`/api` 由平台保留） |
| 独立引擎（配了 `engine-public-base-url`） | `{origin}/onlinePreview?fileId=…`（直连引擎，无 `/api`） |

**引擎独立进程部署时 `mango.file-preview.engine-public-base-url` 必须配置**；未配置时应用产出相对地址，仅内嵌部署（引擎与应用同源）正确。

**引擎不参与 PDF 与转换产物**：独立部署时不要拿引擎的 403/200 当 PDF 是否可用的判据；PDF 的通过标准是应用侧 `preview-artifact` 返回 PDF 字节。

## 3. 后端接入

只使用契约时依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.file.preview</groupId>
    <artifactId>mango-file-preview-api</artifactId>
</dependency>
```

同进程启用文件预览能力时依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.file.preview</groupId>
    <artifactId>mango-file-preview-starter</artifactId>
</dependency>
```

独立部署的预览引擎（`cn.keking.ServerMain`）要按票据远程直取源文件时，依赖远程取源 starter：

```xml
<dependency>
    <groupId>io.mango.platform.file.preview</groupId>
    <artifactId>mango-file-preview-starter-remote</artifactId>
</dependency>
```

该包只带来声明式 Feign 取源能力（`mango-infra-feign-starter`）与引擎侧下载器桥接，不引入 Mango 应用上下文；独立引擎进程把它放进 classpath 并配置 `mango.file-preview.remote.base-url` 即可，不需要把引擎改回 `url` 通道。

预览能力通常还需要文件中心和转换处理能力：

```xml
<dependency>
    <groupId>io.mango.platform.file</groupId>
    <artifactId>mango-file-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.mango.infra.fileproc</groupId>
    <artifactId>mango-infra-fileproc-starter</artifactId>
</dependency>
```

运行依赖：

| 依赖 | 用途 |
|------|------|
| `FileApi` | 查询文件元数据 |
| `IFileContentProvider` | 以文件中心服务契约读取源文件流 |
| `ITokenStore` | 保存 `file-preview:entry:*` 和 `file-preview:source:*` 短期 token |
| `ILeaseLocker` | 协调多实例转换任务和 Worker 槽位 |
| `ApiResourceApi` | 启动时注册预览引擎公开资源 |
| `mango-file-preview-engine` | 内置 kkFileView 预览引擎和静态资源 |

`ITokenStore` 和 `ILeaseLocker` 由宿主应用的 `mango-infra-kv-starter` 装配。file preview 不提供进程内 token fallback；
宿主需要启用 `mango.kv.capability.enabled=true`、`mango.kv.capability.locker=true` 和
`mango.kv.capability.token-store=true`，并通过 `mango.kv.store.type` 选择 Memory、Redis、JDBC 或自动探测。
缺少 `ITokenStore` 或 `ILeaseLocker` 时应用会在启动期失败，避免多实例部署时把入口 token 或转换协调状态静默保存在单个 JVM 中。

`FileSettingsApi` 和 `ConvertApi` 是可选协作能力。缺少文件设置服务时，Office 预览大小限制使用 200 MiB 默认值；缺少格式转换服务时，直接预览仍可用，Office 转换任务返回 `FAILED`，提示“当前未启用文件格式转换能力，请下载原文件查看”；这是部署能力缺失，不是格式问题。格式本身不支持在线预览时返回 `NOT_SUPPORTED`，提示“该格式不支持在线预览，请下载原文件查看”，不要把两类结果混用 `FAILED`，否则运维会把路由问题当成转换引擎问题。

单体应用通过 `mango-file-preview-starter` 把预览引擎组件装配到宿主 Spring 上下文，复用宿主端口，
不会注册 kkFileView 的独立 `ServerMain`，也不会通过 engine JAR 覆盖宿主应用 Banner。

## 4. 前端接入

业务页面通常不直接拼 kkFileView 地址，而是用文件前端能力里的预览组件。

常见方式：

| 场景 | 做法 |
|------|------|
| 管理后台文件列表 | 使用 `@mango/file` 的文件管理页面和预览入口 |
| 业务详情页预览单个文件 | 使用 `FilePreviewPanel`，传入业务保存的 `fileId` |
| 自定义预览按钮 | 调 `/file-preview/files/preview-link?fileId=...`，打开返回的 `previewUrl` |

前端预览提供方可指向：

| 地址 | 说明 |
|------|------|
| `/file-preview/files/preview-link` | 返回 JSON，适合组件先拿链接再打开 |
| `/file-preview/files/preview` | 直接 forward 到预览页，适合新窗口打开 |

## 5. 快速开始

1. 后端接入 `mango-file-starter` 和 `mango-file-preview-starter`。
2. 如果需要 Office 转换能力，按环境接入 `mango-infra-fileproc-starter` 和对应转换依赖。
3. 业务上传文件后只保存 `fileId`。
4. 详情页用 `FilePreviewPanel` 或调用 `/file-preview/files/preview-link` 创建预览链接。
5. 当前用户必须拥有 `file:files:download` 权限；文件管理菜单的正式资源声明会给对应角色登记该权限。
6. 预览引擎读取源文件：内嵌引擎（`mango-app` 同时装有 starter 与 engine）在同一进程内按票据直取；独立部署引擎装载 `mango-file-preview-starter-remote` 并开启 `mango.file-preview.remote.enabled` 后，按票据远程直取（`GET /file-preview/sources`）；两者都没有时才回退到 `url` 通道，此时必须配置 `mango.file-preview.source-base-url`，否则签发返回 `180004`。

## 6. 配置说明

配置前缀是 `mango.file-preview`。

```yaml
mango:
  kv:
    store:
      type: redis
    capability:
      enabled: true
      locker: true
      token-store: true
  file-preview:
    enabled: true
    engine-path: /onlinePreview
    entry-path: /api/file-preview/files/preview-entry
    source-path: /file-preview/sources
    source-base-url: http://mango-app:8080
    source-token-expire-seconds: 86400
    standalone-ui-enabled: false
    conversion:
      worker-count: 2
      office-base-port: 2001
    remote: # 仅独立部署引擎所在进程需要
      enabled: true
      base-url: http://mango-app:8080
      connect-timeout: 5s
      read-timeout: 60s
office:
  plugin:
    task:
      timeout: 5m

cache:
  type: redis # 多实例独立引擎使用 redis；单体可使用 jdk/default
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.file-preview.enabled` | `true` | 是否启用文件预览自动配置 |
| `mango.file-preview.engine-path` | `/onlinePreview` | 预览引擎入口路径 |
| `mango.file-preview.entry-path` | `/api/file-preview/files/preview-entry` | 预览入口地址（浏览器侧相对路径，可写完整 URL）。默认带 `/api` 是因为标准部署经前端代理访问；直连后端或代理前缀不是 `/api` 的部署必须覆盖，否则签发出来的 `previewUrl` 会 404 |
| `mango.file-preview.source-path` | `/file-preview/sources` | `url` 兜底通道下预览引擎读取源文件的短期 URL 前缀 |
| `mango.file-preview.source-base-url` | 无（`url` 兜底时必填） | 仅供 `url` 兜底通道使用：引擎经该地址访问 Mango 的 `/file-preview/sources`。**不再从当前请求地址推断**：走 `url` 兜底但未配置时报 `180004`，不会静默回退。其 host 会自动加入引擎信任列表，无需重复写 `trust.host` |
| `mango.file-preview.engine-public-base-url` | 空 | 引擎对外地址的**纯 origin**（可选带部署上下文，如 `https://engine.example.com/engine`）；**不要填 `/api`**（`/api` 是应用网关前缀，独立部署时平台自动剥离），也不要填 `engine-path` |
| `mango.file-preview.remote.base-url` | 无（开启远程直取时必填） | **引擎回打应用的地址**（引擎侧配置）。`mango.file-preview.remote.enabled=true` 时必填（缺失时引擎侧启动失败并点名该配置项）；`engine-public-base-url` 未配置不会失败，应用产出相对地址，仅内嵌部署正确 |
| `mango.file-preview.source-token-expire-seconds` | `86400` | 入口 token 和源文件 token 有效期，单位秒 |
| `mango.file-preview.standalone-ui-enabled` | `false` | 是否允许访问 kkFileView 独立首页和演示文件管理入口 |
| `mango.file-preview.internal-source-enabled` | `true` | 是否允许引擎按短期票据直取源文件（内嵌部署在进程内直取，独立部署配合 `mango.file-preview.remote.*` 远程直取）。设为 `false` 表示**强制**回退 `url` 兜底通道，此时必须配置 `source-base-url` |
| `mango.file-preview.remote.enabled` | `false` | 独立部署引擎是否启用远程直取；与 `mango-file-preview-starter-remote` 同时生效 |
| `mango.file-preview.remote.connect-timeout` | `5s` | 远程直取的连接超时（`Duration` 写法，如 `3s`、`500ms`） |
| `mango.file-preview.remote.read-timeout` | `60s` | 远程直取的读取超时；大文件取流时按该值放宽 |
| `mango.file-preview.conversion.worker-count` | `2` | 每个预览实例的转换 Worker 数量，同时决定默认 LibreOffice 进程数量 |
| `mango.file-preview.conversion.office-base-port` | `2001` | 未显式配置端口列表时生成 LibreOffice 端口的起始端口 |

本地单实例开发可以使用 `mango.kv.store.type=memory`；多实例部署应选择 Redis 或 JDBC，确保任意实例都能读取已签发 token。

Office 转换任务按文件内容版本生成稳定转换键；同一文件同一版本不区分 PDF/图片模式只允许一个底层转换，不同文件由固定大小的转换 Worker 池并行处理。`mango.file-preview.conversion.worker-count` 是唯一并发配置，默认 2；页面展示、任务消费能力和默认 LibreOffice 进程数量保持一致。未显式配置 `office.plugin.server.ports` 时，端口从 `office-base-port` 开始按 Worker 数量连续生成；显式配置端口列表时，数量不一致会在启动阶段失败。普通 PDF 预览和图片预览共用同一协调器；任务 Worker 使用 infra-kv 分布式槽位和文件版本租约，多个实例共同遵守同一并发上限。请求超时只结束当前请求视图，在途 LibreOffice 任务完成前不会启动同键的第二个转换，避免 `forceUpdatedCache` 形成输出竞争和 Office 断连。转换结果先写入唯一临时文件，校验成功后再原子替换缓存；源文件通过 `fileId` 对应的 `IFileContentProvider.downloadForService` 流读取，不会为大文件构造整文件字节数组。

Office 转换后的 PDF 由 PDF.js 通过同源 `/file-preview/generated?token=...&fileName=...` 读取，不再经 `/getCorsFile` 回源。该接口虽然属于 PUBLIC 路由，但必须携带有效 source token，并校验 token 对应的 `fileId` 与转换文件名；token 过期后不能继续读取转换结果。原始文件由引擎按票据直取（内嵌同进程或远程直取）；仅在 `url` 兜底通道下才由引擎从 `source-base-url` 下载。

引擎的 `/onlinePreview`、`/getCorsFile` 仍接受任意 URL，必须保留 host 白名单防止被当作 SSRF 代理。配置 `source-base-url` 后其 host 由 `FilePreviewTrustHostInitializer` 在启动时自动加入信任列表；只有使用 `url` 兜底通道或其它引擎入口时才需要手工配置引擎侧 `trust.host`。`fileId` 通道不需要 `trust.host` 包含 Mango 域名。

### 源文件直取（`fileId` 参数）

只要有一份会向引擎注册下载器（`cn.keking.spi.FileDownloader`）的实现，预览地址就不再拼对外域名，而是直接带源标识：

```
/api/onlinePreview?fileId=<短期取源票据>&fullfilename=<引擎文件名>
```

引擎收到 `fileId` 后不做 URL 解析、不发起 HTTP，直接把该标识交给已注册的下载器，由下载器把源文件流式落到引擎工作目录。两种部署各有一个实现：

| 部署形态 | 依赖 | 取源实现 | 开关 |
|----------|------|----------|------|
| 内嵌（`mango-app` 同时装 starter 与 engine） | `mango-file-preview-starter` | `FilePreviewEngineDownloader`，调 `IFilePreviewService#openSource` 进程内读流 | `mango.file-preview.internal-source-enabled=true`（默认） |
| 独立引擎（`cn.keking.ServerMain`） | `mango-file-preview-starter-remote` | `FilePreviewRemoteSourceDownloader`（`mango-file-preview-support`），经 Feign 调 Mango | `mango.file-preview.remote.enabled=true`（默认 `false`） |

收益：

- 容器不需要能解析或访问对外域名，域名变更不需要重新部署；
- 大文件不再产生 `sources` 出口流量，也少一次「流出再落盘」的拷贝；
- 取源票据仍然是短期、不可枚举的（`file-preview:source:*`，默认 24 小时），越权与过期行为与 `sources` 接口一致。

引擎侧只用了一个扩展点：`DownloadUtils#downLoad` 先问 `FileDownloaders`，没有实现接管时原样走内置下载。因此升级 kkFileView 只需保留这一处转调，其余代码（`WebUtils`、`TrustHostFilter`、各 `FilePreview` 实现）未改动。

#### 远程直取（独立部署引擎）

`mango-file-preview-starter-remote` 只提供两件事：一个声明式 Feign 客户端和一个向引擎注册的下载器，两者都受 `mango.file-preview.remote.enabled` 控制，引擎进程不需要装 Mango 应用 starter。远程侧**不提供**「这个票据是否有效」的探测端点：票据本身就是凭据，有效性只在取流时校验一次。

| 调用 | 接口 | 返回值 | 说明 |
|------|------|--------|------|
| 读取源文件 | `GET /file-preview/sources?token=...`（`FilePreviewSourceFeignClient`） | 裸 `feign.Response` | 下载器用 `response.body().asInputStream()` 直接 `Files.copy` 到引擎目录，**不把整个文件读成 `byte[]`**；票据无效或过期时该接口拒绝，下载器返回 `code=1` / `下载失败: <原因>` |

实现要点：

- 流式转发：`feign.Response` 在响应体未知或超阈值时返回活动流（feign-core 13.6 的 `InvocationContext.MAX_RESPONSE_BUFFER_SIZE`），调用方负责关闭，异常路径与空 body 都转成 `code=1` / `下载失败: <原因>`；
- 日志级别：`mango.feign.logger-level` 保持默认 `BASIC`，**不得**设为 `FULL`——`FULL` 会把响应体重新缓冲整个文件；
- 凭据边界：取流请求不带用户登录态，授权凭据就是路径上的短期一次性票据（`file-preview:source:*`，绑定发起方上下文，取流时校验并恢复该上下文再判文件权限）；
- 内部调用签名（可选）：`mango-infra-feign-starter` 在配置 `mango.internal-call.secret` 后会为该 Feign 调用附加 HMAC 内部调用头，接收端只对登记为内部接口的路径做校验——`/file-preview/sources` 不在内部路径集合内（它在 `FilePreviewPermitPathBeanPostProcessor#PERMIT_PATHS` 中），因此该签名是纵深防御的一层附加信息，**不是**授权依据，也不新增服务间令牌、不改 permit 白名单；
- 装配互斥：内嵌进程已有 `FilePreviewEngineDownloader` 时，远程下载器（`@ConditionalOnMissingBean(FileDownloader.class)`）不会重复注册；
- 开关语义：`mango.file-preview.internal-source-enabled` 是运维总开关。置 `false` 表示**强制**走 `url` 兜底（排除取源实现），与 `mango.file-preview.remote.enabled` 是“与”关系：只要总开关关着，即使远程直取已配置也不会使用。

约束与失败边界：

- 独立部署引擎开启远程直取后，**不再需要** `internal-source-enabled: false` 与 `source-base-url`；那套 `url` 兜底只在不想让引擎装 `mango-file-preview-starter-remote` 时使用，且拿到 `url` 形态预览地址时必须能访问 `source-base-url`；
- `remote.enabled=true` 但缺少 `remote.base-url` → 应用启动失败，错误信息直接点名配置项；
- 引擎信任门对远程票据只判非空白（不做远程探测），票据无效时在取流阶段被拒绝，同样不会进入引擎的解包与转换；网络异常表现为下载失败，不会被误当成「票据有效」；
- 下载器读流失败时返回与内置下载一致的 `code=1` 与 `下载失败: <原因>`，不会产出空文件。

`mango-file-preview-engine` 也支持独立引擎端口配置：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.file-preview.engine.port` | `8012` | 预览引擎端口 |
| `MANGO_FILE_PREVIEW_ENGINE_PORT` | 无 | 环境变量形式的引擎端口 |
| `KK_SERVER_PORT` | 无 | kkFileView 兼容环境变量 |
| `mango.file-preview.conversion.worker-count` | `2` | 每个预览实例的转换 Worker 数量；也可使用环境变量 `MANGO_FILE_PREVIEW_WORKER_COUNT` |
| `mango.file-preview.conversion.office-base-port` | `2001` | 自动生成 LibreOffice 端口的起始端口；也可使用环境变量 `MANGO_FILE_PREVIEW_OFFICE_BASE_PORT` |
| `KK_OFFICE_PLUGIN_PORTS` | 无 | 显式指定 LibreOffice 端口列表，数量必须与 Worker 数量一致 |
| `office.xlsx.web.buttons.enabled` | `true` | 是否在 XLS/XLSX Web 预览页显示“跳转 HTML 预览”和“打印”按钮；关闭时 Luckysheet 从页面顶部铺满 |
| `KK_OFFICE_XLSX_WEB_BUTTONS_ENABLED` | 无 | `office.xlsx.web.buttons.enabled` 的环境变量形式 |

隐藏 XLS/XLSX Web 预览页的两个外部入口时，配置预览引擎：

```properties
office.xlsx.web.buttons.enabled=false
```

容器部署也可以使用环境变量：

```bash
KK_OFFICE_XLSX_WEB_BUTTONS_ENABLED=false
```

配置缺失时默认值为 `true`，保留升级前的按钮和 `20px` 顶部区域。设置为 `false` 后，模板不渲染按钮区域，Luckysheet 使用 `top: 0`；该配置只作用于 `officeweb.ftl` 的 XLS/XLSX Web 预览，不改变 PDF、图片、视频、音频、普通文档、下载或新窗口预览行为。

## 8. 接口/API 使用

HTTP 接口前缀是 `/file-preview`。

| 方法 | 路径 | 访问模式 | 用途 |
|------|------|----------|------|
| GET | `/file-preview/files/preview-link?fileId=...` | PERMISSION (`file:files:download`) | 创建短期预览入口 |
| GET | `/file-preview/files/preview?fileId=...` | PERMISSION (`file:files:download`) | 按文件 ID forward 到预览页 |
| GET | `/file-preview/files/preview-entry?token=...` | PUBLIC | 使用已签发入口 token 进入预览 |
| GET | `/file-preview/files/preview-progress?token=...` | PUBLIC | 查询 Office 转换进度 |
| GET | `/file-preview/files/preview-content?token=...` | PUBLIC | 原生 PDF 读取原文件交给 PDF.js |
| GET | `/file-preview/files/preview-artifact?token=...` | PUBLIC | Office 转换产物读取（同为 token 鉴权） |
| GET | `/file-preview/sources?token=...` | PUBLIC | 预览引擎读取源文件流 |
| GET | `/file-preview/generated?token=...&fileName=...` | PUBLIC | 在 source token 有效期内读取对应的转换 PDF |

Java API：

```java
R<FilePreviewLinkVO> response = filePreviewApi.preview(fileId);
```

token 行为：

| token | key 前缀 | 生成入口 | 用途 |
|-------|----------|----------|------|
| 入口 token | `file-preview:entry:` | `/files/preview-link` | 公开入口换取引擎预览链接 |
| 源文件 token | `file-preview:source:` | `/files/preview` 或 `/files/preview-entry` | 预览引擎读取源文件 |

入口 token 换取预览页后，按文件扩展名决定渲染方式：

| 文件类型 | 渲染方式 |
|----------|----------|
| PDF（含 `application/pdf` 内容类型） | 平台 PDF.js 页面读取 `/preview-content` |
| `doc`、`docx`、`ppt`、`pptx`、`odt`、`ods`、`odp` | 异步队列转换为 PDF，PDF.js 页面读取 `/preview-artifact` |
| 压缩包、表格、图片、文本、媒体等其余格式 | 直接跳到预览引擎 `/onlinePreview`，由引擎按类型渲染；引擎不支持的格式由引擎返回明确提示 |

Office 转换统一使用 LibreOffice/JODConverter；只有显式关闭 `mango.fileproc.convert.office-to-pdf-enabled` 时才回退 Aspose。XLSX/CSV 的引擎 Web 预览不依赖引擎 Office 插件。

压缩包（`zip`、`rar`、`7z` 等）由引擎的压缩包预览页渲染，内部文件通过 `/compressed-file` 读取，不再进入 PDF 转换队列。

## 9. 返回字段

`FilePreviewLinkVO`：

| 字段 | 含义 |
|------|------|
| `fileId` | 文件 ID |
| `fileName` | 原始文件名 |
| `previewUrl` | 可打开的预览页面地址 |
| `previewToken` | 预览入口临时令牌，直接引擎预览时为空 |
| `expireSeconds` | token 有效期，单位秒 |

错误码：

| 业务码 | 含义 |
|--------|------|
| `180001` | 文件 ID 不能为空 |
| `180002` | 预览令牌无效或已过期 |
| `180003` | 文件不存在或不可预览 |
| `180004` | 未配置 `mango.file-preview.source-base-url`，无法生成 `url` 兜底取源地址 |

## 10. 管理入口

`mango-file-preview` 不提供管理菜单。

权限和访问边界：

| 入口 | 边界 |
|------|------|
| 创建预览链接 | 要求当前用户拥有 `file:files:download` 权限，并通过文件中心可见性校验 |
| 公开预览入口 | 只接受短期入口 token，不接受任意 `fileId` |
| 源文件读取 | 只接受短期 source token |
| 转换 PDF 读取 | 只接受短期 source token，且生成文件名必须与 token 中的 `fileId` 和文件扩展名匹配 |
| 源文件权限 | 读取时恢复 token 中的上下文，再通过 `IFileContentProvider.downloadForService(fileId)` 读取 |
| kkFileView 独立 UI | 默认由 `standalone-ui-enabled=false` 阻断 |

`file:files:download` 只允许进入签发流程，不表示可以预览任意文件。签发 token 前会调用 `FileApi.get(fileId)`，源文件读取时会恢复签发时的 `MangoContextSnapshot` 并通过文件中心服务契约读取文件流。文件是否属于当前租户、是否归档或删除，仍由 `mango-file` 判定。

`/file-preview/files/preview-link` 和 `/file-preview/files/preview` 的 Controller 与契约测试固定使用 `LOGIN`，不得退回 `file:files:download` 管理权限；匿名主体仍不能用任意 `fileId` 签发预览入口。

## 11. 数据与初始化

本模块没有独立业务表和 Flyway migration。

启动初始化：

| 入口 | 内容 |
|------|------|
| `FilePreviewEngineResourceRegistrar` | 调用 `ApiResourceApi.registerApiResources()` 注册预览引擎公开资源 |
| `FilePreviewPermitPathBeanPostProcessor` | 给 Web 安全放行预览公开入口和 source 路径 |
| `FilePreviewStandaloneUiBlockFilter` | 默认阻断 kkFileView 首页和演示文件管理入口 |

公开入口使用安全链中的 `permitAll`/PUBLIC 资源语义，不通过 `WebSecurityCustomizer.web.ignoring()` 绕过安全过滤器。

自动注册为 PUBLIC 的资源只服务预览引擎页面和短期 token 访问，不接受任意 `fileId`。包括：

其中 `/preview-file/**` 只用于页面内图片和音视频子资源（见 §2.5），不含任何目录列表能力。

```text
/onlinePreview
/picturesPreview
/getCorsFile
/directory
/compressed-file
/file-preview/files/preview-entry
/file-preview/files/preview-progress
/file-preview/files/preview-content
/file-preview/files/preview-artifact
/file-preview/sources
/file-preview/generated
/preview-file/**
/pdfjs/**
/js/**
/css/**
/images/**
/bootstrap/**
/highlight/**
/xlsx/**
/static/**
/favicon.ico
```

## 12. 问题排查

| 现象 | 排查点 |
|------|--------|
| 创建预览链接失败 | 检查当前账号是否拥有 `file:files:download`、文件是否存在、租户上下文和文件可见性是否正确 |
| 预览页能打开但文件加载失败 | 检查 `/file-preview/sources`、`/file-preview/files/preview-content`、`/file-preview/files/preview-artifact` 是否被网关放行，token 是否过期，转换 PDF 是否已生成 |
| Office 预览失败 | 检查预览引擎、LibreOffice、Aspose license 和 `mango-infra-fileproc` |
| iframe 内嵌 Office 预览返回 401 | 确认 `/file-preview/files/preview-artifact` 在 permit 白名单和 API 资源中均为 PUBLIC，且网关不额外要求登录 |
| 压缩包进入 PDF 转换并提示失败 | 检查 `FilePreviewMode` 是否把该扩展名判为 `ENGINE`，压缩包不应调用平台转换 |
| 不希望暴露 kkFileView 首页 | 保持 `mango.file-preview.standalone-ui-enabled=false` |
| 反向代理下源文件 URL 不对 | `fileId` 通道不拼对外域名，先确认引擎是否真的收到 `fileId`；`url` 通道下检查 `mango.file-preview.source-base-url` |
| 创建预览链接返回 `180004` | 当前部署没有可用的取源实现（内嵌没装 starter、独立引擎没开 `mango.file-preview.remote.enabled`），需要二选一；若确实要走 `url` 兜底，必须配置 `mango.file-preview.source-base-url` |
| 独立引擎报 `FileNotFound`/票据无法解析 | 确认引擎进程装了 `mango-file-preview-starter-remote` 且 `mango.file-preview.remote.enabled=true`、`base-url` 可访问 |
| 远程取源连接超时或读超时 | 检查 `mango.file-preview.remote.connect-timeout`/`read-timeout`、网络与网关；大文件需放宽 `read-timeout` |
| 预览内存暴涨且 `mango.feign.logger-level=FULL` | 日志全量模式会重新缓冲响应体，改回 `BASIC` |
| Office 预览文字缺失或方块 | 检查引擎字体目录（`fonts-folders` 只追加、不覆盖默认），缺字体只记录 ERROR 不阻断转换 |
| `mp4` 黑屏或只有声音 | 确认编码为 H.264 + AAC，其它编码需提前转码 |
| 压缩包提示需要密码 | 加密压缩包必须提供密码，平台不会绕过 |
| 预览 token 过快失效 | 调整 `mango.file-preview.source-token-expire-seconds` |

## 13. 验证方式

```bash
mvn -f mango/pom.xml \
  -pl :mango-file-preview-api,:mango-file-preview-core,:mango-file-preview-engine,:mango-file-preview-support,:mango-file-preview-starter,:mango-file-preview-starter-remote \
  test

mvn -f mango/pom.xml -pl :mango-file-preview-app -am \
  -Dtest=MangoFilePreviewAppFlowTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

正式验收还必须使用全新 MySQL 启动单体，并在浏览器执行真实上传、创建预览链接、打开预览页和下载；微服务形态应分别启动文件能力与预览能力，确认 Feign 服务路由保留 `/file/files` 基础路径、上下文透传和源文件正文；独立引擎形态还需验证 `mango.file-preview.remote.enabled=true` 下七类格式（doc/docx、ppt/pptx、xls/xlsx、pdf、图片、mp4、zip）的端到端预览。

## 14. 相关文档

- [Mango File](../mango-file/README.md)
- [Fileproc](../../mango-infra/mango-infra-fileproc/README.md)
- [@mango/file](../../../mango-ui/packages/file/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)

### Issue #661 文件打包大小控制影响

`mango-file` 的公共 `FileApi` 新增 `packageFilesWithSizeControl` 后，本模块测试替身同步该方法以保持消费者编译兼容。文件预览模块不调用新打包入口，预览 URL、源文件 token、转换缓存、权限、租户和浏览器验收流程均不变。

### Issue #1171 xdocreport 旧 schema 依赖移除

`mango-file-preview-engine` 不再传递 `fr.opensagres.xdocreport:org.apache.poi.xwpf.converter.core` 和 `org.apache.poi.xwpf.converter.xhtml`。这两个转换器未被引擎代码使用，却会传递 POI 3.x 时代的 `org.apache.poi:ooxml-schemas:1.1`，与 POI 5.2.5 的 `poi-ooxml-lite` 冲突，使消费方基于 POI 5 的 `WorkbookFactory` 解析抛 `NoSuchFieldError ... DocumentFactory Factory`。

- 影响：`mango-file-preview-engine` 和 `mango-file-preview-starter` 的依赖树不再包含 `org.apache.poi:ooxml-schemas`；引擎仍保留 `fr.opensagres.xdocreport.document`，因为 `OnlinePreviewController` 使用其中的 `IOUtils`。
- 需要做的事：业务项目升级到包含本修复的引擎或 starter 版本后，删除应用 POM 里为 `mango-file-preview-engine` 手工添加的 `org.apache.poi:ooxml-schemas` 排除；无需新增其它依赖。
- 验证：`mvn -o -pl :mango-file-preview-engine,:mango-file-preview-starter dependency:tree -Dincludes=org.apache.poi:ooxml-schemas` 无输出，`mvn -pl :mango-file-preview-engine test` 通过。
- 回滚：恢复这两个转换器依赖即可回到旧依赖图，但会同时恢复 POI 5 解析冲突。
