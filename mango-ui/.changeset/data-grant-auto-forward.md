---
'@mango/http-client': minor
'@mango/common': minor
---

新增数据权限 Grant 的自动接力（#977 PR2）：`createMangoHttpClient` 新增 `getUserId` 上下文 provider，收到列表响应的 `X-Mango-Data-Grant` 与 `X-Mango-Data-Grant-Domain` 后按 domain 缓存，后续详情请求自动以多值头 `X-Mango-Data-Grant` 回传（上限 16 个 / 4096 字符，超限按最近使用优先截断）；`absent`、`oversize`、`scope-unresolved` 与未知 Skipped 取值删除该域的键，`responseType: 'stream'` 的下载请求不携带；`@mango/common` 的旧单例 `utils/request` 同步接入，`Session.clearToken()` 会清空 Grant 缓存，登出与身份切换不再复用旧详情凭证。
