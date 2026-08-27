import { describe, expect, it } from 'vitest';
import { toBackendDateRangeParams } from '../date-range';

describe('toBackendDateRangeParams', () => {
  it('expands default date-only bounds to the complete day', () => {
    expect(toBackendDateRangeParams({ startTime: '2026-08-27', endTime: '2026-08-27' })).toEqual({
      startTime: '2026-08-27 00:00:00',
      endTime: '2026-08-27 23:59:59',
    });
  });

  it('supports custom bound names without changing complete date-time values', () => {
    expect(toBackendDateRangeParams({
      triggerTimeStart: '2026-08-27',
      triggerTimeEnd: '2026-08-27 18:00:00',
      page: 1,
    }, { startKey: 'triggerTimeStart', endKey: 'triggerTimeEnd' })).toEqual({
      triggerTimeStart: '2026-08-27 00:00:00',
      triggerTimeEnd: '2026-08-27 18:00:00',
      page: 1,
    });
  });
});
