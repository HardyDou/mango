# create-mango-business

Mango 业务项目初始化 CLI。

```bash
npm create mango-business@latest guarantee-platform -- --module guarantee --aggregate letter --package com.example.business
```

或：

```bash
mango-business init guarantee-platform --module guarantee --aggregate letter
```

当前包基于仓内 `mango-business-template` 生成项目骨架。发布为独立 npm 包前，需要把模板资产同步到发布包或通过 `--template` 指定模板路径。
