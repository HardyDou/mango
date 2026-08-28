package io.mango.workflow.core.identity;

import io.mango.workflow.api.vo.WorkflowTaskVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowAssigneeIdentityServiceTest {

    @Test
    void enrichTasks_shouldResolveDistinctAssigneesOnceAndPreserveRawNames() {
        IWorkflowAssigneeIdentityProvider provider = mock(IWorkflowAssigneeIdentityProvider.class);
        when(provider.resolveAll(any())).thenReturn(Map.of(
                "admin", new WorkflowAssigneeIdentity(1001L, "管理员"),
                "reviewer", new WorkflowAssigneeIdentity(1002L, "reviewer")));
        WorkflowAssigneeIdentityService service = service(provider);
        WorkflowTaskVO first = task(" admin ");
        WorkflowTaskVO duplicate = task("admin");
        WorkflowTaskVO second = task("reviewer");
        WorkflowTaskVO missing = task("missing");

        service.enrichTasks(List.of(first, duplicate, second, missing));

        verify(provider, times(1)).resolveAll(List.of("admin", "reviewer", "missing"));
        assertThat(first.getAssigneeName()).isEqualTo(" admin ");
        assertThat(first.getAssigneeId()).isEqualTo(1001L);
        assertThat(first.getAssigneeDisplayName()).isEqualTo("管理员");
        assertThat(duplicate.getAssigneeId()).isEqualTo(1001L);
        assertThat(second.getAssigneeDisplayName()).isEqualTo("reviewer");
        assertThat(missing.getAssigneeId()).isNull();
        assertThat(missing.getAssigneeDisplayName()).isNull();
    }

    @Test
    void enrichTasks_shouldFailOpenWhenProviderIsUnavailable() {
        IWorkflowAssigneeIdentityProvider provider = mock(IWorkflowAssigneeIdentityProvider.class);
        when(provider.resolveAll(any())).thenThrow(new IllegalStateException("identity unavailable"));
        WorkflowAssigneeIdentityService service = service(provider);
        WorkflowTaskVO task = task("admin");

        service.enrichTasks(List.of(task));

        assertThat(task.getAssigneeName()).isEqualTo("admin");
        assertThat(task.getAssigneeId()).isNull();
        assertThat(task.getAssigneeDisplayName()).isNull();
    }

    @Test
    void enrichTasks_shouldFailOpenWhenProviderLookupFails() {
        @SuppressWarnings("unchecked")
        ObjectProvider<IWorkflowAssigneeIdentityProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenThrow(new IllegalStateException("provider unavailable"));
        WorkflowAssigneeIdentityService service = new WorkflowAssigneeIdentityService(objectProvider);
        WorkflowTaskVO task = task("admin");

        service.enrichTasks(List.of(task));

        assertThat(task.getAssigneeName()).isEqualTo("admin");
        assertThat(task.getAssigneeId()).isNull();
        assertThat(task.getAssigneeDisplayName()).isNull();
    }

    private WorkflowAssigneeIdentityService service(IWorkflowAssigneeIdentityProvider provider) {
        @SuppressWarnings("unchecked")
        ObjectProvider<IWorkflowAssigneeIdentityProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return new WorkflowAssigneeIdentityService(objectProvider);
    }

    private WorkflowTaskVO task(String assigneeName) {
        WorkflowTaskVO task = new WorkflowTaskVO();
        task.setAssigneeName(assigneeName);
        return task;
    }
}
