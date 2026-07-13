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
  }

  const available = STAGES.map((stage) => results[stage.key]).filter(Boolean);
  if ([...requiredStageKeys].some((key) => !results[key])) {
    const scope = throughIndex >= 0
      ? `截至 ${STAGES[throughIndex].type} 的连续上游链路`
      : '完整 BRD/SRS/TDD/Plan 链路';
    addFinding(findings, RULES.risk, `${riskLevel} 任务必须保留${scope}`);
  }
  if (available.length > 0) {
    for (let index = 1; index < available.length; index += 1) {
      const upstream = available[index - 1];
      const downstream = available[index];
      const upstreamRisk = upstream.result.ast.frontmatter.values.riskLevel;
      const downstreamRisk = downstream.result.ast.frontmatter.values.riskLevel;
      const upstreamIndex = Object.keys(LIFECYCLE_CONTRACT.requiredStagesByRisk).indexOf(upstreamRisk);
      const downstreamIndex = Object.keys(LIFECYCLE_CONTRACT.requiredStagesByRisk).indexOf(downstreamRisk);
      if (upstreamIndex >= 0 && downstreamIndex >= 0 && downstreamIndex < upstreamIndex) {
        addFinding(
          findings,
          RULES.risk,
          `${downstream.type} 风险等级 ${downstreamRisk} 低于上游 ${upstream.type} 的 ${upstreamRisk}；风险只能基于新增事实升级，禁止下游降级`,
        );
      }
    }

    const target = throughIndex >= 0
      ? results[STAGES[throughIndex].key]
      : available.at(-1);
    if (riskLevel && target) {
      const targetRisk = target.result.ast.frontmatter.values.riskLevel;
      if (targetRisk !== riskLevel) {
        addFinding(findings, RULES.risk, `${target.type} 风险等级 ${targetRisk} 与当前入口 ${riskLevel} 不一致`);
      }
    }

    if (results.tdd && results.plan) {
      const tddRisk = results.tdd.result.ast.frontmatter.values.riskLevel;
      const planRisk = results.plan.result.ast.frontmatter.values.riskLevel;
      if (tddRisk !== planRisk) {
        addFinding(findings, RULES.risk, `technical-design 与 implementation-plan 必须使用同一最终风险等级：${tddRisk} != ${planRisk}`);
      }
    }

    const effectiveRisk = riskLevel || available.at(-1).result.ast.frontmatter.values.riskLevel;
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
