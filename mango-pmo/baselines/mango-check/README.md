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
