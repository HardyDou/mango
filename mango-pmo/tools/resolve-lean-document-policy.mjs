#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const toolDir = path.dirname(fileURLToPath(import.meta.url));
const contract = JSON.parse(fs.readFileSync(path.resolve(toolDir, '../contracts/lean-documents.json'), 'utf8'));
const levels = Object.keys(contract.levels);
const levelIndex = new Map(levels.map((level, index) => [level, index]));
const forcedFacts = new Set(contract.forcedL5Facts);
const questionTopics = new Set(contract.questionTopics);

function uniqueInContractOrder(values, catalog) {
  const selected = new Set(values);
  return catalog.filter(value => selected.has(value));
}

function invalidLevel(value) {
  return value !== undefined && value !== null && !levelIndex.has(value);
}

export function resolveLeanDocumentPolicy(facts = {}) {
  const findings = [];
  if (invalidLevel(facts.requirementImpact)) findings.push(`非法需求影响等级：${facts.requirementImpact}`);
  if (invalidLevel(facts.solutionRisk)) findings.push(`非法方案风险等级：${facts.solutionRisk}`);
  if (invalidLevel(facts.requestedLevel)) findings.push(`非法请求等级：${facts.requestedLevel}`);
  if (facts.materialUnknowns !== undefined && !Array.isArray(facts.materialUnknowns)) {
    findings.push('materialUnknowns 必须是数组');
  }
  const unknownTopics = Array.isArray(facts.materialUnknowns) ? facts.materialUnknowns : [];
  for (const topic of unknownTopics) {
    if (!questionTopics.has(topic)) findings.push(`未知询问主题：${topic}`);
  }
  if (facts.triggerFacts !== undefined && !Array.isArray(facts.triggerFacts)) {
    findings.push('triggerFacts 必须是数组');
  }
  const declaredFacts = Array.isArray(facts.triggerFacts) ? facts.triggerFacts : [];
  for (const fact of declaredFacts) {
    if (!forcedFacts.has(fact)) findings.push(`未知强制 L5 事实：${fact}`);
  }
  if (findings.length > 0) return { action: 'STOP', findings };

  const matchedForcedFacts = uniqueInContractOrder(declaredFacts, contract.forcedL5Facts);
  const hasForcedL5 = matchedForcedFacts.length > 0;
  const missingRiskFacts = !hasForcedL5 && (!facts.requirementImpact || !facts.solutionRisk);
  const questions = [...unknownTopics];
  if (missingRiskFacts) questions.push('BUSINESS_GOAL', 'SYSTEM_BOUNDARY');

  let finalLevel = hasForcedL5
    ? 'L5'
    : (missingRiskFacts ? null : levels[Math.max(levelIndex.get(facts.requirementImpact), levelIndex.get(facts.solutionRisk))]);

  if (facts.requestedLevel && finalLevel && levelIndex.get(facts.requestedLevel) < levelIndex.get(finalLevel)) {
    if (hasForcedL5) {
      return {
        action: 'STOP',
        findings: [`${matchedForcedFacts.join(',')} 固定为 L5，不允许降级`],
        finalLevel,
        matchedForcedFacts,
      };
    }
    if (!facts.downwardOverrideConfirmed) questions.push('LEVEL_OVERRIDE');
    else finalLevel = facts.requestedLevel;
  } else if (facts.requestedLevel && (!finalLevel || levelIndex.get(facts.requestedLevel) > levelIndex.get(finalLevel))) {
    finalLevel = facts.requestedLevel;
  }

  const groupedQuestions = uniqueInContractOrder(questions, contract.questionTopics);
  if (groupedQuestions.length > 0) {
    return {
      action: 'ASK',
      finalLevel,
      questions: groupedQuestions,
      matchedForcedFacts,
      findings: [],
    };
  }

  const levelPolicy = contract.levels[finalLevel];
  const templates = levelPolicy.template ? [levelPolicy.template] : (levelPolicy.templates ?? []);
  return {
    action: levelPolicy.artifactPolicy === 'NONE' ? 'DIRECT' : 'WRITE',
    finalLevel,
    documentVersion: levelPolicy.documentVersion,
    artifactPolicy: levelPolicy.artifactPolicy,
    templates,
    documentCount: templates.length,
    maxA4Pages: levelPolicy.maxA4Pages ?? null,
    matchedForcedFacts,
    findings: [],
  };
}

function parseArgs(argv) {
  const index = argv.indexOf('--facts');
  return index >= 0 ? argv[index + 1] : '';
}

function main(argv) {
  const factsPath = parseArgs(argv);
  if (!factsPath) throw new Error('usage: resolve-lean-document-policy.mjs --facts <json-file>');
  const facts = JSON.parse(fs.readFileSync(path.resolve(factsPath), 'utf8'));
  const result = resolveLeanDocumentPolicy(facts);
  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  process.exitCode = result.action === 'STOP' ? 1 : 0;
}

if (process.argv[1]?.endsWith('resolve-lean-document-policy.mjs')) main(process.argv.slice(2));
