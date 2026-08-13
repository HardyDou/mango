import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'index.vue'), 'utf8');

describe('notice inbound detail', () => {
  it('uses the shared sanitized rich text viewer for message bodies', () => {
    expect(source).toContain('RichTextViewer,');
    expect(source).toContain('<RichTextViewer :content="bodyContent(current)"');
    expect(source).not.toContain('<pre class="notice-inbound-page__body">');
    expect(source).toContain('escapeHtml(message.bodyText)');
  });

  it('previews attachments through FilePreviewPanel using the file id', () => {
    expect(source).toContain("import { FilePreviewPanel } from '@mango/file';");
    expect(source).toContain('@click="openAttachmentPreview(row)"');
    expect(source).toContain('<FilePreviewPanel v-if="previewAttachment?.fileId" :file-id="previewAttachment.fileId"');
    expect(source).toContain('if (!attachment.fileId) return;');
  });

  it('places processing status after the attachment section', () => {
    expect(source.indexOf('<MangoPageSection title="附件">')).toBeLessThan(
      source.indexOf('<MangoPageSection title="处理状态">'),
    );
  });

  it('renders provider codes through a user-facing label', () => {
    expect(source).toContain('providerLabel(row.channelType, row.providerCode)');
    expect(source).toContain('providerLabel(current.channelType, current.providerCode)');
    expect(source).toContain("STANDARD_MAIL: '标准邮件'");
    expect(source).not.toContain('prop="providerCode" label="提供方"');
  });
});
