---
'@mango/file': patch
'@mango/common': patch
---

修复上传请求未声明 multipart Content-Type 的问题。此前宿主默认的 `application/json` 头会让 axios 把 FormData 序列化为 JSON，导致低于分片阈值的小文件直传被后端 `/file/files` 以非 multipart 请求拒绝（#1041）。

- `@mango/file`：`upload`、`uploadBatch`、`uploadServerPart` 的 FormData 请求显式声明 `multipart/form-data`。
- `@mango/common`：实例版 `createUploadApi(httpClient)` 的 `uploadFile`/`uploadImage`/`uploadExcel`/`uploadMultiple` 同样显式声明，与同文件旧版上传辅助保持一致。
