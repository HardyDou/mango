const BUILD_ARGUMENT = 'build';
const RECURSIVE_ARGUMENT = '-r';

/**
 * Phase-one PR policy: report every quality failure, but block only when an
 * affected workspace build cannot complete. A build failure means the
 * affected frontend cannot produce runnable output; all other checks remain
 * advisory while the existing debt and baseline are remediated.
 */
export function isBlockingPrCommand(command, arguments_) {
  return command === 'pnpm' && arguments_.includes(RECURSIVE_ARGUMENT) && arguments_.includes(BUILD_ARGUMENT);
}

export function advisoryRecord(command, arguments_, status) {
  return {
    command: [command, ...arguments_].join(' '),
    status: status ?? 'unknown',
  };
}
