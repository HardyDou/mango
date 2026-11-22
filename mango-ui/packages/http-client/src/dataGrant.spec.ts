import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  DATA_GRANT_ABSENT,
  DATA_GRANT_DOMAIN_HEADER,
  DATA_GRANT_HEADER,
  DATA_GRANT_REQUIRED_HEADER,
  DATA_GRANT_SKIPPED_HEADER,
  MAX_DATA_GRANT_COUNT,
  MAX_DATA_GRANT_LENGTH,
  applyDataGrantResponse,
  clearDataGrants,
  collectDataGrants,
  deleteDataGrant,
  nextDataGrantSequence,
  recordDataGrant,
} from './dataGrant';

const identity = { userId: '10023', tenantId: '1' } as const;

let warn: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  clearDataGrants();
  warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
});

afterEach(() => {
  warn.mockRestore();
  clearDataGrants();
});

describe('data grant store', () => {
  it('records grants per domain and returns them as a comma separated header', () => {
    recordDataGrant('fixture:item', 'grant-a', identity);
    recordDataGrant('fixture:order', 'grant-b', identity);

    expect(collectDataGrants(identity)).toEqual({
      value: 'grant-b, grant-a',
      domains: ['fixture:order', 'fixture:item'],
    });
  });

  it('keeps multiple domains side by side and deletes them one by one', () => {
    recordDataGrant('fixture:item', 'grant-a', identity);
    recordDataGrant('fixture:order', 'grant-b', identity);

    deleteDataGrant('fixture:item');

    expect(collectDataGrants(identity)).toEqual({ value: 'grant-b', domains: ['fixture:order'] });
  });

  it('ignores a stale sequence so out-of-order responses cannot roll the grant back', () => {
    recordDataGrant('fixture:item', 'new-grant', identity, 20);
    expect(recordDataGrant('fixture:item', 'old-grant', identity, 10)).toBe(false);

    expect(collectDataGrants(identity)).toEqual({ value: 'new-grant', domains: ['fixture:item'] });
  });

  it('drops grants whose identity no longer matches and clears the key', () => {
    recordDataGrant('fixture:item', 'grant-a', identity);

    expect(collectDataGrants({ userId: '10023', tenantId: '2' })).toBeUndefined();
    expect(collectDataGrants({ userId: '10024', tenantId: '1' })).toBeUndefined();
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('domain=fixture:item'));
  });

  it('returns undefined when nothing was recorded', () => {
    expect(collectDataGrants(identity)).toBeUndefined();
  });

  it('keeps sequences monotonic across clear so in-flight responses stay older', () => {
    const first = nextDataGrantSequence();
    expect(nextDataGrantSequence()).toBeGreaterThan(first);
    clearDataGrants();
    expect(nextDataGrantSequence()).toBeGreaterThan(first + 1);
  });

  it(`keeps only the ${MAX_DATA_GRANT_COUNT} most recently used grants`, () => {
    for (let index = 0; index < MAX_DATA_GRANT_COUNT + 1; index += 1) {
      recordDataGrant(`domain-${index}`, `grant-${index}`, identity);
    }

    const collected = collectDataGrants(identity);

    expect(collected?.domains).toHaveLength(MAX_DATA_GRANT_COUNT);
    expect(collected?.domains[0]).toBe(`domain-${MAX_DATA_GRANT_COUNT}`);
    expect(collected?.domains).not.toContain('domain-0');
    expect(warn).toHaveBeenCalledWith(expect.stringContaining(`超过 ${MAX_DATA_GRANT_COUNT} 个数据域`));
  });

  it('truncates by recent use instead of sending nothing when the header would be too long', () => {
    const big = 'x'.repeat(MAX_DATA_GRANT_LENGTH - 10);
    recordDataGrant('domain-old', big, identity);
    recordDataGrant('domain-new', `${big}new`, identity);

    const collected = collectDataGrants(identity);

    expect(collected?.value).toBe(`${big}new`);
    expect(collected?.domains).toEqual(['domain-new']);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining(`超过 ${MAX_DATA_GRANT_LENGTH} 字符`));
  });

  it('keeps truncated history recent-use ordered so the next request sends the survivors', () => {
    recordDataGrant('domain-a', 'grant-a', identity);
    recordDataGrant('domain-b', 'grant-b', identity);
    expect(collectDataGrants(identity)?.value).toBe('grant-b, grant-a');

    recordDataGrant('domain-a', 'grant-a2', identity);
    expect(collectDataGrants(identity)?.value).toBe('grant-a2, grant-b');
  });
});

