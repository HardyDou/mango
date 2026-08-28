package io.mango.workflow.starter.identity;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.request.IdentityUserBatchRequest;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityUserApiWorkflowAssigneeIdentityProviderTest {

    @Test
    void resolveAll_shouldUseOneBatchRequestForIdsAndUsernames() {
        IdentityUserApi identityUserApi = mock(IdentityUserApi.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IdentityUserApi> apiProvider = mock(ObjectProvider.class);
        when(apiProvider.getIfAvailable()).thenReturn(identityUserApi);
        when(identityUserApi.listUserInfos(org.mockito.ArgumentMatchers.any())).thenReturn(R.ok(List.of(
                user(1001L, "admin", "管理员"),
                user(1002L, "reviewer", null))));
        IdentityUserApiWorkflowAssigneeIdentityProvider provider =
                new IdentityUserApiWorkflowAssigneeIdentityProvider(apiProvider);

        var result = provider.resolveAll(List.of("admin", "1002", "missing"));

        ArgumentCaptor<IdentityUserBatchRequest> queryCaptor = ArgumentCaptor.forClass(IdentityUserBatchRequest.class);
        verify(identityUserApi).listUserInfos(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getUserIds()).containsExactly(1002L);
        assertThat(queryCaptor.getValue().getUsernames()).containsExactly("admin", "missing");
        assertThat(result.get("admin").userId()).isEqualTo(1001L);
        assertThat(result.get("admin").displayName()).isEqualTo("管理员");
        assertThat(result.get("1002").displayName()).isEqualTo("reviewer");
        assertThat(result).doesNotContainKey("missing");
    }

    private IdentityUserInfoVO user(Long userId, String username, String nickname) {
        IdentityUserInfoVO user = new IdentityUserInfoVO();
        user.setUserId(userId);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }
}
