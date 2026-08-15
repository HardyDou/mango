#!/usr/bin/env node
process.argv.push('--check');
await import('./create-release-plan.mjs');
