# Mango Check Baseline

## Purpose

`no-new-violations-baseline.json` is the repository baseline for the `mango:check` `no-new-violations` gate.

It records existing violations that are not handled in the current task, so the gate can block only newly introduced issues. The baseline was created while handling issue #205.

## Local Command

Run from the `mango/` Maven root:

```bash
mvn mango:check \
  -Dmango.check.gate=no-new-violations \
  -Dmango.check.baselineFile=../mango-pmo/baselines/mango-check/no-new-violations-baseline.json \
  -Dmango.check.codeLevelExcludedModules=mango-platform/mango-file-preview \
  -DreportFile=target/mango-check-report.json
```

`mango-platform/mango-file-preview` is excluded only from code-level static analysis sources: PMD, Checkstyle and SpotBugs. Mango-specific rules from `mango-check` still run for that module.

## Update Flow

Update this file only when historical violations are intentionally resolved or the gate rules change.

1. Run the local command and inspect the generated report.
2. Confirm `newIssueCount` is `0`.
3. Replace `no-new-violations-baseline.json` with the new passing report baseline.
4. Record the verification command and summary in the PR.

Do not use this baseline to hide issues introduced by the current change.

## Fingerprint Form Migration

Baseline entries are keyed by the issue fingerprint. For Checkstyle `CyclomaticComplexityCheck` the fingerprint used to embed the configured threshold (`number:15,number:24`); since #1339 it records only the actual value (`number:24`). After upgrading the plugin, the first run reports the already-recorded entries as new, so step 2 of the update flow cannot reach `newIssueCount = 0` on its own.

Handle that case by checking the shape of the reported entries instead of the total:

1. Run the local command with the upgraded plugin.
2. Confirm every reported entry is a `CyclomaticComplexityCheck` entry and that their count stays within the number of entries carrying the old form (at most 20 for this repository).
3. Replace the baseline, then read the diff back and confirm the only changes are those fingerprints losing the threshold token — no entry from another rule moved, disappeared or was added.
4. Record both the run summary and the diff reading in the PR.

The replacement is a whole-file swap, so the report must be complete before you use it. A fingerprint form migration re-records existing violations under the new form; it does not waive anything introduced by the current change.
