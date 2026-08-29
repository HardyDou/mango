package io.mango.workflow.core.service.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAutoAssignmentStrategyTest {

    @Test
    void leastTasksChoosesLowestCountAndUsesStableUserIdTieBreak() {
        assertThat(WorkflowAutoAssignmentSelector.selectLeastTasksCandidate(
                List.of(30L, 10L, 20L), Map.of(30L, 2L, 10L, 1L, 20L, 1L)))
                .isEqualTo(10L);
    }

    @Test
    void affinityChoosesMostRecentHandlerThatRemainsACandidate() {
        assertThat(WorkflowAutoAssignmentSelector.selectAffinityCandidate(
                List.of(10L, 20L), List.of(30L, 20L, 10L)))
                .isEqualTo(20L);
    }

    @Test
    void affinityReturnsNullWhenHistoryHasNoCandidate() {
        assertThat(WorkflowAutoAssignmentSelector.selectAffinityCandidate(
                List.of(10L, 20L), List.of(30L, 40L)))
                .isNull();
    }
}
