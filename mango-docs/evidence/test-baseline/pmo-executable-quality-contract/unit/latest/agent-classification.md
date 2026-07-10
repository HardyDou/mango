# PMO 受控隔离新会话分类实验

- 结论：**FAIL**
- 时间：2026-07-10T06:58:50.461Z
- Codex：codex-cli 0.144.1
- 模型：gpt-5.6-sol
- 案例：14，每组每案例重复 3 次
- 隔离：每案例独立进程、临时目录、临时 HOME、仅含认证的临时 CODEX_HOME、ephemeral、忽略用户配置和项目 rules。
- 口径：只测 Agent 对 PMO 测试义务的结构化分类，不证明代码实现、测试运行或业务正确性。

| 指标 | Current PMO | Candidate PMO |
|---|---:|---:|
| Agent 分类精确匹配率 | 45.24% | 88.10% |
| 关键案例精确匹配率 | 41.67% | 86.11% |
| 放行/阻断 + 测试义务匹配率 | 71.43% | 92.86% |
| 风险等级匹配率 | 78.57% | 90.48% |
| 静态 Review 义务匹配率 | 76.19% | 90.48% |
| 完成运行 | 39/42 | 39/42 |

## 未精确匹配

- current/agent-r1-threshold-unit/#1: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}, actual={"risk":"R1","staticReviewRequired":false,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}
- current/agent-r1-threshold-unit/#2: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}, actual={"risk":"R1","staticReviewRequired":false,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}
- current/agent-r1-threshold-unit/#3: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}, actual={"risk":"R1","staticReviewRequired":false,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}
- current/agent-r1-protocol-behavior/#1: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}, actual={"risk":"R1","staticReviewRequired":false,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}
- current/agent-r1-protocol-behavior/#2: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}, actual={"risk":"R1","staticReviewRequired":false,"requiredTests":["UNIT"],"decision":"PASS","reasonCode":"KEY_LOGIC_UNIT"}
- current/agent-r1-direct-sut-mock/#2: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}, actual={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}
- current/agent-r1-direct-sut-mock/#3: expected={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}, actual={"risk":"R1","staticReviewRequired":true,"requiredTests":["UNIT"],"decision":"BLOCK","reasonCode":"USELESS_TEST"}
- current/agent-r2-real-api-flow/#1: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"COMPLEX_FLOW_API"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"COMPLEX_FLOW_API"}
- candidate/agent-r2-real-api-flow/#1: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"COMPLEX_FLOW_API"}, actual={"risk":"R2","staticReviewRequired":false,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"COMPLEX_FLOW_API"}
- current/agent-r2-service-entry-bypass/#1: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"API_ENTRY_BYPASS"}, actual={"risk":"R2","staticReviewRequired":true,"requiredTests":["API"],"decision":"BLOCK","reasonCode":"API_ENTRY_BYPASS"}
- current/agent-r2-service-entry-bypass/#2: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"API_ENTRY_BYPASS"}, actual={"risk":"R2","staticReviewRequired":false,"requiredTests":["API"],"decision":"BLOCK","reasonCode":"API_ENTRY_BYPASS"}
- current/agent-r2-service-entry-bypass/#3: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"API_ENTRY_BYPASS"}, actual={"risk":"R2","staticReviewRequired":true,"requiredTests":["API"],"decision":"BLOCK","reasonCode":"API_ENTRY_BYPASS"}
- current/agent-r2-wrapped-protected-mock/#1: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}
- current/agent-r2-wrapped-protected-mock/#3: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["API"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}
- candidate/agent-r2-wrapped-protected-mock/#3: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"BLOCK","reasonCode":"PROTECTED_PATH_MOCK"}
- current/agent-r2-external-wiremock/#1: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"EXTERNAL_BOUNDARY_MOCK"}, actual={"risk":"R2","staticReviewRequired":false,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"EXTERNAL_BOUNDARY_MOCK"}
- current/agent-r2-external-wiremock/#3: expected={"risk":"R2","staticReviewRequired":true,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"EXTERNAL_BOUNDARY_MOCK"}, actual={"risk":"R2","staticReviewRequired":false,"requiredTests":["UNIT","API"],"decision":"PASS","reasonCode":"EXTERNAL_BOUNDARY_MOCK"}
- current/agent-r3-internal-api-route-mock/#1: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"INTERNAL_API_MOCK"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["API","UI"],"decision":"BLOCK","reasonCode":"INTERNAL_API_MOCK"}
- current/agent-r3-internal-api-route-mock/#2: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"INTERNAL_API_MOCK"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["API","UI"],"decision":"BLOCK","reasonCode":"INTERNAL_API_MOCK"}
- current/agent-r3-internal-api-route-mock/#3: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"INTERNAL_API_MOCK"}, actual={"risk":"R3","staticReviewRequired":true,"requiredTests":["API","UI"],"decision":"BLOCK","reasonCode":"INTERNAL_API_MOCK"}
- current/agent-process-micro-button-layout/#1: expected={"risk":"R0","staticReviewRequired":true,"requiredTests":[],"decision":"PASS","reasonCode":"MECHANICAL_STATIC"}, actual={"risk":"R1","staticReviewRequired":true,"requiredTests":["UI"],"decision":"BLOCK","reasonCode":"UI_ASSERTION_MISSING"}
- current/agent-process-micro-button-layout/#3: expected={"risk":"R0","staticReviewRequired":true,"requiredTests":[],"decision":"PASS","reasonCode":"MECHANICAL_STATIC"}, actual={"risk":"R1","staticReviewRequired":true,"requiredTests":["UI"],"decision":"BLOCK","reasonCode":"UI_ASSERTION_MISSING"}
- current/agent-process-permission-disguised-as-micro/#1: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"USER_FLOW_UI"}, actual={"risk":"R2","staticReviewRequired":true,"requiredTests":["UI"],"decision":"BLOCK","reasonCode":"UI_ASSERTION_MISSING"}
- candidate/agent-process-permission-disguised-as-micro/#1: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"USER_FLOW_UI"}, actual=null
- candidate/agent-process-permission-disguised-as-micro/#2: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"USER_FLOW_UI"}, actual=null
- current/agent-process-permission-disguised-as-micro/#2: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"USER_FLOW_UI"}, actual=null
- current/agent-process-permission-disguised-as-micro/#3: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"USER_FLOW_UI"}, actual=null
- candidate/agent-process-permission-disguised-as-micro/#3: expected={"risk":"R3","staticReviewRequired":true,"requiredTests":["UNIT","API","UI"],"decision":"BLOCK","reasonCode":"USER_FLOW_UI"}, actual=null
