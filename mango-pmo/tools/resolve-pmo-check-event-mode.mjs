#!/usr/bin/env node

import path from "node:path";
import { appendFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const CHANGE_VALIDATION = "change-validation";
const CONTRACT_ONLY = "contract-only";

function normalized(value) {
  return String(value ?? "")
    .trim()
    .toLowerCase();
}

/**
 * Determines whether an event can safely run change-based validation.
 * Terminal PR metadata edits have no stable base/head diff in Gitea.
 */
export function resolvePmoCheckEventMode({
  eventName,
  eventAction,
  pullRequestState,
  pullRequestMerged,
} = {}) {
  const isTerminalPullRequest =
    normalized(pullRequestState) === "closed" ||
    normalized(pullRequestMerged) === "true";
  if (
    normalized(eventName) === "pull_request" &&
    normalized(eventAction) === "edited" &&
    isTerminalPullRequest
  ) {
    return CONTRACT_ONLY;
  }
  return CHANGE_VALIDATION;
}

function parseArgs(argv) {
  const result = {
    eventName: process.env.EVENT_NAME,
    eventAction: process.env.EVENT_ACTION,
    pullRequestState: process.env.PR_STATE,
    pullRequestMerged: process.env.PR_MERGED,
    output: process.env.GITHUB_OUTPUT || "",
  };
  const fields = new Map([
    ["--event-name", "eventName"],
    ["--event-action", "eventAction"],
    ["--pull-request-state", "pullRequestState"],
    ["--pull-request-merged", "pullRequestMerged"],
    ["--output", "output"],
  ]);
  for (let index = 0; index < argv.length; index += 1) {
    const field = fields.get(argv[index]);
    if (!field) continue;
    result[field] = argv[index + 1] ?? "";
    index += 1;
  }
  return result;
}

export function runPmoCheckEventModeCli(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  const mode = resolvePmoCheckEventMode(args);
  if (args.output) {
    appendFileSync(args.output, `mode=${mode}\n`);
  }
  process.stdout.write(`${JSON.stringify({ mode })}\n`);
  return 0;
}

const invokedPath = process.argv[1] ? path.resolve(process.argv[1]) : "";
if (invokedPath === fileURLToPath(import.meta.url)) {
  process.exitCode = runPmoCheckEventModeCli();
}
