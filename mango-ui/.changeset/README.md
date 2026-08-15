# Mango Changesets

Every pull request that changes a published npm package adds a Changeset with the package name, semver bump and consumer-facing summary. `pnpm release:change-check` compares the pull request diff with these declarations and rejects missing or unrelated package entries.

Release operators use `mango release plan` to consume all pending Changesets, calculate internal dependents and synchronize the CLI release matrix. Formal publication remains a local, explicitly authorized action.

The one-time `legacy-reconciliation.json` record covers only the gap between the last successful pre-Changesets Release and the first release prepared by the new workflow. It is not a reusable fallback.
