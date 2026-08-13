function registryResult(result, version) {
  if (result.status === 0 && result.stdout.trim() === version) {
    return 'exists';
  }
  const output = `${result.stdout || ''}\n${result.stderr || ''}`;
  if (result.status !== 0 && /(?:E404|\b404\b|not found|no match found)/iu.test(output)) {
    return 'absent';
  }
  return 'unknown';
}

export function classifyNpmBatchRecovery(publishResult, consumeResult, version) {
  const publish = registryResult(publishResult, version);
  const consume = registryResult(consumeResult, version);
  if (publish === 'exists' && consume === 'exists') {
    return 'verify-existing';
  }
  if (publish === 'absent' && consume === 'absent') {
    return 'publish-absent';
  }
  return 'stop';
}
