import { resolveBusinessApplyRegistration } from '@mango/workflow';
import { registerMangoWorkflowBusinessExampleAdminPages } from '../admin-pages';

vi.mock('../views/business-form/index.vue', () => ({ default: {} }));
vi.mock('../business-components/DocumentTableApprovalDetail.vue', () => ({ default: {} }));
vi.mock('../business-components/ExpenseApprovalDetail.vue', () => ({ default: {} }));
vi.mock('@mango/admin-pages/core', () => ({
  registerModulePages: vi.fn(),
}));

describe('workflow business example admin pages', () => {
  it('registers custom apply components before the workflow custom apply page resolves them', () => {
    registerMangoWorkflowBusinessExampleAdminPages();

    expect(resolveBusinessApplyRegistration('workflow.expense.apply')?.title).toBe('费用报销申请');
    expect(resolveBusinessApplyRegistration('workflow.contractSeal.apply')?.title).toBe('合同用印申请');
  });
});
