export function selectConsumerCliMode({ candidateDirectory, candidatePackageNames, releaseCandidateMatrix }) {
  if (candidateDirectory && candidatePackageNames.has('@mango/cli')) {
    return 'candidate';
  }
  return releaseCandidateMatrix ? 'published' : 'source';
}
