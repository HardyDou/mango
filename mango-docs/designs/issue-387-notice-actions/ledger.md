# Issue 387 Notice Action Scenarios Ledger

## Delivery Contract

Target: implement controlled interaction messages across at least five Mango modules and verify API, E2E, and UI behavior.

Business developer handoff: use `NoticeSendEvent` with structured `messageTarget` and `messageActions`. Use `FLOW` for custom interaction flows and `ROUTE` for registered menu or route names.

## Items

| ID | Source | Requirement | Design Decision | Code Deliverable | README/Usage | Design Doc | E2E Script | Test Baseline | Acceptance | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|---|
| AC-387-001 | User | No arbitrary frontend address in message interaction | Use `targetType + targetKey + params` | Notice API/core/frontend | This ledger | design.md | notice E2E | evidence baseline | API and UI checks | CANDIDATE | pending |
| AC-387-002 | User | Workflow messages support approval scenarios | Add workflow targets/actions | Workflow subscriber | This ledger | design.md | notice E2E | evidence baseline | Integration/E2E | CANDIDATE | pending |
| AC-387-003 | User | Job messages support failure alarm scenario | Add job target/actions | Job alarm service | This ledger | design.md | notice E2E | evidence baseline | Integration/E2E | CANDIDATE | pending |
| AC-387-004 | User | Payment messages support multiple risk scenarios | Add payment targets/actions including custom flow | Payment notification service | This ledger | design.md | notice E2E | evidence baseline | Integration/E2E | CANDIDATE | pending |
| AC-387-005 | User | Identity messages support account security scenarios | Add identity targets/actions | Identity user service | This ledger | design.md | notice E2E | evidence baseline | Integration/E2E | CANDIDATE | pending |
| AC-387-006 | User | Auth messages support login security scenarios | Add auth targets/actions | Auth controller | This ledger | design.md | notice E2E | evidence baseline | Integration/E2E | CANDIDATE | pending |
| AC-387-007 | User | UI and page interaction pass | E2E asserts buttons, navigation, action call, screenshot | Notice client page | This ledger | design.md | notice E2E | evidence baseline | Playwright | CANDIDATE | pending |

## Test Cases

| Case ID | Source | Scenario | Priority | Level | Automation | Test Data | Stable Contract | Entry | Evidence | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-387-API-001 | AC-387-001 | Structured notice event transfers to send command | P0 | API | AUTO | Unit event object | VO fields | Maven test | pending | CANDIDATE |
| TC-387-API-002 | AC-387-002..006 | Five module scenario commands include controlled targets/actions | P0 | API | AUTO | In-memory commands | target/action fields | Maven test | pending | CANDIDATE |
| TC-387-E2E-001 | AC-387-007 | User sees action buttons and executes event action | P0 | E2E | AUTO | Intercepted notice data | visible text and request | Playwright | pending | CANDIDATE |
| TC-387-E2E-002 | AC-387-007 | User opens a message target by name | P0 | E2E | AUTO | Intercepted route data | URL and page content | Playwright | pending | CANDIDATE |
| TC-387-UI-001 | AC-387-007 | Notice table actions do not overlap | P1 | Screenshot | AUTO | Intercepted notice data | screenshot and console checks | Playwright | pending | CANDIDATE |

