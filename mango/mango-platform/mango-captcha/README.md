# Mango Captcha 验证码模块

## 模块职责

验证码模块提供**纯能力**，只负责：
- **生成** - 根据类型生成验证码
- **存储** - 存储验证码答案（Redis/DB/Memory）
- **验证** - 核对用户输入的答案

**不负责：**
- 哪些接口需要验证码
- 何时触发验证码
- 验证码失败的处理

## 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         验证码模块（纯能力）                       │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ CaptchaApi   │  │ Storage      │  │ 类型                │ │
│  │ - generate() │  │ Redis/DB/   │  │ ARITHMETIC        │ │
│  │ - verify()   │  │ Memory       │  │ BLOCK_PUZZLE     │ │
│  │ - getTypes() │  │ 自动检测      │  │ SMS              │ │
│  └──────────────┘  └──────────────┘  │ EMAIL            │ │
│                                         └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 验证码类型

| 类型 | 说明 |
|-----|------|
| ARITHMETIC | 算术验证码（如：1+2=?） |
| BLOCK_PUZZLE | 滑块验证码 |
| SMS | 短信验证码 |
| EMAIL | 邮件验证码 |

## 存储策略

自动检测优先级：**Redis > DB > Memory**

可通过配置切换：
```yaml
mango:
  captcha:
    storage: auto  # auto/redis/db/memory
```

## 滑块验证码图库

滑块验证码默认使用模块内置图库：

```text
classpath:captcha/block-puzzle/workspace.jpg
classpath:captcha/block-puzzle/city.jpg
classpath:captcha/block-puzzle/garden.jpg
classpath:captcha/block-puzzle/pears.jpg
classpath:captcha/block-puzzle/village.jpg
classpath:captcha/block-puzzle/mountain.jpg
classpath:captcha/block-puzzle/courtyard.jpg
```

业务项目可以维护自己的图库，并通过配置替换默认图库。支持 `classpath:`、`file:`、`http:`、`https:` 路径；不写协议时按 `classpath:` 处理。

```yaml
mango:
  captcha:
    block-puzzle:
      width: 280
      height: 160
      slider-size: 50
      image-locations:
        - classpath:captcha/block-puzzle/office.jpg
        - classpath:captcha/block-puzzle/street.jpg
        - file:/data/mango/captcha/gallery/lobby.jpg
```

接口仍返回前端可直接渲染的 `backgroundImage`、`sliderImage`、`x`、`y`，前端不需要关心图片来自内置图库还是业务图库。

## API 接口

### 生成验证码

```
GET /captcha/types
GET /captcha/arithmetic
GET /captcha/block-puzzle
```

| 类型 | URL |
|-----|-----|
| 算术验证码 | GET /captcha/arithmetic |
| 滑块验证码 | GET /captcha/block-puzzle |

返回：
```json
{
  "code": 200,
  "data": {
    "key": "d37af63fb25348619f1e712c526baba2",
    "type": "ARITHMETIC",
    "image": "data:image/png;base64,...",
    "expireTime": 300
  }
}
```

### 验证验证码

```
POST /captcha/verify
Content-Type: application/json

{
  "key": "d37af63fb25348619f1e712c526baba2",
  "code": "32"
}
```

返回：
```json
{
  "code": 200,
  "success": true
}
```

### 查询支持的类型

```
GET /captcha/types
```

返回：
```json
{
  "code": 200,
  "data": {
    "types": ["ARITHMETIC", "BLOCK_PUZZLE", "SMS", "EMAIL"],
    "currentStorage": "REDIS"
  }
}
```

## 短信/邮件发送

```
POST /captcha/sms/send
POST /captcha/email/send
```

请求：
```json
{
  "mobile": "13800138000",
  "email": "test@example.com"
}
```

## 业务无感知集成

验证码模块支持**业务无感知**集成，通过外部配置决定哪些接口需要验证码：

### 流程

