# Issue 387 Notice Action Scenarios Design

## Goal

Upgrade Mango site messages from passive text into controlled interaction messages for common platform scenarios.

The interaction target must use `targetType + targetKey + params`. Business modules must not persist or send arbitrary frontend page addresses as the interaction contract.

## Scope

Included:

- Notice API event contract carries scene, subject, target, data, and actions.
- Existing notice send listeners transfer the structured contract into send commands.
- Five platform modules provide scenario examples:
  - Workflow: approval task and process result messages.
  - Job: failed instance alarm.
  - Payment: payment order, refund order, exception, reconciliation, and settlement messages.
  - Identity: account opened, password reset, external identity bind/unbind.
  - Auth: login success and login locked security messages.
- Notice client page renders message actions and routes only by controlled target key.
- E2E covers message list UI, action button interaction, event action request, and named target navigation.

Excluded:

- No generic external web link support.
- No business-specific approval implementation inside notice.
- No new message template editor UI.
- No new role or permission model.

## Target Protocol

`targetType` values:

- `NONE`: message has no primary interaction target.
- `ROUTE`: `targetKey` is a registered menu or route name.
- `FLOW`: `targetKey` is a custom business interaction flow key.

`params` is a structured query-like map. It can contain business identifiers and filters, but must not be treated as a page address.

## Scenario Plan

| Module | Event | Target | Action |
|---|---|---|---|
| Workflow | `workflow.task.assigned` | `ROUTE`, `workflow:task:detail` | View approval task |
| Workflow | process completed/rejected/ended | `ROUTE`, task list menu keys | View process |
| Job | `job.instance.failed` | `ROUTE`, `job:instance` | View failed instance |
| Payment | order/refund success or failure | `ROUTE`, payment order menu keys | View payment or refund order |
| Payment | exception/reconciliation/settlement risk | `ROUTE` or `FLOW` | View or handle risk |
| Identity | user created/password reset/bind/unbind | `ROUTE`, `system:user` | View member account |
| Auth | login success/locked | `ROUTE`, account or user menu keys | View profile/security user |

Payment exception handling uses `FLOW` to demonstrate a custom interaction flow without embedding the flow implementation in notice.

## API Changes

`NoticeSendEvent` adds:

- `messageScene`
- `messageSubject`
- `messageTarget`
- `messageData`
- `messageActions`
- `messageExpireTime`

The local and remote send listeners copy those fields into `SendNoticeCommand`.

## Data Changes

The notice migration from the first part of this issue stores structured message target, subject, data, actions, and action request records.

No additional database migration is required for scenario wiring.

## Frontend Interaction

The notice client:

- Shows up to two available actions in the table operation column.
- Executes `EVENT` actions through the notice action API.
- Emits `ROUTE` and `FLOW` actions with `targetKey` and `params`.

The site message page:

- Opens targets through named target keys.
- Does not navigate by arbitrary message-provided address.
- Keeps empty/disabled target handling visible to the user.

## Test Plan

| ID | Level | Scenario | Automation |
|---|---|---|---|
| TC-387-API-001 | Unit/API | Notice event listener copies structured fields | AUTO |
| TC-387-API-002 | Integration | Five scenario commands persist target and actions | AUTO |
| TC-387-E2E-001 | E2E | Notice list renders structured actions and executes event action | AUTO |
| TC-387-E2E-002 | E2E | Notice action navigates by named target key | AUTO |
| TC-387-UI-001 | Screenshot | Notice message page has no overlapping action controls | AUTO |

