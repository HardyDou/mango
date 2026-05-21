package io.mango.workflow.core.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowApprovalThresholdTest {

    private final WorkflowApprovalThreshold threshold = new WorkflowApprovalThreshold();

    @Test
    void requiredApprovals_shouldCeilByPassRatio() {
        assertThat(threshold.requiredApprovals(3, 50)).isEqualTo(2);
        assertThat(threshold.requiredApprovals(5, 60)).isEqualTo(3);
        assertThat(threshold.requiredApprovals(4, 100)).isEqualTo(4);
    }

    @Test
    void requiredApprovals_shouldClampRatio() {
        assertThat(threshold.requiredApprovals(3, 0)).isEqualTo(1);
        assertThat(threshold.requiredApprovals(3, 120)).isEqualTo(3);
        assertThat(threshold.requiredApprovals(0, 50)).isEqualTo(1);
    }
}
