package io.mango.guarantee.core.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.guarantee.api.query.GuaranteeCaseQuery;
import io.mango.guarantee.api.vo.GuaranteeCaseVO;
import io.mango.guarantee.core.mapper.GuaranteeCaseMapper;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.vo.WorkflowBusinessProcessVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuaranteeCaseServiceImplTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void pageFillsCurrentWorkflowNodeWhenWorkflowApiAvailable() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        GuaranteeCaseMapper caseMapper = mock(GuaranteeCaseMapper.class);
        WorkflowBusinessProcessApi workflowApi = mock(WorkflowBusinessProcessApi.class);
        GuaranteeCaseServiceImpl service = new GuaranteeCaseServiceImpl(caseMapper, new SingleObjectProvider(workflowApi));

        GuaranteeCaseVO caseVO = new GuaranteeCaseVO();
        caseVO.setCaseNo("GH202605170001");
        when(caseMapper.selectVisiblePage(any(Page.class), eq(1L), any(GuaranteeCaseQuery.class)))
                .thenReturn(List.of(caseVO));

        WorkflowBusinessProcessVO processVO = new WorkflowBusinessProcessVO();
        processVO.setBusinessKey("GH202605170001");
        processVO.setProcessInstanceId("proc-1");
        processVO.setProcessName("保函审批");
        processVO.setStatus("审批中");
        processVO.setCurrentTaskName("风控初审");
        processVO.setCurrentTaskDefinitionKey("risk_review");
        when(workflowApi.latestByBusinessKeys(List.of("GH202605170001"))).thenReturn(List.of(processVO));

        PersistencePageResult<GuaranteeCaseVO> result = service.page(new GuaranteeCaseQuery());

        assertThat(result.getRecords()).hasSize(1);
        GuaranteeCaseVO filled = result.getRecords().get(0);
        assertThat(filled.getProcessInstanceId()).isEqualTo("proc-1");
        assertThat(filled.getProcessName()).isEqualTo("保函审批");
        assertThat(filled.getProcessStatus()).isEqualTo("审批中");
        assertThat(filled.getCurrentTaskName()).isEqualTo("风控初审");
        assertThat(filled.getCurrentTaskDefinitionKey()).isEqualTo("risk_review");
        verify(workflowApi).latestByBusinessKeys(List.of("GH202605170001"));
    }

    private record SingleObjectProvider(WorkflowBusinessProcessApi api)
            implements ObjectProvider<WorkflowBusinessProcessApi> {

        @Override
        public WorkflowBusinessProcessApi getObject(Object... args) {
            return api;
        }

        @Override
        public WorkflowBusinessProcessApi getIfAvailable() {
            return api;
        }

        @Override
        public WorkflowBusinessProcessApi getIfUnique() {
            return api;
        }

        @Override
        public WorkflowBusinessProcessApi getObject() {
            return api;
        }
    }
}
