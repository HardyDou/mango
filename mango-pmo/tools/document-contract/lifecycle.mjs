import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { loadContract, validateContractRuleLinks } from './contract-loader.mjs';
import { definedIds, documentIds, traceSectionIds, validateDocument } from './validator.mjs';

const LIFECYCLE_CONTRACT = loadContract('mango-pmo/contracts/document-lifecycle.json');
const STAGES = LIFECYCLE_CONTRACT.stages.map((stage) => ({
  key: stage.key,
  type: stage.documentType,
  contractPath: stage.contract
}));
const RULES = Object.fromEntries(Object.entries(LIFECYCLE_CONTRACT.rules).map(([key, value]) => [key, value.ruleId]));

function addFinding(findings, ruleId, message) {
  findings.push({ severity: 'FAIL', ruleId, message });
}

export function sha256(source) {
  return crypto.createHash('sha256').update(source, 'utf8').digest('hex');
}

export function parseLifecycleArgs(argv) {
  const args = { brd: '', srs: '', tdd: '', plan: '', riskLevel: '', throughStage: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (['--brd', '--srs', '--tdd', '--plan', '--risk', '--through'].includes(value)) {
      const key = value === '--risk' ? 'riskLevel' : value === '--through' ? 'throughStage' : value.slice(2);
      args[key] = argv[index + 1] ?? '';
      index += 1;
    }
  }
  return args;
}

export function loadLifecycleDocuments(paths) {
  const documents = {};
  for (const stage of STAGES) {
    const filePath = paths[stage.key];
    if (!filePath) continue;
    const resolved = path.resolve(filePath);
    if (!fs.existsSync(resolved)) {
      documents[stage.key] = { ...stage, resolved, missing: true };
      continue;
    }
    documents[stage.key] = { ...stage, resolved, source: fs.readFileSync(resolved, 'utf8') };
  }
  return documents;
}

