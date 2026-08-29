import { createApp, nextTick } from 'vue';
import { describe, expect, it } from 'vitest';
import * as workflowPackage from '../../index';

describe('workflow business ui exports', () => {
  it('exports business layout and sidebar components', () => {
    expect(workflowPackage.WorkflowLayout).toBeTruthy();
    expect(workflowPackage.WorkflowSidebar).toBeTruthy();
    expect(workflowPackage.WorkflowInstanceSummary).toBeTruthy();
    expect(workflowPackage.WorkflowInstanceProgress).toBeTruthy();
    expect(workflowPackage.WorkflowDefinitionGraph).toBeTruthy();
    expect(workflowPackage.WorkflowDefinitionGraphDialog).toBeTruthy();
    expect(workflowPackage.WorkflowInstanceHistory).toBeTruthy();
    expect(workflowPackage.WorkflowInstanceHistoryDialog).toBeTruthy();
  });

  it.each([
    [{ assigneeDisplayName: '管理员', assigneeName: 'admin' }, '管理员'],
    [{ assigneeName: 'admin' }, 'admin'],
    [{ claimStatus: 'UNCLAIMED' as const }, '待领取'],
    [{}, '-'],
  ])('renders the workflow assignee fallback order for %o', async (summary, expected) => {
    const host = document.createElement('div');
    document.body.appendChild(host);
    const app = createApp(workflowPackage.WorkflowInstanceSummary, { summary });

    app.mount(host);
    await nextTick();

    const assignee = Array.from(host.querySelectorAll('.workflow-instance-summary__item'))
      .find(item => item.querySelector('span')?.textContent === '办理人');
    expect(assignee?.querySelector('strong')?.textContent).toBe(expected);
    app.unmount();
    host.remove();
  });
});
