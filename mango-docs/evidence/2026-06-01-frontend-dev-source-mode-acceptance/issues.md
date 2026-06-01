# 2026-06-01 Frontend Dev Source Mode Acceptance Issues

本文件记录本次真实浏览器验收发现并已登记到 GitHub 的问题。

## Issues

| Issue | 严重级别 | 现象 | 影响 | 证据 |
| --- | --- | --- | --- | --- |
| [#43](https://github.com/HardyDou/mango/issues/43) | Medium | 控制台提示 `X-Frame-Options` 只能通过 HTTP header 设置，`frame-ancestors` 通过 meta 会被忽略 | 安全策略声明无效，安全评审可能误判实际防护能力 | `acceptance-summary.json` |
| [#44](https://github.com/HardyDou/mango/issues/44) | Medium | `/system/user` 分页 `total` 期望 Number，实际收到 String `"1"` | API/UI 契约不一致，后续分页行为和测试稳定性有风险 | `acceptance-summary.json`、`02-system-user-page.png` |
| [#45](https://github.com/HardyDou/mango/issues/45) | Low | Element Plus `ElPagination` 报 deprecated usage warning | Element Plus 升级风险和 QA 控制台噪声 | `acceptance-summary.json` |

## 验收结论

上述问题未阻断本次主链路验收：页面访问、系统配置新增/删除、字典搜索均通过真实接口和浏览器验证。问题已作为后续修复项独立跟踪，修复时需要重新执行至少 `/system/user` 和分页相关页面的真实浏览器回归。
