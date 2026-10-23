# mango-deploy 脚本调整提案（Issue #1106 配套）

## 结论

平台侧已把 Bootstrap 生命周期收敛为幂等契约（`mango/mango-infra/mango-infra-bootstrap/README.md` §7.1）。`bin/mango-deploy` 的 `commit_release` 应按契约删掉两处反推控制表的逻辑，改为统一序列。

## 契约依据

- §7.1.1 统一调用序列：同一构建版本重发沿用升级序列，`apply` 与 `finalize` 都幂等返回，不需要任何预检。
- §7.1.3 成功判定：只看退出码；禁止依赖 `executionId`、执行表行数、日志文本、控制表字段。
- 契约变更点：`finalize` 对"已是 stable 且 fingerprint 相同"的 generation 返回 `FINALIZED`、0 步，不再报 `BOOTSTRAP_CANDIDATE_MISSING`。

## 需要删除的内容

| 位置 | 现状 | 删除原因 |
|---|---|---|
| `meta_get DATA_UPGRADE == skipped` 分支 | 数据升级跳过时先比对控制表身份，不一致才 reconcile | 属于反推控制表，契约 §7.1.1 明确禁止 |
| `platform_control_matches_manifest` 及其调用 | 用控制表字段判断"是否需要 finalize" | 同一原因；且该判断为假时走的 plan/apply/finalize 正是原缺陷路径 |

保留 `platform_control_matches_manifest` 函数体本身不影响，但不得再用于决定 whether-to-finalize。

## 调整后的 `commit_release` 片段

```bash
if [[ "$(jq -r '.deploy.dataUpgrade.type // empty' "$config")" == mango-bootstrap ]]; then
  if [[ "${RELEASE_MODE:-INCREMENTAL}" == RESET ]]; then
    run_mango_bootstrap verify
  else
    run_mango_bootstrap plan
    run_mango_bootstrap apply
    run_bootstrap_finalize
    run_mango_bootstrap verify
  fi
  write_phase FINALIZED
else
  run_required_hook
fi
```

## 行为对照

| 场景 | 调整前 | 调整后 |
|---|---|---|
| 空库首次发布 | 走 `else` 分支 finalize（依赖 apply 已产生候选） | 同左，`apply --strategy=cold` 已内含 finalize，随后 finalize 幂等返回 |
| 版本升级 | finalize 正常提交 | 不变 |
| 同一构建版本重发 | 控制表判定为假 → plan/apply/finalize，finalize 报 `BOOTSTRAP_CANDIDATE_MISSING` | apply 幂等返回 `FINALIZED`，finalize 幂等返回 `FINALIZED`，进入 `SUCCEEDED` |
| DATA_UPGRADE=skipped | 依赖身份比对决定是否 finalize | 无条件走统一序列，靠平台幂等收敛 |
| RESET | 只 verify | 不变 |

## 需要引擎确认的三点

1. `run_mango_bootstrap apply` 是否传入显式 strategy。契约要求空库用 `cold`、升级用 `rolling`；同版本重发两种策略都幂等。
2. `run_bootstrap_finalize` 是否传 `--mango.release.fingerprint`。传入时必须与运行时计算一致，否则会在任何 DB 操作前报 `BOOTSTRAP_FINGERPRINT_MISMATCH`；这与调整前行为一致。
3. 失败回滚逻辑是否对 `FINALIZED` 的 generation 调用 `abort`。契约 §7.1.5 规定该调用会返回 `BOOTSTRAP_CANDIDATE_MISSING`，应改为用上一版本清单重新走统一序列。

## 不在本提案范围

- `write_phase FINALIZING` 在清维护与入口复核之前。任何 finalize 失败都会停在 `FAILED_MAINTENANCE` 且拒绝自动回滚。本次修复消除了这个具体触发源，但设计本身仍在，建议单独登记。