export function validateLifecycle(documents, options = {}) {
  const findings = [...validateContractRuleLinks(LIFECYCLE_CONTRACT)];
  const results = {};
  const riskLevel = options.riskLevel ?? '';
  const throughStage = options.throughStage ?? '';
  if (riskLevel && !Object.hasOwn(LIFECYCLE_CONTRACT.requiredStagesByRisk, riskLevel)) {
    addFinding(findings, RULES.risk, `未知风险等级：${riskLevel}`);
  }
  const throughIndex = throughStage ? STAGES.findIndex((stage) => stage.key === throughStage) : -1;
  if (throughStage && throughIndex < 0) {
    addFinding(findings, RULES.order, `未知移交阶段：${throughStage}`);
  }
  const completeStageKeys = riskLevel
    ? (LIFECYCLE_CONTRACT.requiredStagesByRisk[riskLevel] ?? [])
    : STAGES.map((stage) => stage.key);
  const requiredStageKeys = new Set(
    throughIndex >= 0
      ? completeStageKeys.filter((key) => STAGES.findIndex((stage) => stage.key === key) <= throughIndex)
      : completeStageKeys
  );

  for (const stage of STAGES) {
    const document = documents[stage.key];
    if (!document || document.missing || typeof document.source !== 'string') {
      if (requiredStageKeys.has(stage.key)) addFinding(findings, RULES.order, `缺少生命周期阶段：${stage.type}`);
      continue;
    }
    const contract = loadContract(stage.contractPath);
    const result = validateDocument(document.source, contract, {
      ...options,
      documentPath: document.resolved
    });
    results[stage.key] = { ...document, contract, result };
    for (const finding of result.findings) {
      addFinding(findings, finding.ruleId, `${stage.type}: ${finding.message}`);
    }
    const meta = result.ast.frontmatter.values;
    if (meta.status !== 'APPROVED' || meta.action !== 'NEXT') {
      addFinding(findings, RULES.gate, `${stage.type} 必须经过外部门禁并处于 APPROVED/NEXT`);
    }
    if (riskLevel && meta.riskLevel !== riskLevel) {
      addFinding(findings, RULES.risk, `${stage.type} 风险等级 ${meta.riskLevel} 与入口 ${riskLevel} 不一致`);
    }
  }

  const available = STAGES.map((stage) => results[stage.key]).filter(Boolean);
  if ([...requiredStageKeys].some((key) => !results[key])) {
    const scope = throughIndex >= 0
      ? `截至 ${STAGES[throughIndex].type} 的连续上游链路`
      : '完整 BRD/SRS/TDD/Plan 链路';
    addFinding(findings, RULES.risk, `${riskLevel} 任务必须保留${scope}`);
  }
  if (available.length > 0) {
    const levels = new Set(available.map((entry) => entry.result.ast.frontmatter.values.riskLevel));
    if (levels.size > 1) addFinding(findings, RULES.risk, `四阶段风险等级不一致：${[...levels].join(', ')}`);
    const effectiveRisk = riskLevel || available[0].result.ast.frontmatter.values.riskLevel;
    if (!riskLevel && throughIndex < 0 && ['L2', 'L3'].includes(effectiveRisk) && available.length !== STAGES.length) {
      addFinding(findings, RULES.risk, `${effectiveRisk} 复杂任务必须保留完整 BRD/SRS/TDD/Plan 链路`);
    }
  }

  for (let index = 1; index < STAGES.length; index += 1) {
    const upstreamStage = STAGES[index - 1];
    const downstreamStage = STAGES[index];
    const upstream = results[upstreamStage.key];
    const downstream = results[downstreamStage.key];
    if (!upstream || !downstream) continue;
    const upstreamMeta = upstream.result.ast.frontmatter.values;
    const downstreamMeta = downstream.result.ast.frontmatter.values;
    if (downstreamMeta.upstreamDocumentId !== upstreamMeta.documentId) {
      addFinding(findings, RULES.order, `${downstreamStage.type} 的 upstreamDocumentId 未引用 ${upstreamMeta.documentId}`);
    }
    const actualHash = sha256(upstream.source);
    if (downstreamMeta.upstreamDocumentHash !== actualHash) {
      addFinding(findings, RULES.hash, `${downstreamStage.type} 的上游摘要已失效：期望 ${actualHash}，实际 ${downstreamMeta.upstreamDocumentHash}`);
    }

    const coveragePrefixes = upstream.contract.traceability.localCoveragePrefixes;
    const upstreamIds = definedIds(upstream.result, coveragePrefixes);
    const downstreamIds = traceSectionIds(downstream.result, downstream.contract, downstream.contract.upstreamPrefixes);
    const allDownstreamReferences = documentIds(downstream.result, downstream.contract.upstreamPrefixes);
    for (const id of upstreamIds) {
      if (!downstreamIds.has(id)) addFinding(findings, RULES.trace, `${downstreamStage.type} 未承接上游 ID：${id}`);
    }
    for (const id of allDownstreamReferences) {
      if (!upstreamIds.has(id)) addFinding(findings, RULES.trace, `${downstreamStage.type} 引用了上游不存在的 ID：${id}`);
    }
  }

  return { findings, results };
}

export function runLifecycleCli(argv = process.argv.slice(2)) {
  const args = parseLifecycleArgs(argv);
  const documents = loadLifecycleDocuments(args);
  const checked = validateLifecycle(documents, {
    riskLevel: args.riskLevel,
    throughStage: args.throughStage
  });
  process.stdout.write('\n=== 产品文档生命周期检查 ===\n');
  if (checked.findings.length === 0) {
    process.stdout.write('结果：PASS\n');
    return 0;
  }
  for (const finding of checked.findings) process.stdout.write(`[FAIL] ${finding.ruleId} ${finding.message}\n`);
  process.stdout.write(`结果：FAIL (${checked.findings.length})\n`);
  return 1;
}
