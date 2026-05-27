import {
  registerBusinessApplyComponents,
  registerBusinessApprovalComponents,
  type BusinessApprovalContext,
  type WorkflowTaskActionKey,
} from '@mango/workflow';
import WorkflowBusinessFormView from './views/business-form/index.vue';
import DocumentTableApprovalDetail from './business-components/DocumentTableApprovalDetail.vue';
import ExpenseApprovalDetail from './business-components/ExpenseApprovalDetail.vue';

let registered = false;

export function registerWorkflowBusinessExampleComponents() {
  if (registered) {
    return;
  }
  registered = true;

  registerBusinessApplyComponents({
    'workflow.expense.apply': {
      title: '费用报销申请',
      component: WorkflowBusinessFormView,
    },
    'workflow.contractSeal.apply': {
      title: '合同用印申请',
      component: WorkflowBusinessFormView,
    },
  });

  registerBusinessApprovalComponents({
    'workflow.contractSeal.approve': {
      component: DocumentTableApprovalDetail,
      collectVariables: collectContractSealVariables,
      collectComment: collectContractSealComment,
      validateBeforeAction: validateContractSealAction,
      commentMode: 'BUSINESS_FORM',
    },
    'workflow.contractSeal.approve.manager': {
      component: DocumentTableApprovalDetail,
      collectVariables: collectContractSealVariables,
      collectComment: collectContractSealComment,
      validateBeforeAction: validateContractSealAction,
      commentMode: 'BUSINESS_FORM',
    },
    'workflow.contractSeal.approve.legal': {
      component: DocumentTableApprovalDetail,
      collectVariables: collectContractSealVariables,
      collectComment: collectContractSealComment,
      validateBeforeAction: validateContractSealAction,
      commentMode: 'BUSINESS_FORM',
    },
    'workflow.contractSeal.approve.finance': {
      component: DocumentTableApprovalDetail,
      collectVariables: collectContractSealVariables,
      collectComment: collectContractSealComment,
      validateBeforeAction: validateContractSealAction,
      commentMode: 'BUSINESS_FORM',
    },
    'workflow.contractSeal.approve.sealKeeper': {
      component: DocumentTableApprovalDetail,
      collectVariables: collectContractSealVariables,
      collectComment: collectContractSealComment,
      validateBeforeAction: validateContractSealAction,
      commentMode: 'BUSINESS_FORM',
    },
    'workflow.expense.approve': {
      component: ExpenseApprovalDetail,
      collectVariables: collectExpenseVariables,
    },
    'workflow.expense.approve.manager': {
      component: ExpenseApprovalDetail,
      collectVariables: collectExpenseVariables,
    },
    'workflow.expense.approve.finance': {
      component: ExpenseApprovalDetail,
      collectVariables: collectExpenseVariables,
    },
  });
}

export const registerWorkflowBusinessExampleApprovalComponents = registerWorkflowBusinessExampleComponents;

function collectExpenseVariables(context: BusinessApprovalContext) {
  if (context.permissions.financeReview !== 'EDITABLE') {
    return {};
  }
  return {
    approvedAmount: context.variables.approvedAmount,
  };
}

function collectContractSealVariables(context: BusinessApprovalContext) {
  const result: Record<string, any> = {};
  if (context.permissions.managerOpinion === 'EDITABLE') {
    result.managerOpinion = context.variables.managerOpinion;
  }
  if (context.permissions.legalOpinion === 'EDITABLE') {
    result.legalOpinion = context.variables.legalOpinion;
  }
  if (context.permissions.financeOpinion === 'EDITABLE') {
    result.financeOpinion = context.variables.financeOpinion;
  }
  if (context.permissions.sealKeeperOpinion === 'EDITABLE') {
    result.approvedSealCount = context.variables.approvedSealCount;
    result.sealKeeperOpinion = context.variables.sealKeeperOpinion;
  }
  return result;
}

function collectContractSealComment(context: BusinessApprovalContext) {
  if (context.permissions.managerOpinion === 'EDITABLE') {
    return context.variables.managerOpinion;
  }
  if (context.permissions.legalOpinion === 'EDITABLE') {
    return context.variables.legalOpinion;
  }
  if (context.permissions.financeOpinion === 'EDITABLE') {
    return context.variables.financeOpinion;
  }
  if (context.permissions.sealKeeperOpinion === 'EDITABLE') {
    return context.variables.sealKeeperOpinion;
  }
  return undefined;
}

async function validateContractSealAction(context: BusinessApprovalContext, action: WorkflowTaskActionKey) {
  const comment = collectContractSealComment(context);
  if ((action === 'complete' || action === 'reject') && comment !== undefined && !String(comment).trim()) {
    throw new Error('请填写审批意见');
  }
  if (action === 'complete' && context.permissions.sealKeeperOpinion === 'EDITABLE'
    && Number(context.variables.approvedSealCount ?? -1) < 0) {
    throw new Error('实际用印份数不能小于 0');
  }
}
