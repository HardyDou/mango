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
});
