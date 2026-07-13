import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const PMO_ROOT = fileURLToPath(new URL('../../', import.meta.url));

export function repositoryPath(relativePath) {
  const normalized = relativePath.startsWith('mango-pmo/')
    ? relativePath.slice('mango-pmo/'.length)
    : relativePath;
  return path.resolve(PMO_ROOT, normalized);
}

export function loadContract(relativePath) {
  const contractPath = repositoryPath(relativePath);
  const content = fs.readFileSync(contractPath, 'utf8');
  const contract = JSON.parse(content);
  contract.__path = contractPath;
  return contract;
}

function collectRuleIds(value, results = new Set()) {
  if (Array.isArray(value)) {
    value.forEach((entry) => collectRuleIds(entry, results));
    return results;
  }
  if (!value || typeof value !== 'object') return results;
  for (const [key, child] of Object.entries(value)) {
    if (key === 'ruleId' && typeof child === 'string') results.add(child);
    collectRuleIds(child, results);
  }
  return results;
}

export function validateContractRuleLinks(contract) {
  const findings = [];
  const sourcePath = repositoryPath(contract.ruleSource);
  if (!fs.existsSync(sourcePath)) {
    return [{ severity: 'FAIL', ruleId: 'CONTRACT-ASSET-001', message: `规范源不存在：${contract.ruleSource}` }];
  }
  const source = fs.readFileSync(sourcePath, 'utf8');
  for (const ruleId of collectRuleIds(contract)) {
    if (!source.includes(ruleId)) {
      findings.push({ severity: 'FAIL', ruleId: 'CONTRACT-ASSET-001', message: `${ruleId} 未在唯一规范源 ${contract.ruleSource} 中声明` });
    }
  }
  if (contract.template) {
    const templatePath = repositoryPath(contract.template);
    if (!fs.existsSync(templatePath)) {
      findings.push({ severity: 'FAIL', ruleId: 'CONTRACT-ASSET-001', message: `模板不存在：${contract.template}` });
    }
  }
  return findings;
}
