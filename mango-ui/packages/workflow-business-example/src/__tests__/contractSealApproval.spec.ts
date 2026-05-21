import {
  collectBusinessApprovalComment,
  collectBusinessApprovalVariables,
  resolveBusinessApprovalRegistration,
  type BusinessApprovalContext,
} from '@mango/workflow/src/components/businessApproval';
import { registerWorkflowBusinessExampleComponents } from '../register';

vi.mock('../views/business-form/index.vue', () => ({ default: {} }));
vi.mock('../business-components/DocumentTableApprovalDetail.vue', () => ({ default: {} }));
vi.mock('../business-components/ExpenseApprovalDetail.vue', () => ({ default: {} }));

describe('contract seal approval registration', () => {
  it('binds editable manager opinion as workflow comment and variable', () => {
    registerWorkflowBusinessExampleComponents();
    const registration = resolveBusinessApprovalRegistration('workflow.contractSeal.approve.manager');
    const context = contractContext({
      managerOpinion: '部门负责人同意用印',
      legalOpinion: '只读法务意见',
    }, {
      managerOpinion: 'EDITABLE',
      legalOpinion: 'READONLY',
      financeOpinion: 'HIDDEN',
      sealKeeperOpinion: 'HIDDEN',
    });

    expect(registration?.commentMode).toBe('BUSINESS_FORM');
    expect(collectBusinessApprovalComment(registration, context, 'complete')).toBe('部门负责人同意用印');
    expect(collectBusinessApprovalVariables(registration, context, 'complete')).toEqual({
      managerOpinion: '部门负责人同意用印',
    });
  });

  it('binds legal and finance editable opinions as workflow comments', () => {
    registerWorkflowBusinessExampleComponents();
    const legalRegistration = resolveBusinessApprovalRegistration('workflow.contractSeal.approve.legal');
    const financeRegistration = resolveBusinessApprovalRegistration('workflow.contractSeal.approve.finance');

    expect(collectBusinessApprovalComment(legalRegistration, contractContext({
      legalOpinion: '法务审核通过',
    }, {
      legalOpinion: 'EDITABLE',
    }), 'complete')).toBe('法务审核通过');

    expect(collectBusinessApprovalComment(financeRegistration, contractContext({
      financeOpinion: '财务复核通过',
    }, {
      financeOpinion: 'EDITABLE',
    }), 'complete')).toBe('财务复核通过');
  });

  it('collects seal keeper opinion and validates approved seal count', async () => {
    registerWorkflowBusinessExampleComponents();
    const registration = resolveBusinessApprovalRegistration('workflow.contractSeal.approve.sealKeeper');
    const context = contractContext({
      approvedSealCount: 2,
      sealKeeperOpinion: '已登记实际用印',
    }, {
      sealKeeperOpinion: 'EDITABLE',
    });

    expect(collectBusinessApprovalVariables(registration, context, 'complete')).toEqual({
      approvedSealCount: 2,
      sealKeeperOpinion: '已登记实际用印',
    });
    await expect(registration?.validateBeforeAction?.(context, 'complete')).resolves.toBeUndefined();

    await expect(registration?.validateBeforeAction?.(contractContext({
      approvedSealCount: -1,
      sealKeeperOpinion: '数量异常',
    }, {
      sealKeeperOpinion: 'EDITABLE',
    }), 'complete')).rejects.toThrow('实际用印份数不能小于 0');
  });

  it('validates required business form opinion before submitting approval', async () => {
    registerWorkflowBusinessExampleComponents();
    const registration = resolveBusinessApprovalRegistration('workflow.contractSeal.approve.manager');
    const context = contractContext({
      managerOpinion: '   ',
    }, {
      managerOpinion: 'EDITABLE',
    });

    expect(collectBusinessApprovalComment(registration, context, 'complete')).toBe('');
    await expect(registration?.validateBeforeAction?.(context, 'complete')).rejects.toThrow('请填写审批意见');
  });
});

function contractContext(
  variables: Record<string, any>,
  permissions: Record<string, any>,
): BusinessApprovalContext {
  return {
    businessType: 'CONTRACT_SEAL_APPROVAL',
    businessKey: 'SEAL-E2E-001',
    applyId: 'APPLY-SEAL-E2E-001',
    processInstanceId: 'PROC-1',
    taskId: 'TASK-1',
    taskDefinitionKey: 'dept_manager_approve',
    nodeName: '部门负责人审批',
    nodeExtension: {},
    readonly: false,
    variables,
    permissions,
    records: [],
  };
}
