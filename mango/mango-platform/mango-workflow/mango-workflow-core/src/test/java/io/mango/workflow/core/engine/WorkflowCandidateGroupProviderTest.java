package io.mango.workflow.core.engine;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowCandidateGroupProviderTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final WorkflowCandidateGroupProvider provider = new WorkflowCandidateGroupProvider(jdbcTemplate);

    @Test
    void memberUserIds_shouldExpandRoleGroupToActiveMemberUserIds() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                .thenReturn(List.of(1002L, 1003L));

        assertThat(provider.memberUserIds("ROLE:8173435341741372828")).containsExactly(1002L, 1003L);
    }

    @Test
    void memberUserIds_shouldExpandOrgLeaderGroup() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                .thenReturn(List.of(1004L));

        assertThat(provider.memberUserIds("ORG_LEADER:9")).containsExactly(1004L);
    }

    @Test
    void memberUserIds_shouldIgnoreUnknownPrefixWithoutQuery() {
        assertThat(provider.memberUserIds("DEPT:1")).isEmpty();
        assertThat(provider.memberUserIds("ROLE")).isEmpty();
        assertThat(provider.memberUserIds(" ")).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void memberUserIds_shouldIgnoreNonNumericGroupId() {
        assertThat(provider.memberUserIds("ROLE:risk-managers")).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }
}
