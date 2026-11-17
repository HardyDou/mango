---
'@mango/pmo': patch
---

新增 `tools/check-surefire-versions.mjs`：解析 reactor 与父链，校验每个模块的有效 Surefire 版本不低于 3.0.0，并禁止在根 POM 与 `mango-parent` 之外自行钉版本。配合根 POM 统一声明 Surefire 3.2.5，避免模块回落到 2.x 后 `Tests run: 0` 让测试静默空跑。
