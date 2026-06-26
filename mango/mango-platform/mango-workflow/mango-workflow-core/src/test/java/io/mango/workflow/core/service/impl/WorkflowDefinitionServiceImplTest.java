package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.core.engine.WorkflowDesignerBpmnConverter;
import io.mango.workflow.core.entity.WorkflowCategory;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowDefinitionVersion;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.mapper.WorkflowNodeDefinitionMapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.bpmn.model.BpmnModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    private ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    private WorkflowDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dataScopeApplierProvider.getIfAvailable()).thenReturn(new TestDataScopeApplier());
        service = new WorkflowDefinitionServiceImpl(
                definitionMapper,
                categoryMapper,
                versionMapper,
                nodeDefinitionMapper,
                domainApi,
                repositoryService,
                bpmnConverter,
                new ObjectMapper(),
                dataScopeApplierProvider);
    }

    @Test
    void page_shouldApplyDataScopeToWorkflowDefinitionList() {
        Page<WorkflowDefinition> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(definitionMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        WorkflowDefinitionPageQuery query = new WorkflowDefinitionPageQuery();
        query.setPage(1);
        query.setSize(10);
        query.setKeyword("审批");
        query.setDomainCode("PAYMENT");

        R<PageResult<WorkflowDefinitionVO>> result = service.page(query);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Wrapper<WorkflowDefinition>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(definitionMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment)
                .contains("definition_name", "definition_key", "domain_code", "org_id", "created_by")
                .contains("updated_time");
    }

    @Test
    void get_shouldApplyDataScopeToWorkflowDefinitionDetail() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(1001L);
        definition.setCategoryId(10L);
        definition.setDomainCode("PAYMENT");
        definition.setDefinitionName("合同用印审批");
        definition.setDefinitionKey("contract_seal_approval");
        definition.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        when(definitionMapper.selectOne(any(Wrapper.class))).thenReturn(definition);

        R<WorkflowDefinitionVO> result = service.get(1001L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getId()).isEqualTo(1001L);
        ArgumentCaptor<Wrapper<WorkflowDefinition>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(definitionMapper).selectOne(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("id", "org_id", "created_by");
    }

    @Test
    void deployInternal_shouldResolveDefinitionByTenantWithoutMemberDataScope() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(8801L, "55", "workflow-initializer", "INTERNAL",
                        "INTERNAL_USER", "INTERNAL_ORG", 55L, "workflow-starter"));
        try {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setId(3001L);
            definition.setTenantId(55L);
            definition.setCategoryId(20L);
            definition.setDomainCode("PAYMENT");
            definition.setDefinitionName("退款审批");
            definition.setDefinitionKey("PAYMENT_REFUND_APPROVAL");
            definition.setDesignerJson("{\"id\":\"startEvent\"}");
            definition.setStatus(WorkflowDefinitionStatus.DRAFT.name());
            when(definitionMapper.selectOne(any(Wrapper.class))).thenReturn(definition);
            when(versionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
            doAnswer(invocation -> {
                WorkflowDefinitionVersion version = invocation.getArgument(0);
                version.setId(4001L);
                return 1;
            }).when(versionMapper).insert(any(WorkflowDefinitionVersion.class));

            BpmnModel bpmnModel = new BpmnModel();
            when(bpmnConverter.toModel(anyString(), eq("PAYMENT_REFUND_APPROVAL"), eq("退款审批")))
                    .thenReturn(bpmnModel);
            when(bpmnConverter.toXml(anyString(), eq("PAYMENT_REFUND_APPROVAL"), eq("退款审批")))
                    .thenReturn("<definitions />");
            DeploymentBuilder builder = org.mockito.Mockito.mock(DeploymentBuilder.class);
            Deployment deployment = org.mockito.Mockito.mock(Deployment.class);
            ProcessDefinitionQuery processDefinitionQuery = org.mockito.Mockito.mock(ProcessDefinitionQuery.class);
            ProcessDefinition processDefinition = org.mockito.Mockito.mock(ProcessDefinition.class);
            when(repositoryService.createDeployment()).thenReturn(builder);
            when(builder.name("退款审批")).thenReturn(builder);
            when(builder.key("PAYMENT_REFUND_APPROVAL")).thenReturn(builder);
            when(builder.tenantId("55")).thenReturn(builder);
            when(builder.addBpmnModel("PAYMENT_REFUND_APPROVAL.bpmn20.xml", bpmnModel)).thenReturn(builder);
            when(builder.deploy()).thenReturn(deployment);
            when(deployment.getId()).thenReturn("deploy-internal");
            when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
            when(processDefinitionQuery.deploymentId("deploy-internal")).thenReturn(processDefinitionQuery);
            when(processDefinitionQuery.processDefinitionKey("PAYMENT_REFUND_APPROVAL")).thenReturn(processDefinitionQuery);
            when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
            when(processDefinitionQuery.singleResult()).thenReturn(processDefinition);
            when(processDefinition.getId()).thenReturn("proc-internal");
            when(processDefinition.getVersion()).thenReturn(1);

            R<WorkflowDeployVO> result = service.deployInternal(3001L);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<Wrapper<WorkflowDefinition>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
            verify(definitionMapper).selectOne(wrapperCaptor.capture());
            String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
            assertThat(sqlSegment).contains("tenant_id", "id");
            assertThat(sqlSegment).doesNotContain("org_id", "created_by");
        } finally {
            MangoContextHolder.clear();
        }
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
        ArgumentCaptor<Wrapper<WorkflowDefinition>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(definitionMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("status", "published_version_no", "process_definition_id", "org_id", "created_by");
    }

    @Test
    void page_shouldFilterStartEntryVisibleDefinitionsWhenRequested() {
        Page<WorkflowDefinition> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(definitionMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        WorkflowDefinitionPageQuery query = new WorkflowDefinitionPageQuery();
        query.setPage(1);
        query.setSize(10);
        query.setPublishedOnly(true);
        query.setStartEntryVisible(true);

        R<PageResult<WorkflowDefinitionVO>> result = service.page(query);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Wrapper<WorkflowDefinition>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(definitionMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("workflow_definition_version", "version_no = workflow_definition.published_version_no",
                        "start_entry_visible", "status", "published_version_no", "process_definition_id");
    }

    @Test
    void page_shouldOnlyReturnVisibleStartEntriesInPublishedOnlyResult() {
        WorkflowDefinition visibleDefinition = new WorkflowDefinition();
        visibleDefinition.setId(1001L);
        visibleDefinition.setCategoryId(10L);
        visibleDefinition.setDefinitionName("可独立发起流程-草稿名");
        visibleDefinition.setDefinitionKey("VISIBLE_APPROVAL");
        visibleDefinition.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        visibleDefinition.setPublishedVersionNo(2);
        visibleDefinition.setProcessDefinitionId("proc-visible-draft");

        Page<WorkflowDefinition> page = new Page<>(1, 10);
        page.setRecords(List.of(visibleDefinition));
        page.setTotal(1);
        when(definitionMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        WorkflowDefinitionVersion visiblePublished = new WorkflowDefinitionVersion();
        visiblePublished.setDefinitionId(1001L);
        visiblePublished.setVersionNo(2);
        visiblePublished.setCategoryId(10L);
        visiblePublished.setDefinitionName("可独立发起流程");
        visiblePublished.setDefinitionKey("VISIBLE_APPROVAL");
        visiblePublished.setStartEntryVisible(true);
        visiblePublished.setDesignerJson("{\"name\":\"visible\"}");
        visiblePublished.setDeploymentId("deploy-visible");
        visiblePublished.setProcessDefinitionId("proc-visible");
        visiblePublished.setProcessDefinitionVersion(2);
        visiblePublished.setPublishStatus("SUCCESS");
        visiblePublished.setPublishTime(LocalDateTime.parse("2026-06-26T10:00:00"));
        when(versionMapper.selectOne(any(Wrapper.class))).thenReturn(visiblePublished);

        WorkflowDefinitionPageQuery query = new WorkflowDefinitionPageQuery();
        query.setPage(1);
        query.setSize(10);
        query.setPublishedOnly(true);
        query.setStartEntryVisible(true);

        R<PageResult<WorkflowDefinitionVO>> result = service.page(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getList())
                .extracting(WorkflowDefinitionVO::getDefinitionName)
                .containsExactly("可独立发起流程")
                .doesNotContain("仅业务内嵌流程");
        assertThat(result.getData().getList().get(0).getStartEntryVisible()).isTrue();
    }

    @Test
    void ensurePublished_existingPublishedDeployableDefinition_returnsPublishedSnapshotWithoutCreatingOrDeploying() {
        WorkflowCategory category = new WorkflowCategory();
        category.setId(20L);
        category.setStatus(1);
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(category);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(1002L);
        definition.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        definition.setDeploymentId("deploy-existing");
        definition.setProcessDefinitionId("proc-existing");
        definition.setProcessDefinitionVersion(7);
        definition.setPublishedVersionNo(3);
        when(definitionMapper.selectOne(any(Wrapper.class))).thenReturn(definition);

        ProcessDefinitionQuery query = org.mockito.Mockito.mock(ProcessDefinitionQuery.class);
        ProcessDefinition processDefinition = org.mockito.Mockito.mock(ProcessDefinition.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionId("proc-existing")).thenReturn(query);
        when(query.singleResult()).thenReturn(processDefinition);
        when(domainApi.detailByCode("PAYMENT")).thenReturn(R.ok(enabledDomain()));

        R<WorkflowDeployVO> result = service.ensurePublished(ensureCommand());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getDeploymentId()).isEqualTo("deploy-existing");
        assertThat(result.getData().getProcessDefinitionId()).isEqualTo("proc-existing");
        assertThat(result.getData().getProcessDefinitionVersion()).isEqualTo(7);
        assertThat(result.getData().getVersionNo()).isEqualTo(3);
        verify(categoryMapper, never()).insert(any(WorkflowCategory.class));
        verify(definitionMapper, never()).insert(any(WorkflowDefinition.class));
        verify(repositoryService, never()).createDeployment();
    }

    @Test
    void ensurePublished_missingDefinition_createsCategoryAndDefinitionThenDeploys() {
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        WorkflowDefinition createdDefinition = insertedDefinition();
        createdDefinition.setId(3001L);
        when(definitionMapper.selectOne(any(Wrapper.class))).thenReturn(null, createdDefinition);
        when(domainApi.detailByCode("PAYMENT")).thenReturn(R.ok(enabledDomain()));
        when(categoryMapper.selectById(2001L)).thenAnswer(invocation -> {
            WorkflowCategory category = new WorkflowCategory();
            category.setId(invocation.getArgument(0));
            category.setDomainCode("PAYMENT");
            category.setStatus(1);
            return category;
        });
        doAnswer(invocation -> {
            WorkflowCategory category = invocation.getArgument(0);
            category.setId(2001L);
            return 1;
        }).when(categoryMapper).insert(any(WorkflowCategory.class));
        doAnswer(invocation -> {
            WorkflowDefinition definition = invocation.getArgument(0);
            definition.setId(3001L);
            return 1;
        }).when(definitionMapper).insert(any(WorkflowDefinition.class));
        when(versionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            WorkflowDefinitionVersion version = invocation.getArgument(0);
            version.setId(4001L);
            return 1;
        }).when(versionMapper).insert(any(WorkflowDefinitionVersion.class));

        BpmnModel bpmnModel = new BpmnModel();
        when(bpmnConverter.toModel(anyString(), eq("PAYMENT_REFUND_APPROVAL"), eq("退款审批")))
                .thenReturn(bpmnModel);
        when(bpmnConverter.toXml(anyString(), eq("PAYMENT_REFUND_APPROVAL"), eq("退款审批")))
                .thenReturn("<definitions />");
        DeploymentBuilder builder = org.mockito.Mockito.mock(DeploymentBuilder.class);
        Deployment deployment = org.mockito.Mockito.mock(Deployment.class);
        ProcessDefinitionQuery processDefinitionQuery = org.mockito.Mockito.mock(ProcessDefinitionQuery.class);
        ProcessDefinition processDefinition = org.mockito.Mockito.mock(ProcessDefinition.class);
        when(repositoryService.createDeployment()).thenReturn(builder);
        when(builder.name("退款审批")).thenReturn(builder);
        when(builder.key("PAYMENT_REFUND_APPROVAL")).thenReturn(builder);
        when(builder.tenantId("1")).thenReturn(builder);
        when(builder.addBpmnModel("PAYMENT_REFUND_APPROVAL.bpmn20.xml", bpmnModel)).thenReturn(builder);
        when(builder.deploy()).thenReturn(deployment);
        when(deployment.getId()).thenReturn("deploy-created");
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.deploymentId("deploy-created")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey("PAYMENT_REFUND_APPROVAL")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(processDefinition);
        when(processDefinition.getId()).thenReturn("proc-created");
        when(processDefinition.getVersion()).thenReturn(1);

        R<WorkflowDeployVO> result = service.ensurePublished(ensureCommand());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getDeploymentId()).isEqualTo("deploy-created");
        assertThat(result.getData().getProcessDefinitionId()).isEqualTo("proc-created");
        assertThat(result.getData().getProcessDefinitionVersion()).isEqualTo(1);
        assertThat(result.getData().getVersionNo()).isEqualTo(1);
        ArgumentCaptor<WorkflowCategory> categoryCaptor = ArgumentCaptor.forClass(WorkflowCategory.class);
        verify(categoryMapper).insert(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getDomainCode()).isEqualTo("PAYMENT");
        assertThat(categoryCaptor.getValue().getCategoryCode()).isEqualTo("PAYMENT_BUILTIN");
        ArgumentCaptor<WorkflowDefinition> definitionCaptor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(definitionMapper).insert(definitionCaptor.capture());
        assertThat(definitionCaptor.getValue().getDefinitionKey()).isEqualTo("PAYMENT_REFUND_APPROVAL");
        assertThat(definitionCaptor.getValue().getCategoryId()).isEqualTo(2001L);
        verify(versionMapper).insert(any(WorkflowDefinitionVersion.class));
        verify(versionMapper).updateById(any(WorkflowDefinitionVersion.class));
        verify(definitionMapper).updateById(any(WorkflowDefinition.class));
    }

    private EnsureWorkflowDefinitionCommand ensureCommand() {
        EnsureWorkflowDefinitionCommand command = new EnsureWorkflowDefinitionCommand();
        command.setDomainCode("PAYMENT");
        command.setCategoryCode("PAYMENT_BUILTIN");
        command.setCategoryName("支付内置流程");
        command.setCategorySort(20);
        command.setDefinitionName("退款审批");
        command.setDefinitionKey("PAYMENT_REFUND_APPROVAL");
        command.setDesignerJson("{\"id\":\"startEvent\"}");
        command.setFormCode("payment_refund_approval");
        command.setFormJson("{\"mode\":\"CUSTOM\"}");
        command.setAdminUsers(List.of("admin"));
        return command;
    }

    private DomainVO enabledDomain() {
        DomainVO domain = new DomainVO();
        domain.setDomainCode("PAYMENT");
        domain.setStatus(1);
        return domain;
    }

    private WorkflowDefinition insertedDefinition() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setCategoryId(2001L);
        definition.setDomainCode("PAYMENT");
        definition.setDefinitionName("退款审批");
        definition.setDefinitionKey("PAYMENT_REFUND_APPROVAL");
        definition.setDesignerJson("{\"id\":\"startEvent\"}");
        definition.setFormJson("{\"mode\":\"CUSTOM\"}");
        definition.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        return definition;
    }

    private static class TestDataScopeApplier implements DataScopeApplier {

        @Override
        public <T> void apply(QueryWrapper<T> wrapper, String resourceCode, DataScopeMapping mapping) {
            assertThat(resourceCode).isEqualTo("workflow:definition:list");
            assertThat(mapping.orgField()).isEqualTo("org_id");
            assertThat(mapping.selfField()).isEqualTo("created_by");
            wrapper.and(condition -> condition.in(mapping.orgField(), List.of(100L, 200L))
                    .or()
                    .eq(mapping.selfField(), 9001L));
        }
    }
}
