# create-mango-app

Mango 业务项目初始化 CLI。

```bash
npm create mango-app@latest guarantee-platform -- --module guarantee --aggregate letter --package com.example.business
```

或：

```bash
mango init guarantee-platform --module guarantee --aggregate letter
```

当前包基于仓内 `mango-business-starter` 生成项目骨架。发布为独立 npm 包前，需要把 starter 资产同步到发布包或通过 `--template` 指定模板路径。
