## Summary

- Standardize Mango capability README docs across backend platform, infra, frontend packages, selected frontend entries, starter and topology entries.
- Add executable governance gates for capability docs, README audits, PR body completion and PMO intent routing.
- Record 9-role expert review evidence and close the discovered README coverage gaps.

## PMO / Scope

- PMO preflight: executed
- Role / phase: pmo / governance
- Task paths: AGENTS.md, mango-pmo, mango-docs, mango, mango-ui, mango-business-starter, .github
- Loaded PMO files: rules/00-dev-flow.md, rules/03-ai-coding-redlines.md, agents/05-pmo-agent.md, rules/05-ai-delivery-quality.md, rules/06-document-assets.md, rules/08-capability-docs.md, rules/07-mango-issue-runbook.md, rules/04-test-assets.md, rules/02-dev-environment.md, agents/01-pm-agent.md, rules/product/01-prd-template.md, rules/product/02-sprint.md
- PR type: governance

## Capability Docs

- Affected Mango capabilities: all managed Mango module README docs, selected frontend entry README docs, capability map, PMO capability-doc gates, PR template, business starter templates
- Module README: updated, 67 managed README files now follow the relevant capability or frontend-entry template and missing module/entry README files are covered
- Capability map: updated, full platform / infra / frontend / starter index plus combined business integration loops
- PMO rules: updated, capability docs governance and preflight intent routing clarified
- `mango-pmo/rules/index.json`: updated, capability docs rule is indexed
- Not applicable reason: runtime behavior, public API, configuration, menu, permission, tenant, page, startup and validation behavior were not changed; this PR changes governance docs, README usage docs, templates and validation gates

## Validation

```bash
node mango-pmo/tools/audit-module-readmes.mjs --self-test
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/check-capability-docs.mjs --self-test
node mango-pmo/tools/check-capability-docs.mjs --base main --head HEAD
node mango-pmo/tools/check-governance-intent.mjs
node mango-pmo/tools/check-pmo-preflight.mjs
node mango-business-starter/scripts/check-template.mjs
pnpm -F @mango/cli test
mvn -f mango/pom.xml -pl mango-platform/mango-auth -am test
mvn -f mango/pom.xml -pl mango-platform/mango-access -am test
mvn -f mango/pom.xml -pl mango-platform/mango-authorization -am test
mvn -f mango/pom.xml -pl mango-infra/mango-infra-test -am test
node -e "JSON.parse(require('fs').readFileSync('mango-pmo/rules/index.json','utf8')); console.log('index ok')"
git diff --check
```

- Result: passed locally
- Unverified items: full Maven reactor tests, frontend package builds, GitHub Actions real PR runtime
- Risks: README content is based on static code inspection and expert review; frontend build commands were attempted for `@mango/file`, `@mango/workflow`, `@mango/job`, `@mango/auth`, `@mango/rbac`, `@mango/system` but local `node_modules` is missing and `vite` is unavailable, so runtime behavior still needs module-level verification when each capability changes

## PMO Exceptions

- None
