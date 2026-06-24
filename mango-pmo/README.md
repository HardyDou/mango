# Mango PMO Baseline

`mango-pmo` is the source of truth for Mango process rules, agent roles, templates, and PMO tools.

Business projects should not copy or edit this directory by hand. They consume a versioned snapshot through `@mango/pmo` and `@mango/cli`:

```bash
mango pmo status --project-dir .
mango pmo check --project-dir .
mango pmo sync --project-dir .
mango pmo upgrade --project-dir .
```

The executable business snapshot is installed under `business-pmo/mango-baseline` and includes:

| Path | Purpose |
|------|---------|
| `rules/index.json` | preflight routing index |
| `rules/**` | PMO, backend, frontend, testing, product, and documentation rules |
| `agents/**` | PM, tech lead, dev, QA, and PMO role files |
| `tools/pmo-preflight.mjs` | prints required rules for a task |
| `tools/delivery-contract-check.mjs` | checks design and delivery ledger files |
| `tools/acceptance-evidence-check.mjs` | checks acceptance evidence files |
| `templates/**` | delivery contract and evidence templates |
| `baseline.json` | package version and SHA-256 manifest |

Business requirements, designs, ledgers, and evidence belong outside the baseline, usually under `business-docs/**`.
