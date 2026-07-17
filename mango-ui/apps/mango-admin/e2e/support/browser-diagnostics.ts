import type { Page } from '@playwright/test';

export function collectBrowserDiagnostics(page: Page): string[] {
  const diagnostics: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error') {
      diagnostics.push(`console: ${message.text()}`);
    }
  });
  page.on('pageerror', error => diagnostics.push(`pageerror: ${error.message}`));
  page.on('requestfailed', (request) => {
    diagnostics.push(
      `requestfailed: ${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`,
    );
  });
  page.on('response', (response) => {
    if (response.status() >= 500) {
      diagnostics.push(`response: ${response.status()} ${response.request().method()} ${response.url()}`);
    }
  });
  return diagnostics;
}