```
请求 → 拦截器（查配置）→ 需要验证码？
                           │
              ┌─────────────┴─────────────┐
              │                           │
          不需要                       需要
          放行                     检查 Header
                                        │
                           ┌────────────┴────────────┐
                           │                         │
                    Header为空                Header有值
                    返回 428                captchaApi.verify()
                           │                         │
                           ▼                         ▼
                     前端调验证码            验证通过→业务处理
                     重试请求
```

### Header 参数

| Header | 说明 |
|--------|------|
| X-Captcha-Key | 验证码标识 |
| X-Captcha-Code | 用户输入的答案 |

### 428 响应

接口需要验证码但未携带时，返回 HTTP 428：

```json
{
  "code": "CAPTCHA_REQUIRED",
  "msg": "请先完成验证",
  "data": "ARITHMETIC"
}
```

前端收到 428 后：
1. 调用验证码接口获取验证码
2. 弹出验证码让用户输入
3. 重新提交请求（带 Header）

## 外部配置接口

验证码模块通过 SPI 接口获取外部配置：

```java
public interface CaptchaConfigService {
    CaptchaConfig getConfig(String path);
}
```

外部系统（菜单/风控）实现此接口，提供：
- 哪些接口需要验证码
- 需要哪种类型的验证码

## 短信/邮件供应商

通过 SPI 接口扩展：

```java
public interface SmsProvider {
    void send(String mobile, String code);
}

public interface EmailProvider {
    void send(String email, String code);
}
```

## 模块结构

```
mango-captcha/
├── mango-captcha-api/           # 接口定义
│   └── spi/                    # SPI接口
│       ├── CaptchaStorage.java
│       ├── CaptchaConfigService.java
│       ├── SmsProvider.java
│       └── EmailProvider.java
├── mango-captcha-core/         # 核心实现
│   ├── service/                # 验证码服务
│   ├── storage/               # 存储实现
│   └── provider/              # 默认供应商
├── mango-captcha-starter/     # Spring Boot Starter
│   └── config/                # 自动配置
└── sql/                       # 数据库脚本
```

## 使用示例

### 后端业务代码（完全无感知）

```java
@PostMapping("/user/register")
public R<Void> register(@RequestBody RegisterRequest request) {
    // 业务代码，不需要知道验证码存在
    return userService.register(request);
}
```

### 前端请求封装

```typescript
async function request(url, options) {
  const res = await fetch(url, options);

  if (res.status === 428) {
    // 需要验证码
    const data = await res.json();
    const captchaRes = await fetch(`/captcha/${data.data.toLowerCase()}`);
    const captchaData = await captchaRes.json();

    // 弹出验证码
    const userCode = await showCaptchaModal(captchaData.data);

    // 重新提交
    options.headers = {
      ...options.headers,
      'X-Captcha-Key': captchaData.data.key,
      'X-Captcha-Code': userCode
    };

    return request(url, options);
  }

  return res.json();
}

// 业务调用
const result = await request('/user/register', {
  method: 'POST',
  body: JSON.stringify(formData)
});
```

### 菜单配置

```json
{
  "path": "/user/register",
  "meta": {
    "title": "注册",
    "captcha": {
      "type": "ARITHMETIC",
      "required": true
    }
  }
}
```

## 配置项

```yaml
mango:
  captcha:
    storage: auto              # 存储策略
    arithmetic:
      width: 200              # 图片宽度
      height: 100             # 图片高度
      expire: 300              # 过期时间(秒)
    header:
      key: X-Captcha-Key      # Header名
      code: X-Captcha-Code    # Header名
```

## 错误码

| 错误码 | 说明 |
|--------|------|
| CAPTCHA_REQUIRED | 需要验证码（HTTP 428） |
| CAPTCHA_ERROR | 验证码错误（HTTP 400） |
| CAPTCHA_EXPIRED | 验证码已过期（HTTP 400） |
