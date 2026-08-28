package io.mango.workflow.core.service.impl;

import io.mango.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.workflow.api.command.ReplaceWorkflowBusinessParticipantsCommand;
import io.mango.workflow.api.query.WorkflowParticipationAccessQuery;
import io.mango.workflow.api.query.WorkflowParticipationPageQuery;
import io.mango.workflow.api.vo.WorkflowParticipationAccessVO;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.entity.WorkflowProcessParticipantEntity;
import io.mango.workflow.core.mapper.WorkflowFormInstanceMapper;
import io.mango.workflow.core.mapper.WorkflowProcessParticipantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowParticipationServiceTest {

    private final WorkflowProcessParticipantMapper mapper = mock(WorkflowProcessParticipantMapper.class);
    private final WorkflowFormInstanceMapper formInstanceMapper = mock(WorkflowFormInstanceMapper.class);
    private final TenantMemberProvider tenantMemberProvider = mock(TenantMemberProvider.class);
    private final AuthUserProvider authUserProvider = mock(AuthUserProvider.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final WorkflowParticipationService service = new WorkflowParticipationService(
            mapper, formInstanceMapper, tenantMemberProvider, authUserProvider, jdbcTemplate);

    @BeforeEach
    void resetCollaborators() {
        reset(mapper, formInstanceMapper, tenantMemberProvider, authUserProvider, jdbcTemplate);
    }

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void replaceBusinessParticipants_validatesAllUsersBeforeWriting() {
        setContext(1001L, 11L, "alice");
        TenantMemberVO member = new TenantMemberVO();
        member.setMemberId(501L);
        member.setTenantId(11L);
        member.setUserId(2001L);
        member.setStatus(1);
        AuthUserVO user = new AuthUserVO();
        user.setUserId(2001L);
        user.setUsername("bob");
        user.setStatus(1);
        when(tenantMemberProvider.getEnabledMember(2001L, 11L)).thenReturn(member);
        when(authUserProvider.getByIdForAuth(2001L)).thenReturn(user);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(11L), eq(501L))).thenReturn(1L);
        when(tenantMemberProvider.getEnabledMember(2002L, 11L)).thenReturn(null);

        ReplaceWorkflowBusinessParticipantsCommand command = new ReplaceWorkflowBusinessParticipantsCommand();
        command.setProcessKey("expense");
        command.setBusinessKey("E-1");
        command.setProcessInstanceId("proc-1");
        command.setParticipantUserIds(List.of(2001L, 2002L));

        assertThatThrownBy(() -> service.replaceBusinessParticipants(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("参与用户不属于当前租户或已停用");
        verify(mapper, never()).selectList(any());
        verify(mapper, never()).insert(any(WorkflowProcessParticipantEntity.class));
        verify(mapper, never()).updateById(any(WorkflowProcessParticipantEntity.class));
    }

    @Test
    void accessUsesCurrentTenantAndUserParticipationRows() {
        setContext(1001L, 11L, "alice");
        WorkflowProcessParticipantEntity row = new WorkflowProcessParticipantEntity();
        row.setParticipantType("COMPLETED_HANDLER");
        row.setProcessInstanceId("proc-1");
        when(mapper.selectList(any())).thenReturn(List.of(row));
        WorkflowParticipationAccessQuery query = new WorkflowParticipationAccessQuery();
        query.setProcessKey("expense");
        query.setBusinessKey("E-1");

        WorkflowParticipationAccessVO result = service.access(query);

        assertThat(result.isReadable()).isTrue();
        assertThat(result.getParticipantTypes()).extracting(type -> type.name()).containsExactly("COMPLETED_HANDLER");
        assertThat(result.getLatestProcessInstanceId()).isEqualTo("proc-1");
    }

    @Test
    void myPagesBusinessCoordinatesInDatabaseBeforeLoadingParticipantTypes() {
        setContext(1001L, 11L, "alice");
        WorkflowProcessParticipantEntity summary = participant("expense", "E-2", null,
                null, LocalDateTime.of(2026, 8, 28, 10, 0));
        when(mapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
            Page<WorkflowProcessParticipantEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(summary));
            page.setTotal(2);
            return page;
        });
        WorkflowProcessParticipantEntity handler = participant("expense", "E-2", "proc-2",
                "COMPLETED_HANDLER", LocalDateTime.of(2026, 8, 28, 10, 0));
        WorkflowProcessParticipantEntity initiator = participant("expense", "E-2", "proc-2",
                "INITIATOR", LocalDateTime.of(2026, 8, 27, 10, 0));
        when(mapper.selectList(any())).thenReturn(List.of(handler, initiator));
        WorkflowParticipationPageQuery query = new WorkflowParticipationPageQuery();
        query.setPage(1);
        query.setSize(100);

        var result = service.my(query);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getProcessInstanceId()).isEqualTo("proc-2");
        assertThat(result.getList().get(0).getParticipantTypes())
                .extracting(Enum::name)
                .containsExactly("INITIATOR", "COMPLETED_HANDLER");
    }

    @Test
    void replaceBusinessParticipantsRejectsMismatchedProcessCoordinatesBeforeWriting() {
        setContext(1001L, 11L, "alice");
        TenantMemberVO member = new TenantMemberVO();
        member.setMemberId(501L);
        member.setTenantId(11L);
        member.setUserId(2001L);
        member.setStatus(1);
        AuthUserVO user = new AuthUserVO();
        user.setUserId(2001L);
        user.setStatus(1);
        WorkflowFormInstanceEntity form = new WorkflowFormInstanceEntity();
        form.setDefinitionKey("leave");
        form.setBusinessKey("L-1");
        when(tenantMemberProvider.getEnabledMember(2001L, 11L)).thenReturn(member);
        when(authUserProvider.getByIdForAuth(2001L)).thenReturn(user);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(11L), eq(501L))).thenReturn(1L);
        when(formInstanceMapper.selectOne(any())).thenReturn(form);
        ReplaceWorkflowBusinessParticipantsCommand command = new ReplaceWorkflowBusinessParticipantsCommand();
        command.setProcessKey("expense");
        command.setBusinessKey("E-1");
        command.setProcessInstanceId("proc-1");
        command.setParticipantUserIds(List.of(2001L));

        assertThatThrownBy(() -> service.replaceBusinessParticipants(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("流程实例与业务坐标不匹配");
        verify(mapper, never()).selectList(any());
        verify(mapper, never()).insert(any(WorkflowProcessParticipantEntity.class));
        verify(mapper, never()).updateById(any(WorkflowProcessParticipantEntity.class));
    }

    private WorkflowProcessParticipantEntity participant(String processKey, String businessKey,
                                                         String processInstanceId, String type,
                                                         LocalDateTime lastParticipatedAt) {
        WorkflowProcessParticipantEntity row = new WorkflowProcessParticipantEntity();
        row.setProcessKey(processKey);
        row.setBusinessKey(businessKey);
        row.setProcessInstanceId(processInstanceId);
        row.setParticipantType(type);
        row.setLastParticipatedAt(lastParticipatedAt);
        return row;
    }

    private void setContext(Long userId, Long tenantId, String principal) {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(userId, String.valueOf(tenantId), principal, "default", "USER", "ORG", 100L, "internal-admin"));
    }
}
