# Issue 99 文件上传访问地址回归报告

## 验证环境

- 工作树：`.mango/worktrees/issue-99-file-upload-url`
- 分支：`feature/issue-99-file-upload-url`
- 后端：`http://127.0.0.1:18366`
- 前端：`http://127.0.0.1:8056`
- 数据库：`mango_dev_debb2f`
- 租户：`tenantId=1`

## 覆盖范围

- 上传接口 `/file/files`
- 详情接口 `/file/files/detail`
- 预览接口 `/file/files/preview`
- 批量上传接口 `/file/files/batch`
- DIRECT 和 presigned 本地存储返回相对路径时的外部地址转换
- `X-Forwarded-Proto`、`X-Forwarded-Host`、`X-Forwarded-Port`、`X-Forwarded-Prefix` 反向代理头

## 验证命令

```bash
mvn -q -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -am test -Dcheckstyle.skip=true -Dtest=FileAccessUrlAssemblerTest,FilePreviewUrlBuilderTest,LocalFileStorageTest -Dsurefire.failIfNoSpecifiedTests=false

mvn -q -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-resource-sync-starter -am test -Dcheckstyle.skip=true -Dtest=AppModuleResourceManifestSyncRunnerTest -Dsurefire.failIfNoSpecifiedTests=false

mvn -q -f mango/pom.xml -pl mango-app/monolith/mango-monolith-app -am test -DskipTests -Dcheckstyle.skip=true

PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:8056 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18366 pnpm -F mango-admin exec playwright test e2e/specs/file-upload-url-contract.spec.ts --config playwright.config.ts --project chromium
```

## 结果

- 单元测试：通过。
- 后端 resource manifest 回归：通过。
- monolith 编译：通过。
- E2E：通过，1 个用例覆盖上传、详情、预览和批量上传。
- UI 识别：本轮为文件接口契约问题，使用 Playwright 生成回归报告截图作为证据；前端本地服务保持可访问。

## 结论

文件上传相关响应中的 `url`、`previewUrl`、`downloadUrl`、`directPreviewUrl`、`directDownloadUrl` 在反向代理头存在时均返回 `https://files.example.com/api/...` 形式的外部绝对地址，不再暴露缺少域名的相对路径。
