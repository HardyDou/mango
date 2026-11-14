package io.mango.workflow.core.engine;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowAssigneeCollectionTest {

    private final WorkflowCandidateGroupProvider candidateGroupProvider = mock(WorkflowCandidateGroupProvider.class);
    private final AuthUserProvider authUserProvider = mock(AuthUserProvider.class);
    private final WorkflowAssigneeCollection collection =
            new WorkflowAssigneeCollection(candidateGroupProvider, authUserProvider);

    @Test
    void groupMembers_shouldResolveGroupMembersToUsernames() {
        when(candidateGroupProvider.memberUserIds("ROLE:817")).thenReturn(List.of(1002L, 1003L));
        when(authUserProvider.getByIdForAuth(1002L)).thenReturn(authUser(1002L, "finance"));
        when(authUserProvider.getByIdForAuth(1003L)).thenReturn(authUser(1003L, "safety"));

        assertThat(collection.groupMembers("ROLE:817")).containsExactly("finance", "safety");
    }

    @Test
    void groupMembers_shouldDeduplicateMembersAcrossGroups() {
        when(candidateGroupProvider.memberUserIds("ROLE:817")).thenReturn(List.of(1002L, 1003L));
        when(candidateGroupProvider.memberUserIds("POST:2001")).thenReturn(List.of(1003L));
        when(authUserProvider.getByIdForAuth(1002L)).thenReturn(authUser(1002L, "finance"));
        when(authUserProvider.getByIdForAuth(1003L)).thenReturn(authUser(1003L, "safety"));

        assertThat(collection.groupMembers("ROLE:817, POST:2001")).containsExactly("finance", "safety");
    }

    @Test
    void groupMembers_shouldKeepEmptyAssigneeMarkerWhenGroupHasNoMember() {
        when(candidateGroupProvider.memberUserIds("ROLE:999")).thenReturn(List.of());

        assertThat(collection.groupMembers("ROLE:999"))
                .containsExactly(WorkflowAssigneeCollection.EMPTY_ASSIGNEE);
    }

    @Test
    void groupMembers_shouldKeepEmptyAssigneeMarkerWhenGroupsBlank() {
        assertThat(collection.groupMembers(" ")).containsExactly(WorkflowAssigneeCollection.EMPTY_ASSIGNEE);
    }

    @Test
    void groupMembers_shouldFallbackToUserIdWhenUsernameMissing() {
        when(candidateGroupProvider.memberUserIds("ORG:5")).thenReturn(List.of(1004L));
        when(authUserProvider.getByIdForAuth(1004L)).thenReturn(null);

        assertThat(collection.groupMembers("ORG:5")).containsExactly("1004");
    }

    private AuthUserVO authUser(Long userId, String username) {
        AuthUserVO user = new AuthUserVO();
        user.setUserId(userId);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
