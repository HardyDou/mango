# Mango Component Release Notes Template

Copy this file to `.changeset/release-notes.txt` when a new Mango component batch is planned. Replace every instruction comment with release-specific facts before `mango release prepare`; comments and empty sections are rejected by the release-notes checker.

## Pull Requests

<!-- One bullet per actual release-bearing PR. Required format: - PR #123 Fixed|Added|Changed summary. Packages: exact coordinates. Business Adaptation: consumer action. Keep superseded or audit-only PRs in Audit History instead. -->

## Fixed

<!-- Describe consumer-visible defects fixed by this batch. Remove this heading only when Added or Changed is populated. -->

## Added

<!-- Describe consumer-visible capabilities added by this batch. Remove this heading only when Fixed or Changed is populated. -->

## Changed

<!-- Describe behavior, governance, compatibility or operational changes. Remove this heading only when Fixed or Added is populated. -->

## Versions

<!-- List previous and target versions for every released coordinate plus explicitly unchanged compatibility coordinates. -->

## Published Packages

<!-- List exact coordinates in machine-plan topology order and identify objects that are deliberately not published. -->

## Business Impact

<!-- State affected/unaffected consumers and any API, data, database, menu, permission, tenant, configuration, runtime or operational impact. -->

## Upgrade Estimate

- Audience:
- Engineering Effort:
- Execution Window:
- Service Downtime:
- Rollback Effort:
- Assumptions:

## Upgrade Notes

<!-- Give executable dependency, PMO and consumer adaptation steps. Distinguish aggregate and direct/module consumers. -->

## Verification

<!-- Give exact consumer-visible assertions and commands for the release candidate and post-publication consumer. -->

## Rollback

<!-- Give reversible consumer rollback steps, data/Maven implications and the immutable-coordinate rule. -->

## Audit History

<!-- Optional: list superseded, recovery or historical PRs that are not release-bearing changes in this batch. -->
