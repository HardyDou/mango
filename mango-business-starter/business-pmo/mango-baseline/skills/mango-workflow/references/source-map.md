# Mango Approval Workflow Source Map

Resolve paths from the current Mango source root. Read source files themselves before making implementation decisions.

## Capability Documents

| Purpose | Relative path |
|---|---|
| Backend overview | `mango/mango-platform/mango-workflow/README.md` |
| Business approval integration | `mango-docs/guides/business-integration/workflow-business-approval.md` |
| Frontend package | `mango-ui/packages/workflow/README.md` |
| Business component example | `mango-ui/packages/workflow-business-example/README.md` |

## Backend Contracts

| Purpose | Relative path or search |
|---|---|
| Java API package | `mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/` |
| Commands | `.../api/command/` |
| Enums | `.../api/enums/` |
| Runtime services | `rg -n "class .*Workflow.*Service" mango/mango-platform/mango-workflow` |
| Events | `rg -n "WorkflowEvent|DomainEvent|Subscriber" mango/mango-platform/mango-workflow` |

## Frontend And Validation

| Purpose | Relative path |
|---|---|
| HTTP APIs and types | `mango-ui/packages/workflow/src/api/workflow.ts` |
| Form config parser | `mango-ui/packages/workflow/src/workflowFormConfig.ts` |
| Definition page | `mango-ui/packages/workflow/src/views/workflow-definition/index.vue` |
| Designer | `mango-ui/packages/workflow/src/views/workflow-definition/components/workflow-designer/` |
| Start page | `mango-ui/packages/workflow/src/views/start-process/index.vue` |
| Task detail | `mango-ui/packages/workflow/src/views/task-detail/index.vue` |
| Management E2E | `mango-ui/apps/mango-admin/e2e/specs/workflow-management.spec.ts` |
| Business registration | `mango-ui/packages/workflow-business-example/src/register.ts` |
| Approval components | `mango-ui/packages/workflow-business-example/src/business-components/` |
