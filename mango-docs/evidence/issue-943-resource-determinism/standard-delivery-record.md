# 标准交付记录

任务：Issue #943 ORG_MEMBER_BINDING 确定性修复

## 1. 元数据

- 任务 ID：Issue #943 release regression
- 交付模式：STANDARD
- 需求影响：L2 - Mango 1.0.53 的管理员根组织关系在独立空库生成不同主键，阻断业务仓 portable Resource cold baseline。
- 方案风险：L2 - 修改 Identity portable Resource 的持久化主键语义，但限定在 `ORG_MEMBER_BINDING` 新建关系路径；已有业务键关系不换 ID。
- 最终风险：L2
- 工作区决策：REUSE - `fix/issue-943-deterministic-org-member-id`

## 2. 目标与范围

- 目标：同一 `ORG_MEMBER_BINDING` 声明在独立空库生成相同的关系主键和 Resource `target_id`。
- 成功条件：声明可提供固定 `targetId`；未提供时按稳定业务身份派生；已有关系保持原 ID；两次空库真实 Mapper 结果一致。
- 处理范围：Identity Resource handler、默认管理员根组织声明、真实 Mapper 回归测试和能力说明。
- 不处理范围：不修改全局雪花 ID 策略、正常组织成员 API、数据库结构、baseline 比较器或历史数据库关系 ID。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R-01 | `ORG_MEMBER_BINDING` Resource handler | 独立空库中存在相同成员、组织和岗位稳定身份 | 新关系使用声明 `targetId`，缺省时按 `tenant_member_org + tenantId + memberId + orgCode` 派生 | 稳定 ID 已被其它关系占用时明确失败且不插入 | 两次空库的关系业务快照和 Handler `targetId` 完全一致 |
| R-02 | 已有数据库 Resource 重放 | 同业务键关系已经存在且 ID 可能由旧版本动态生成 | 复用已有关系 ID，只更新岗位和标志 | 不得删除、换主键或重复插入 | 幂等测试证明已有 ID 保持不变 |
| R-03 | 默认管理员根组织声明 | 加载 `identity.user.admin-root-org` | 声明携带固定正数关系 `targetId` 并提升版本 | 声明遗漏固定 ID 时合同测试失败 | 声明合同和真实 Handler 测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | R-01、R-03 | 复用 `PortableResourceIds.declaredOrStable`；派生身份使用租户、成员和组织编码，不依赖运行时组织主键 | Identity starter handler 和声明 | 回退本次提交 |
| D-02 | R-01 | 新建前按候选 ID 查询占用，冲突时 fail-closed | Identity starter handler | 回退碰撞检查 |
| D-03 | R-02 | 先按现有唯一业务键查询，命中后保留数据库 ID | Identity starter handler | 保留当前行为，不单独回滚 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I-01 | D-01 至 D-03 | 1 | `OrgMemberBindingResourceHandler` | 显式/派生稳定 ID、碰撞拒绝和已有关系兼容生效 |
| I-02 | D-01 | 2 | `identity-common-bootstrap.yml`、Identity README、能力地图 | 默认声明和消费者说明同步 |
| I-03 | D-01 至 D-03 | 3 | `OrgMemberBindingResourceHandlerIntegrationTest`、声明合同测试 | 双空库、显式 ID、幂等和碰撞用例通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R-01 至 R-03 | M10/M11 定向测试 | `mvn -B -ntp -f mango/pom.xml -pl :mango-identity-starter -am -Dtest=OrgMemberBindingResourceHandlerIntegrationTest,IdentityResourceDeclarationContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | 6 tests，0 failures，0 errors；覆盖两次空库重建、声明 ID、已有关系 ID 和碰撞失败 |
| R-01 至 R-03 | M11 真实 MySQL cold baseline | baohan-system 使用发布版 `mango-maven-plugin:1.0.53`、修复后的 `mango-identity-starter:1.0.53` 和 `-Pbsql` 执行 `package` | PASS | MySQL 8.4.8 上 replay、determinism 和 verify 三库验证通过；生成并打包 34 份 BSQL；fingerprint=`32a101ad79e4fc172bb137643425c3bbace480ada9a30e51349bfe66faed67ed` |
| R-01 至 R-03 | M09 模块质量检查 | `mvn -B -ntp -f mango/pom.xml -pl :mango-identity-starter verify` | PASS | 23 tests，0 failures，0 errors；模块 `BUILD SUCCESS` |
| R-01 至 R-03 | M08 文档与测试质量检查 | 运行 STANDARD 记录、模块 README、README 源事实、测试质量和后端 mock 审计 | PASS | STANDARD/README/测试质量检查通过；mock 审计 `block=0, warn=0` |

## 7. 例外与剩余风险

- 本地已用真实 MySQL 和 baohan-system 完成 cold baseline 生成验证；仍需独立发布新的 Mango Maven 版本，并由 baohan-system 升级后在正式 CI 构建中复核。发布和业务仓升级不在本次编码范围内。
