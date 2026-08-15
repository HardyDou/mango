# Mango Changesets

Every pull request that changes a published npm package adds a Changeset with the package name, semver bump and consumer-facing summary. `pnpm release:change-check` compares the pull request diff with these declarations and rejects missing or unrelated package entries.

Release operators use `mango release plan` to consume all pending Changesets, calculate internal dependents and synchronize the CLI release matrix. Formal publication remains a local, explicitly authorized action.

Before planning a batch, copy [release-notes-template.md](./release-notes-template.md) to `release-notes.txt` and replace every instruction with facts from the complete successful-release baseline-to-candidate PR range. The release notes must map every actual PR to `Fixed`, `Added` or `Changed`, exact published packages and business adaptation; superseded or audit-only PRs stay in the separate Audit History section.

The same checked document records versions, publication topology, business impact, executable upgrade steps, verification and rollback. `Upgrade Estimate` is required to state the affected audience, engineering effort, execution window, service downtime, rollback effort and assumptions. `mango release prepare` rejects missing or empty sections, missing PR/package/adaptation mappings, unresolved instruction placeholders and incomplete estimates.

The one-time `legacy-reconciliation.json` record covers only the gap between the last successful pre-Changesets Release and the first release prepared by the new workflow. It is not a reusable fallback.
