# PMO 可执行质量空白上下文 A/B 实验报告

- 时间：2026-07-10T02:18:33.462Z
- 场景数：48
- 隔离运行数：192（普通场景 3 次，关键场景 5 次）
- 隔离条件：每次使用独立临时目录、空 HOME、空 CODEX_HOME、固定 UTC、代理禁用、无会话历史。

## 结果

| 指标 | Current executable checks | Candidate quality gate | 阈值 |
|---|---:|---:|---:|
| 总体正确率 | 21.35% | 100.00% | ≥ 95% |
| 关键红线检出率 | 4.17% | 100.00% | 100% |
| 合法正例误报 | 0 | 0 | 0 |
| 重复结论一致率 | 100.00% | 100.00% | ≥ 95% |
| 相对提升 | - | 78.65 个百分点 | ≥ 30 个百分点 |

结论：**PASS**

## 场景明细

| 场景 | 期望 | 关键 | Current | Candidate | Candidate 规则 |
|---|---|---:|---|---|---|
| noop-constant-assert | BLOCK | 是 | PASS | BLOCK | PQT-NOOP-003 |
| noop-self-assert | BLOCK | 是 | PASS | BLOCK | PQT-NOOP-002 |
| noop-getter-setter | BLOCK | 否 | PASS | BLOCK | PQT-NOOP-004 |
| noop-does-not-throw | BLOCK | 否 | PASS | BLOCK | PQT-NOOP-005 |
| noop-not-null | BLOCK | 否 | PASS | BLOCK | PQT-NOOP-006 |
| mock-tested-target | BLOCK | 是 | BLOCK | BLOCK | PQT-MOCK-003 |
| mock-protected-node-under-alias-test | BLOCK | 是 | PASS | BLOCK | PQT-MOCK-004 |
| api-direct-service | BLOCK | 是 | PASS | BLOCK | PQT-API-001 |
| api-mocks-service | BLOCK | 是 | PASS | BLOCK | PQT-API-002 |
| api-mocks-mapper | BLOCK | 是 | PASS | BLOCK | PQT-API-002 |
| verify-only-mock | BLOCK | 否 | PASS | BLOCK | PQT-NOOP-007 |
| valid-key-decision-unit | PASS | 否 | PASS | PASS | - |
| valid-external-client-mock-unit | PASS | 否 | PASS | PASS | - |
| valid-real-entry-api | PASS | 否 | PASS | PASS | - |
| controller-direct-mapper | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-001 |
| controller-write-without-validation | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-002 |
| valid-controller-boundary | PASS | 否 | PASS | PASS | - |
| service-write-without-transaction | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-003 |
| valid-service-transaction | PASS | 否 | PASS | PASS | - |
| mapper-unconditional-update | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-004 |
| api-depends-core | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-005 |
| core-depends-starter | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-005 |
| remote-depends-core | BLOCK | 是 | PASS | BLOCK | PQT-JAVA-005 |
| valid-core-depends-api | PASS | 否 | PASS | PASS | - |
| ui-mocks-own-api | BLOCK | 是 | PASS | BLOCK | PQT-UI-001 |
| valid-ui-mocks-third-party | PASS | 否 | PASS | PASS | - |
| ui-element-selector | BLOCK | 否 | PASS | BLOCK | PQT-UI-002 |
| ui-fixed-wait | BLOCK | 否 | PASS | BLOCK | PQT-UI-003 |
| ui-force-click | BLOCK | 否 | PASS | BLOCK | PQT-UI-004 |
| ui-order-selector | BLOCK | 否 | PASS | BLOCK | PQT-UI-005 |
| ui-screenshot-only | BLOCK | 是 | PASS | BLOCK | PQT-UI-006 |
| valid-ui-business-and-screenshot | PASS | 否 | PASS | PASS | - |
| page-missing-capability-anchor | BLOCK | 否 | PASS | BLOCK | PQT-WEB-001 |
| page-hidden-compliance-anchor | BLOCK | 是 | PASS | BLOCK | PQT-WEB-002 |
| form-missing-surface | BLOCK | 否 | PASS | BLOCK | PQT-WEB-003 |
| form-missing-field-anchor | BLOCK | 否 | PASS | BLOCK | PQT-WEB-004 |
| valid-form-contract | PASS | 否 | PASS | PASS | - |
| contract-unknown-test-type | BLOCK | 是 | PASS | BLOCK | PQT-CONTRACT-010, PQT-CONTRACT-011 |
| contract-r2-missing-api | BLOCK | 是 | PASS | BLOCK | PQT-CONTRACT-011 |
| contract-r0-over-testing | BLOCK | 是 | PASS | BLOCK | PQT-NOOP-001 |
| contract-double-protected-node | BLOCK | 是 | PASS | BLOCK | PQT-MOCK-002 |
| contract-valid-external-double | PASS | 否 | PASS | PASS | - |
| contract-expired-exception | BLOCK | 是 | PASS | BLOCK | PQT-EXCEPTION-002 |
| contract-forbidden-exception | BLOCK | 是 | PASS | BLOCK | PQT-EXCEPTION-003 |
| contract-weak-acceptance | BLOCK | 否 | PASS | BLOCK | PQT-CONTRACT-008 |
| contract-valid-r3 | PASS | 否 | PASS | PASS | - |
| baseline-multiple-current-versions | BLOCK | 是 | PASS | BLOCK | PQT-BASELINE-001 |
| baseline-only-latest | PASS | 否 | PASS | PASS | - |

