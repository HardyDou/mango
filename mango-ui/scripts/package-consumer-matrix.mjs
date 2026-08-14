export function classifyRegistryVersionResult(result, expectedVersion) {
  if (result.status === 0) {
    const actualVersion = result.stdout.trim();
    if (actualVersion !== expectedVersion) {
      throw new Error(`registry resolved ${actualVersion || '<empty>'}; expected ${expectedVersion}`);
    }
    return 'published';
  }

  const output = `${result.stdout || ''}\n${result.stderr || ''}`;
  if (/\bE404\b|\b404\s+Not\s+Found\b|\b404\b.*\bnot\s+found\b/iu.test(output)) {
    return 'candidate';
  }

  throw new Error(`registry lookup failed with status ${result.status ?? 1}: ${output.trim() || '<no output>'}`);
}
