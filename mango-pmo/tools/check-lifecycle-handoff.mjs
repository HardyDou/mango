#!/usr/bin/env node
import { runLifecycleCli } from './document-contract/lifecycle.mjs';

process.exitCode = runLifecycleCli();
