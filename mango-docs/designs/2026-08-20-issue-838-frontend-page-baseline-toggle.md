# Issue #838 前端页面基线项目级开关治理设计

## 背景与目标

业务项目当前只要被范围分类器判定为前端变更，就会执行页面骨架基线检查。直接删除 GitHub/Gitea
托管 Workflow 步骤或修改锁定 baseline 会造成 PMO 漂移，删除整个 `pmo-doc-check` 又会破坏稳定
required check 身份。

目标是以业务仓 `mango.config.json` 为唯一项目级配置源，只关闭前端页面基线这一项检查，同时保留
风险合同、业务文档、后端质量和稳定的 `pmo-doc-check` 汇总结果。

## 范围与边界

包含：

- `pmoChecks.frontendPageBaseline` 配置合同与 fail-closed 解析。
- 页面基线 checker、范围分类器和 GitHub/Gitea 托管 Workflow 的统一语义。
- CLI init、PMO sync/upgrade/check 的生成、迁移、保留和校验。
- PMO package、Business Starter baseline 投影、能力说明和回归测试。

不包含：

- 关闭整个 `pmo-doc-check` 或改变 required check 名称。
- 为其它 PMO 检查增加通用开关框架。
- 业务仓私有脚本、Workflow fallback 或发布制品。

## 决策

项目配置固定为：

```json
{
  "pmoChecks": {
    "frontendPageBaseline": false
  }
}
```

- 缺少 `mango.config.json`、`pmoChecks` 或目标字段时默认启用，保持旧项目行为。
- 仅显式布尔值 `false` 关闭；字符串、数字、数组、空值、错误 JSON 和错误对象类型全部失败。
- classifier 输出 `frontend_page_baseline_enabled`。GitHub 跳过独立 frontend job，Gitea 跳过对应步骤；两者继续产生同一个稳定汇总结果。
- checker 直接执行时也读取同一配置；关闭时输出可审计的 `SKIP`，不读取 diff 或前端目录。
- 新项目显式生成 `true`。sync/upgrade 为已有配置补默认值并保留显式 `false`；locked check 校验配置类型。

## 风险与恢复

- 需求影响：L3。改变业务仓 PMO 核心门禁的项目级执行语义，影响 GitHub、Gitea 和 CLI 升级路径。
- 方案风险：L3。错误默认值或条件投影可能静默跳过门禁，错误迁移可能覆盖业务配置。
- 最终风险：L3，FULL 治理；M01=CREATE。
- 恢复方式：整体回退配置解析、Workflow 条件和 CLI 投影；旧项目缺省启用语义不依赖迁移数据。

## 验收

- AC-001：缺省配置保持页面基线启用，新项目显式生成 `true`。
- AC-002：显式布尔 `false` 使 checker、GitHub 和 Gitea 只跳过页面基线。
- AC-003：非法 JSON、对象类型或开关值在 classifier、checker 和 locked check 中失败。
- AC-004：sync/upgrade 补齐缺省字段并保留显式 `false`，不会覆盖其它项目配置。
- AC-005：PMO package、Business Starter baseline 和 CLI 生成投影一致。
- AC-006：稳定 `pmo-doc-check` 汇总、风险合同、文档和后端门禁语义不变。
