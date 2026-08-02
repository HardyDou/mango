import { beforeEach, describe, expect, it, vi } from 'vitest';

const noticeApiMock = vi.hoisted(() => ({
  getNoticeDomains: vi.fn(),
}));

vi.mock('../api/notice', () => noticeApiMock);

import { useNoticeDomains } from '../components/useNoticeDomains';

describe('useNoticeDomains', () => {
  beforeEach(() => {
    noticeApiMock.getNoticeDomains.mockReset();
  });

  it('分别提供纯名称和名称加代码两种业务域文案', async () => {
    noticeApiMock.getNoticeDomains.mockResolvedValue([
      { id: 'job', domainCode: 'JOB', domainName: '任务调度', children: [] },
    ]);
    const domains = useNoticeDomains();

    await domains.loadDomains();

    expect(domains.domainName('JOB')).toBe('任务调度');
    expect(domains.domainText('JOB')).toBe('任务调度（JOB）');
  });
});
