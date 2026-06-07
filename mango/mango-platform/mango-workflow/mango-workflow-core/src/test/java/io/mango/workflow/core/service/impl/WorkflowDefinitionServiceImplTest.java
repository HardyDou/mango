package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.core.engine.WorkflowDesignerBpmnConverter;
import io.mango.workflow.core.entity.WorkflowCategory;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowDefinitionVersion;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.mapper.WorkflowNodeDefinitionMapper;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WorkflowDefinitionServiceImplTest {

    @Mock
    private WorkflowDefinitionMapper definitionMapper;
    @Mock
    private WorkflowCategoryMapper categoryMapper;
    @Mock
    private WorkflowDefinitionVersionMapper versionMapper;
    @Mock
    private WorkflowNodeDefinitionMapper nodeDefinitionMapper;
    @Mock
    private DomainApi domainApi;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private WorkflowDesignerBpmnConverter bpmnConverter;

    private WorkflowDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkflowDefinitionServiceImpl(
                definitionMapper,
                categoryMapper,
                versionMapper,
                nodeDefinitionMapper,
                domainApi,
                repositoryService,
                bpmnConverter,
                new ObjectMapper());
    }

    @Test
    void page_shouldReturnPublishedVersionSnapshotWhenPublishedOnly() {
        WorkflowDefinition draft = new WorkflowDefinition();
        draft.setId(1001L);
        draft.setCategoryId(10L);
        draft.setDefinitionName("合同用印审批-未发布草稿");
        draft.setDefinitionKey("contract_seal_approval");
        draft.setIcon("draft-icon");
        draft.setFormJson("{\"mode\":\"draft\"}");
        draft.setDesignerJson("{\"name\":\"draft\"}");
        draft.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        draft.setPublishedVersionNo(1);
        draft.setProcessDefinitionId("proc-main-draft");
        draft.setLastDeployTime(LocalDateTime.parse("2026-05-21T13:00:00"));

        Page<WorkflowDefinition> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(draft));
        page.setTotal(1);
        when(definitionMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        WorkflowDefinitionVersion published = new WorkflowDefinitionVersion();
        published.setDefinitionId(1001L);
        published.setVersionNo(1);
        published.setCategoryId(10L);
        published.setDefinitionName("合同用印审批");
        published.setDefinitionKey("contract_seal_approval");
        published.setIcon("published-icon");
        published.setFormJson("{\"mode\":\"published\"}");
        published.setDesignerJson("{\"name\":\"published\"}");
        published.setDeploymentId("deploy-1");
        published.setProcessDefinitionId("proc-published");
        published.setProcessDefinitionVersion(3);
        published.setPublishStatus("SUCCESS");
        published.setPublishTime(LocalDateTime.parse("2026-05-20T09:30:00"));
        when(versionMapper.selectOne(any(Wrapper.class))).thenReturn(published);
        WorkflowCategory category = new WorkflowCategory();
        category.setId(10L);
        category.setCategoryName("通用流程");
        when(categoryMapper.selectById(10L)).thenReturn(category);

        WorkflowDefinitionPageQuery query = new WorkflowDefinitionPageQuery();
        query.setPage(1);
        query.setSize(10);
        query.setPublishedOnly(true);

        R<PageResult<WorkflowDefinitionVO>> result = service.page(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getList()).hasSize(1);
        WorkflowDefinitionVO vo = result.getData().getList().get(0);
        assertThat(vo.getDefinitionName()).isEqualTo("合同用印审批");
        assertThat(vo.getIcon()).isEqualTo("published-icon");
        assertThat(vo.getFormJson()).isEqualTo("{\"mode\":\"published\"}");
        assertThat(vo.getDesignerJson()).isEqualTo("{\"name\":\"published\"}");
        assertThat(vo.getProcessDefinitionId()).isEqualTo("proc-published");
        assertThat(vo.getCategoryName()).isEqualTo("通用流程");
        assertThat(vo.getHasUnpublishedChanges()).isFalse();
    }
}
