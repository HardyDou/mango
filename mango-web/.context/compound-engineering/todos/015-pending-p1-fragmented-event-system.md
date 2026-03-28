---
name: fragmented-event-system
description: Three different event mechanisms used for the same logical flow - mittBus, local window events, raw CustomEvent
type: code-review
status: complete
priority: p1
issue_id: "015"
tags: [code-review, architecture, events, p1, resolved]
dependencies: []
---

## Problem Statement

The columns menu uses **THREE different event mechanisms** that are NOT compatible with each other:

**Mechanism 1 - Proper mitt bus** (`@/utils/mitt`):
```typescript
// settings.vue uses this
import { mittBus } from '@/utils/mitt';
mittBus.emit('layoutMobileResize', {...});
```

**Mechanism 2 - Local window events in columnsAside.vue:179-188**:
```typescript
const mittBusEmit = (name: string, data?: unknown) => {
  window.dispatchEvent(new CustomEvent(name, { detail: data }));
};
const mittBusOn = (name: string, callback: (data: unknown) => void) => {
  window.addEventListener(name, handler as EventListener);
};
```

**Mechanism 3 - Local window events in aside.vue:73-81**:
```typescript
// SAME pattern but different scope!
const mittBusOn = (name: string, callback: (data: unknown) => void) => {
  window.addEventListener(name, handler as EventListener);
};
```

## Findings

- **Files**:
  - `src/layout/navBars/breadcrumb/settings.vue` - uses `@/utils/mitt`
  - `src/layout/component/columnsAside.vue:179-188` - local implementation
  - `src/layout/component/aside.vue:73-81` - local implementation (DUPLICATE!)

- **Evidence**: Code simplicity review identified duplicate implementations
- **Impact**: Events may fire but handlers in other components don't receive them

## Root Cause

The columnsAside and aside components define their OWN local `mittBusEmit`/`mittBusOn` functions. These are NOT the same as `@/utils/mitt`. Events fired by columnsAside's local mittBusEmit go nowhere if aside.vue is listening on `@/utils/mitt`.

## Proposed Solutions

### Option A: Unify on @/utils/mitt (Recommended)
**Pros**: Single source of truth, type safety, discoverable
**Cons**: Small refactor
**Effort**: 30 minutes

Remove local implementations from columnsAside.vue and aside.vue. Import and use `@/utils/mitt` everywhere.

### Option B: Remove mitt entirely, use provide/inject
**Pros**: Native Vue reactivity
**Cons**: More refactoring
**Effort**: 2-3 hours

Use Vue's `provide`/`inject` for cross-component communication instead of custom event bus.

### Option C: Remove mitt entirely, use store state
**Pros**: Simplest, all state in Pinia
**Cons**: May overcomplicate simple hover behavior
**Effort**: 1 hour

Instead of events, use `routesList.isColumnsMenuHover` and computed properties.

## Recommended Action

[To be filled during triage]

## Technical Details

**Event names that must match** (currently manual, error-prone):
- `'setSendColumnsChildren'`
- `'restoreDefault'`

**Affected Files**:
- `src/layout/component/columnsAside.vue`
- `src/layout/component/aside.vue`
- `src/utils/mitt.ts`

## Acceptance Criteria

- [x] Single event mechanism used throughout
- [x] No duplicate mittBusEmit/mittBusOn implementations
- [x] Events fire and are received correctly

## Work Log

- 2026-03-28: Identified during code review
- 2026-03-28 15:59: Fixed in commit 74964d9 - Unify event bus, use @/utils/mitt everywhere
