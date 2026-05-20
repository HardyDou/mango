import { describe, expect, it } from 'vitest';
import {
  isWorkflowCommentRequired,
  normalizeWorkflowNodeActions,
  resolveVisibleWorkflowActions,
} from '../taskActions';

describe('workflow task actions', () => {
  it('normalizes compatible default actions', () => {
    const actions = normalizeWorkflowNodeActions();

    expect(actions.complete).toMatchObject({ enabled: true, label: '通过', requireComment: false });
    expect(actions.reject).toMatchObject({ enabled: true, label: '驳回', requireComment: true, danger: true });
    expect(actions.save).toMatchObject({ enabled: false, label: '暂存', disabled: true });
    expect(actions.transfer.tooltip).toContain('转办接口');
  });

  it('uses backend labels and business overrides without adding unauthorized actions', () => {
    const actions = resolveVisibleWorkflowActions({
      complete: { enabled: true, label: '同意', order: 20 },
      reject: { enabled: false, label: '退回修改', order: 10 },
      transfer: { enabled: true, label: '转办给他人', disabled: true, tooltip: '暂未支持', order: 5 },
    }, {
      complete: { label: '确认同意' },
      reject: { visible: true, disabled: true, tooltip: '业务状态不允许驳回' },
      addSign: { visible: false },
    });

    expect(actions.map(action => action.key)).toEqual(['transfer', 'reject', 'complete']);
    expect(actions.find(action => action.key === 'complete')).toMatchObject({
      label: '确认同意',
      buttonType: 'primary',
      disabled: false,
    });
    expect(actions.find(action => action.key === 'reject')).toMatchObject({
      label: '退回修改',
      disabled: true,
      tooltip: '业务状态不允许驳回',
      buttonType: 'danger',
    });
    expect(actions.find(action => action.key === 'transfer')).toMatchObject({
      disabled: true,
      tooltip: '暂未支持',
    });
  });

  it('requires a non-empty comment only when the action config says so', () => {
    expect(isWorkflowCommentRequired({ requireComment: true }, '')).toBe(true);
    expect(isWorkflowCommentRequired({ requireComment: true }, '   ')).toBe(true);
    expect(isWorkflowCommentRequired({ requireComment: true }, '同意')).toBe(false);
    expect(isWorkflowCommentRequired({ requireComment: false }, '')).toBe(false);
  });
});
