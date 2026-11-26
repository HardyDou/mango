---
'@mango/cli': patch
---

`mango dev start <app|group>` 不再因为 `dependsOn` 带入的依赖 app 端口被外部进程占用而整组失败：这类依赖会打印跳过原因后继续启动其余 app，被点名的目标 app 仍然直接失败。后端已在跑、只想重启前端时可以直接 `mango dev start frontend`。
