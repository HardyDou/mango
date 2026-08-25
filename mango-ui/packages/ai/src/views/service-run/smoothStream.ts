export interface StreamFrameScheduler {
  request(callback: FrameRequestCallback): number;
  cancel(handle: number): void;
}

export interface SmoothTextStream {
  push(delta: string): void;
  complete(): Promise<void>;
  cancel(): void;
}

interface SmoothTextStreamOptions {
  reducedMotion?: boolean;
  scheduler?: StreamFrameScheduler;
}

const browserFrameScheduler: StreamFrameScheduler = {
  request: (callback) => window.requestAnimationFrame(callback),
  cancel: (handle) => window.cancelAnimationFrame(handle),
};

export function createSmoothTextStream(
  onText: (text: string) => void,
  options: SmoothTextStreamOptions = {},
): SmoothTextStream {
  const scheduler = options.scheduler ?? browserFrameScheduler;
  let displayed = '';
  let pending = '';
  let frameHandle: number | undefined;
  let finishing = false;
  let cancelled = false;
  let completionResolvers: Array<() => void> = [];

  function resolveCompletion() {
    const resolvers = completionResolvers;
    completionResolvers = [];
    resolvers.forEach((resolve) => resolve());
  }

  function paint() {
    frameHandle = undefined;
    if (cancelled) return;
    const symbols = Array.from(pending);
    const count = streamSliceSize(symbols.length, finishing);
    displayed += symbols.slice(0, count).join('');
    pending = symbols.slice(count).join('');
    onText(displayed);
    if (pending) schedule();
    else resolveCompletion();
  }

  function schedule() {
    if (frameHandle === undefined && pending && !cancelled) frameHandle = scheduler.request(paint);
  }

  return {
    push(delta) {
      if (!delta || cancelled) return;
      pending += delta;
      if (options.reducedMotion) {
        displayed += pending;
        pending = '';
        onText(displayed);
        resolveCompletion();
        return;
      }
      schedule();
    },
    complete() {
      finishing = true;
      if (!pending || cancelled) return Promise.resolve();
      schedule();
      return new Promise<void>((resolve) => completionResolvers.push(resolve));
    },
    cancel() {
      cancelled = true;
      pending = '';
      if (frameHandle !== undefined) scheduler.cancel(frameHandle);
      frameHandle = undefined;
      resolveCompletion();
    },
  };
}

export function streamSliceSize(pendingLength: number, finishing: boolean): number {
  if (pendingLength <= 0) return 0;
  if (pendingLength > 600) return 4;
  if (pendingLength > 60) return 3;
  if (pendingLength > 12 || finishing) return 2;
  return 1;
}

export function finalTextRemainder(receivedText: string, finalText: string): string {
  if (!receivedText) return finalText;
  return finalText.startsWith(receivedText) ? finalText.slice(receivedText.length) : '';
}
