# Issue #1111 真实数据验证摘要

## 环境与数据

- 源数据：本机 MySQL `baohan_test` 测试环境数据，只读复制。
- 验证库：`mango_dev_mango_issue_1111_004`，当前 Mango worktree 专属库。
- 数据库：MySQL `127.0.0.1:3306`，账号标识 `root`；未记录口令。
- 迁移前 `resource_registry`：2652 行，`AUTO=1765`，`INIT_ONLY=887`。

## 迁移结果

执行：

```bash
cat mango/mango-platform/mango-resource/mango-resource-core/src/main/resources/db/migration/resource/V5__normalize_legacy_resource_sync_modes.sql \
  | mysql --host=127.0.0.1 --port=3306 --user=root mango_dev_mango_issue_1111_004
```

结果：

- 影响 2652 行。
- `AUTO` 全部归一为 `SYNC_ALWAYS`，共 1765 行。
- `INIT_ONLY` 全部归一为 `SYNC_ONCE`，共 887 行。
- `AUTO/INIT_ONLY/MANUAL/LOCKED` 查询结果为 0 行。
- 迁移前后受保护 Registry 字段 SHA-256 相同；该 Hash 排除了 `sync_mode` 和数据库自动更新的 `updated_at`：
  - before：`1c199ab84e58b0c83660f990518cb2a9123c083ae44c21e65842b54070c2f17f`
  - after：`1c199ab84e58b0c83660f990518cb2a9123c083ae44c21e65842b54070c2f17f`
- `AUTO` ID 集合 → `SYNC_ALWAYS` ID 集合：`f1aecdddea45725e121b8e99d2d7b762bf3f4178e410f2f1fb6d595f34944fa8`
- `INIT_ONLY` ID 集合 → `SYNC_ONCE` ID 集合：`b36237678b516887455ac09c7aebbd21a84b0df6bf7f7dad1d233f9810733b70`

## 幂等性

同一 SQL 在验证库第二次执行：

- 影响 0 行。
- 旧值仍为 0。
- 资源 ID 集合未发生变化。

## baohan-system 启动观察

参考正常测试环境的处理方式，在验证库执行了“先备份、再删除旧 Registry 登记及关联同步/变更日志、保留业务目标表”的清理：

- `resource_registry` 备份 1 行。
- `resource_sync_log` 备份 5 行。
- `resource_change_log` 备份 5 行。
- 删除后 `authorization_api_resource.id=6673508641616300253` 仍存在。

重新启动 `baohan-system` 后：

- `/actuator/health` 返回 `UP`。
- Bootstrap 状态为 `FINALIZED`。
- `api.mango-link.GET.link.open.public-links.list` 重新登记为 `resource_id=807245613893456101`、`module_code=link`、`sync_mode=SYNC_ALWAYS`。
- `target_id=6673508641616300253` 保持不变。
- 日志未出现 `deprecated sync mode`；启动阶段未再出现原 `mango-link` 冲突。
- 冒烟接口 `GET /link/open/public-links/list?tenantId=1` 返回 HTTP 200、业务 `code=200`，返回 24 条真实数据。
- 异常边界 `GET /link/open/public-links/list` 返回 HTTP 200、业务 `code=400`，明确提示“租户上下文不能为空”。

启动日志：`.runtime/issue-1111/baohan-system-smoke-round2.log`。
冒烟响应：`.runtime/issue-1111/link-smoke-*.json`。

补充：运行约 1 分钟后，eventual worker 观察到另一条既有的 `guarantee-verification` 资源身份冲突；该冲突与 Issue #1111 无关，未删除该业务登记，避免扩大清理范围。

### 独立冲突的候选配置验证

在不修改业务源文件的前提下，临时追加以下运行参数，排除该固定 YAML 已声明接口的自动扫描：

```text
--mango.authorization.resource-sync.exclude-paths=/error,/actuator/**,/guarantee/verification/open/records/verify
```

等待约 70 秒后：

- health=UP。
- Bootstrap=`FINALIZED`。
- `GET /link/open/public-links/list?tenantId=1` 返回业务 `code=200`，24 条数据。
- `guarantee-verification` 冲突：0 条。
- 原 `mango-link` 冲突：0 条。
- `deprecated sync mode`：0 条。

这证明“排除固定声明接口的自动扫描”可以消除本次独立冲突；正式修复仍需由业务项目确认配置范围，并单独登记/评审，不能直接作为 Issue #1111 的隐式改动。该问题已登记为 baohan-system Issue #1829：https://git.inner.yunxinbaokeji.com/baohan/baohan-system/issues/1829。

候选配置验证日志：`.runtime/issue-1111/baohan-system-smoke-exclude-path.log`。

原始迁移、计数、Hash、二次执行和清理校验输出保存在：`.runtime/issue-1111/`
