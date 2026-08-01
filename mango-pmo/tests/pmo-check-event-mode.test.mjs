import assert from "node:assert/strict";
import test from "node:test";

import { resolvePmoCheckEventMode } from "../tools/resolve-pmo-check-event-mode.mjs";

test("terminal Gitea PR body edits use contract-only validation", () => {
  for (const event of [
    {
      eventName: "pull_request",
      eventAction: "edited",
      pullRequestState: "closed",
      pullRequestMerged: "false",
    },
    {
      eventName: "pull_request",
      eventAction: "edited",
      pullRequestState: "open",
      pullRequestMerged: "true",
    },
  ]) {
    assert.equal(resolvePmoCheckEventMode(event), "contract-only");
  }
});

test("open PR changes retain full diff validation", () => {
  for (const event of [
    {
      eventName: "pull_request",
      eventAction: "opened",
      pullRequestState: "open",
      pullRequestMerged: "false",
    },
    {
      eventName: "pull_request",
      eventAction: "synchronize",
      pullRequestState: "open",
      pullRequestMerged: "false",
    },
    {
      eventName: "pull_request",
      eventAction: "reopened",
      pullRequestState: "open",
      pullRequestMerged: "false",
    },
    {
      eventName: "pull_request",
      eventAction: "edited",
      pullRequestState: "open",
      pullRequestMerged: "false",
    },
  ]) {
    assert.equal(resolvePmoCheckEventMode(event), "change-validation");
  }
});

test("push, manual, and incomplete events remain fail-closed change validation", () => {
  for (const event of [
    { eventName: "push" },
    { eventName: "workflow_dispatch" },
    { eventName: "pull_request", eventAction: "edited" },
    {},
  ]) {
    assert.equal(resolvePmoCheckEventMode(event), "change-validation");
  }
});
