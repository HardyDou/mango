---
'@mango/cli': patch
---

`mango dev start` 在每个带端口的 app 启动后打印实际监听地址；前端绑定回环时同时提示把 `MANGO_FRONTEND_HOST` 改成 `0.0.0.0` 才能被容器或其它机器访问。README 6.4.1 补充「前端默认只监听回环、后端默认绑定全部网卡」的差异与配置方法。
