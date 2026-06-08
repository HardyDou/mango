# Issue 107 Regression Report

Date: 2026-06-08
Worktree: `.mango/worktrees/issue-107-mango-check-reactor`
Branch: `feature/issue-107-mango-check-reactor`

## Scope

- Fixed `mango:check -Drule=static` so delegated PMD/Checkstyle/SpotBugs commands prefer the current Maven reactor projects.
- Added a per-delegated-command timeout via `mango.check.staticTimeoutSeconds`.
- Added command logging for delegated static-analysis goals.
- No business API, database schema, or frontend UI was changed.

## PMO Files Loaded

- `mango-pmo/rules/00-dev-flow.md`
- `mango-pmo/rules/03-ai-coding-redlines.md`
- `mango-pmo/agents/03-dev-agent.md`
- `mango-pmo/rules/02-dev-environment.md`
- `mango-pmo/rules/04-test-assets.md`
- `mango-pmo/rules/05-ai-delivery-quality.md`
- `mango-pmo/rules/backend/10-dev-flow.md`
- `mango-pmo/rules/backend/01-code.md`
- `mango-pmo/rules/backend/02-naming.md`
- `mango-pmo/rules/backend/08-test.md`
- `mango-pmo/rules/06-document-assets.md`
- `mango-pmo/agents/01-pm-agent.md`
- `mango-pmo/rules/product/01-prd-template.md`
- `mango-pmo/rules/product/02-sprint.md`

## Validation

| Command | Result | Notes |
| --- | --- | --- |
| `mvn -q -f mango/pom.xml -pl mango-tools/mango-maven-plugin -am test -Dcheckstyle.skip=true -Dtest=CheckMojoTest -Dsurefire.failIfNoSpecifiedTests=false` | PASS | Covers reactor scope resolution and delegated command timeout. |
| `mvn -q -f mango/pom.xml -pl mango-tools/mango-maven-plugin -am install -DskipTests -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dpmd.skip=true` | PASS | Installed current plugin snapshot for CLI-level verification. |
| `mvn -f mango/pom.xml -pl mango-platform/mango-job/mango-job-support,mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter-remote,mango-platform/mango-job/mango-job-starter -am mango:check -Drule=static -DskipTests -Dmango.check.staticTimeoutSeconds=15` | EXPECTED FAIL | Static analysis still fails on existing PMD/Checkstyle debt, but delegated commands now use the current Maven reactor scope. |

## Assertions

- `resolveStaticAnalysisProjects` uses `MavenSession#getProjects()` before falling back to recursive project discovery.
- Root aggregator execution no longer blindly scans the full repository when the caller used `-pl ... -am`.
- Delegated PMD, Checkstyle, and SpotBugs commands include explicit `-pl <reactor paths> -am` and are printed to Maven output.
- Hung delegated commands are killed after `mango.check.staticTimeoutSeconds` and return a message containing the goal and full command.

## Evidence Summary

- Unit test exit code: `0`.
- Job static validation output showed the delegated scope restricted to the selected reactor dependency closure:
  - `mango-platform/mango-job`
  - `mango-platform/mango-job/mango-job-api`
  - `mango-platform/mango-job/mango-job-support`
  - `mango-platform/mango-job/mango-job-core`
  - `mango-platform/mango-job/mango-job-starter-remote`
  - `mango-platform/mango-job/mango-job-starter`
  - required upstream modules selected by Maven `-am`
- The static validation ended with `Issues: 1105`; this is existing static-analysis debt, not introduced by this change.
- Earlier full-root static scan observed `Issues: 20620`, confirming the previous behavior was substantially broader.

## UI Review

No product UI changed. Screenshot evidence is a rendered verification report for traceability.

## Unverified Items And Risks

- Full `mvn mango:check -Drule=all` still fails because the repository has existing static-analysis debt outside this task scope.
- This task does not remediate PMD/Checkstyle findings in Job or upstream modules.

## PMO Exceptions

None.
