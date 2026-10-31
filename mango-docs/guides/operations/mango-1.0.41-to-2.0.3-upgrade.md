<!-- mango-release-upgrade-guide: manual -->
# Mango npm Notice 动态菜单路由升级说明

本批次为 npm-only 发布，不发布 Maven/docs，不包含数据库迁移、后端接口变更或服务停机。

## 1. 发布范围

升级 release plan 中的完整 20 包 npm tuple，终点为 `@mango/cli@1.3.8`。具体版本以 `mango-ui/.changeset/release-plan.json` 为准。

## 2. 变更内容

- Notice ROUTE 支持解析当前用户已授权的动态 ShellMenu 路径。
- 站内信和顶部铃铛动作保留 `receiptId`、`orderNo`、`focus` 等 query 参数。
- 自定义 `interaction` 事件继续由业务宿主处理。
- 未授权、缺失或不安全目标不会导航。

## 3. 升级步骤

1. 按 release plan 安装完整 npm tuple，不混用不同发布批次的包。
2. 执行业务项目安装、构建和受影响页面回归。
3. 使用有动态菜单权限的测试账号，从站内信和顶部铃铛分别验证 Notice ROUTE。
4. 验证无权限或不存在目标不会导航，并验证自定义 interaction payload 未变化。

## 4. 验证与回退

- Mango 源仓已验证 `@mango/admin-extension` 6 tests、`@mango/notice` 54 tests、`@mango/admin-shell` 100 tests，以及三个包构建。
- 消费项目应从 npm-group registry 解析精确版本并完成生产构建。
- 回退时恢复成功基线中的完整 npm tuple；不可覆盖已发布坐标。
