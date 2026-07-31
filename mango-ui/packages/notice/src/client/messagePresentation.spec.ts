import { describe, expect, it } from 'vitest';
import { noticeDesktopSummary, presentNoticeMessage, resolveNoticeActionLabel } from './messagePresentation';
import type { NoticeSiteMessage, NoticeSiteMessageAction } from '../types/notice';

const routeAction: NoticeSiteMessageAction = {
  id: 'action-1',
  actionCode: 'OPEN_WORKFLOW',
  actionLabel: '处理任务',
  interactionType: 'ROUTE',
  status: 'AVAILABLE',
  target: { targetType: 'ROUTE', targetKey: 'workflow:task:detail' },
};

function workflowMessage(overrides: Partial<NoticeSiteMessage> = {}): NoticeSiteMessage {
  return {
    id: 'message-1',
    title: '审批待办：费用报销',
    content: '你有新的审批待办，请及时处理。',
    userId: '1001',
    priority: 'HIGH',
    readStatus: 'UNREAD',
    bizName: '审批待办',
    bizType: 'workflow.task.assigned',
    createTime: '2026-07-30 10:00:00',
    subject: { subjectName: '费用报销' },
    data: {
      businessKey: 'EXP-1001',
      applicantName: '张三',
      taskName: '财务审批',
      assigneeName: '李四',
    },
    actions: [routeAction],
    ...overrides,
  };
}

describe('notice message presentation', () => {
  it('按工作流业务阅读顺序输出状态、对象、关键字段和主按钮', () => {
    const presentation = presentNoticeMessage(workflowMessage(), Date.parse('2026-07-30T10:30:00'));

    expect(presentation.typeLabel).toBe('审批待办');
    expect(presentation.statusLabel).toBe('待审批');
    expect(presentation.subject).toBe('费用报销');
    expect(presentation.fields.map((field) => [field.label, field.value])).toEqual([
      ['申请单', 'EXP-1001'],
      ['申请人', '张三'],
      ['当前节点', '财务审批'],
      ['当前处理人', '李四'],
    ]);
    expect(presentation.relativeTime).toBe('30 分钟前');
    expect(presentation.primaryActionLabel).toBe('去审批');
  });

  it('候选任务展示为待领取并使用去领取按钮', () => {
    const message = workflowMessage({
      data: {
        businessKey: 'EXP-1002',
        taskName: '财务审批',
        claimStatus: 'CLAIMABLE',
        candidateGroups: ['ROLE:finance'],
      },
    });

    expect(presentNoticeMessage(message).statusLabel).toBe('待领取');
    expect(resolveNoticeActionLabel(message, routeAction)).toBe('去领取');
  });

  it('终态工作流使用查看申请，且无有效动作时只生成展示信息', () => {
    const completed = workflowMessage({ bizType: 'workflow.process.completed' });
    expect(resolveNoticeActionLabel(completed, routeAction)).toBe('查看申请');

    const noAction = presentNoticeMessage(workflowMessage({ actions: [] }));
    expect(noAction.primaryAction).toBeUndefined();
    expect(noticeDesktopSummary(workflowMessage())).toContain('待审批 · 费用报销');
  });
});