describe('applyDataGrantResponse', () => {
  it('stores the grant when the response carries domain and value', () => {
    applyDataGrantResponse({
      headers: {
        [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
        [DATA_GRANT_HEADER.toLowerCase()]: 'grant-a',
      },
      identity,
      seq: 1,
      sentDomains: [],
    });

    expect(collectDataGrants(identity)).toEqual({ value: 'grant-a', domains: ['fixture:item'] });
    expect(warn).not.toHaveBeenCalled();
  });

  it('ignores grant headers without a domain header only once', () => {
    applyDataGrantResponse({
      headers: { [DATA_GRANT_HEADER.toLowerCase()]: 'grant-a' },
      identity,
      seq: 1,
      sentDomains: [],
    });
    applyDataGrantResponse({
      headers: { [DATA_GRANT_HEADER.toLowerCase()]: 'grant-b' },
      identity,
      seq: 2,
      sentDomains: [],
    });

    expect(collectDataGrants(identity)).toBeUndefined();
    expect(warn).toHaveBeenCalledTimes(1);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('缺少 X-Mango-Data-Grant-Domain'));
  });

  it.each([DATA_GRANT_ABSENT, 'oversize', 'scope-unresolved', 'something-new'])(
    'deletes the cached key for skipped value %s',
    (skipped) => {
      recordDataGrant('fixture:item', 'grant-a', identity);

      applyDataGrantResponse({
        headers: {
          [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
          [DATA_GRANT_SKIPPED_HEADER.toLowerCase()]: skipped,
        },
        identity,
        seq: 2,
        sentDomains: ['fixture:item'],
      });

      expect(collectDataGrants(identity)).toBeUndefined();
    },
  );

  it('deletes the cached key when the response reports absent', () => {
    recordDataGrant('fixture:item', 'grant-a', identity);

    applyDataGrantResponse({
      headers: {
        [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
        [DATA_GRANT_HEADER.toLowerCase()]: DATA_GRANT_ABSENT,
      },
      identity,
      seq: 2,
      sentDomains: ['fixture:item'],
    });

    expect(collectDataGrants(identity)).toBeUndefined();
  });

  it('warns in dev when a cached grant for the domain was not actually sent', () => {
    recordDataGrant('fixture:item', 'grant-cached', identity, 1);

    applyDataGrantResponse({
      headers: {
        [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
        [DATA_GRANT_REQUIRED_HEADER.toLowerCase()]: '1',
      },
      identity,
      seq: 2,
      sentDomains: [],
    });

    expect(warn).toHaveBeenCalledWith(expect.stringContaining('domain=fixture:item'));
  });

  it('stays quiet on the first load, where nothing is cached yet', () => {
    applyDataGrantResponse({
      headers: {
        [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
        [DATA_GRANT_REQUIRED_HEADER.toLowerCase()]: '1',
      },
      identity,
      seq: 1,
      sentDomains: [],
    });

    expect(warn).not.toHaveBeenCalled();
  });

  it('stays quiet when the response also issued a grant for that domain', () => {
    applyDataGrantResponse({
      headers: {
        [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
        [DATA_GRANT_REQUIRED_HEADER.toLowerCase()]: '1',
        [DATA_GRANT_HEADER.toLowerCase()]: 'grant-a',
      },
      identity,
      seq: 1,
      sentDomains: [],
    });

    expect(warn).not.toHaveBeenCalled();
  });

  it('stays quiet when the request did not participate in the relay', () => {
    applyDataGrantResponse({
      headers: {
        [DATA_GRANT_DOMAIN_HEADER.toLowerCase()]: 'fixture:item',
        [DATA_GRANT_REQUIRED_HEADER.toLowerCase()]: '1',
      },
      identity,
      seq: 1,
    });

    expect(warn).not.toHaveBeenCalled();
  });

  it('accepts array valued headers', () => {
    applyDataGrantResponse({
      headers: {
        'x-mango-data-grant-domain': ['fixture:item'],
        'x-mango-data-grant': ['grant-a'],
      },
      identity,
      seq: 1,
      sentDomains: [],
    });

    expect(collectDataGrants(identity)).toEqual({ value: 'grant-a', domains: ['fixture:item'] });
  });
});
