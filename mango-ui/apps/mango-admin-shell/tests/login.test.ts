import { describe, expect, it } from 'vitest';
import { assertLoginOutcome, resolveLoginInstitution } from '../e2e/support/login';

const platform = { tenantId: '1', tenantCode: 'default', tenantName: '芒果集团' };
const business = { tenantId: '9007199254740993', tenantCode: 'business', tenantName: '业务机构' };
const success = (data: unknown) => ({ code: 200, success: true, data });

describe('Shell browser login contract', () => {
  it('uses automatic selection for the only accessible institution', () => {
    expect(resolveLoginInstitution(success([platform]), 'default')).toEqual({ institution: platform, multiple: false });
  });

  it('selects by institution code rather than the first option or display name', () => {
    expect(resolveLoginInstitution(success([platform, business]), 'business')).toEqual({
      institution: business,
      multiple: true,
    });
  });

  it.each([
    ['HTTP-200 business failure', { code: 1400, success: false, data: [platform] }],
    ['contradictory success flag', { code: 200, success: false, data: [platform] }],
    ['contradictory status code', { code: 500, success: true, data: [platform] }],
    ['absent response', null],
    ['missing list', success(null)],
    ['empty list', success([])],
    ['missing target', success([business])],
    ['duplicate code', success([platform, { ...business, tenantCode: 'default' }])],
    ['duplicate ID', success([platform, { ...business, tenantId: '1' }])],
    ['missing ID', success([{ tenantCode: 'default', tenantName: '芒果集团' }])],
    ['numeric ID', success([{ ...platform, tenantId: 1 }])],
    ['empty name', success([{ ...platform, tenantName: ' ' }])],
  ])('rejects %s instead of silently skipping institution selection', (_name, body) => {
    expect(() => resolveLoginInstitution(body, 'default')).toThrow();
  });

  it('accepts an authenticated session for the exact selected institution', () => {
    expect(() =>
      assertLoginOutcome(success({ ...business, accessToken: 'unit-test-session' }), business),
    ).not.toThrow();
  });

  it.each([
    ['business error', { code: 1400, success: false, data: {} }],
    ['empty response', success(null)],
    ['missing token', success(platform)],
    ['wrong institution ID', success({ ...platform, tenantId: '2', accessToken: 'unit-test-session' })],
    ['wrong institution code', success({ ...platform, tenantCode: 'other', accessToken: 'unit-test-session' })],
    [
      'required password change',
      success({ ...platform, accessToken: 'unit-test-session', passwordResetRequired: true }),
    ],
  ])('rejects %s before claiming browser login passed', (_name, body) => {
    expect(() => assertLoginOutcome(body, platform)).toThrow();
  });

  it('does not put returned credentials into failure messages', () => {
    expect(() =>
      assertLoginOutcome({ code: 401, success: false, data: { accessToken: 'unit-test-secret' } }, platform),
    ).toThrow('登录未成功');
  });
});
