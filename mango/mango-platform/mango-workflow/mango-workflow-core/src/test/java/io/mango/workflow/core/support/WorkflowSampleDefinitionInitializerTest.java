package io.mango.workflow.core.support;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.core.entity.WorkflowCategory;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.service.IWorkflowDefinitionService;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowSampleDefinitionInitializerTest {

    @Mock
    private WorkflowCategoryMapper categoryMapper;
    @Mock
    private WorkflowDefinitionMapper definitionMapper;
    @Mock
    private IWorkflowDefinitionService definitionService;
    @Mock
    private RepositoryService repositoryService;

    private WorkflowSampleProperties properties;
    private WorkflowSampleDefinitionInitializer initializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new WorkflowSampleProperties();
        initializer = new WorkflowSampleDefinitionInitializer(
                properties,
                categoryMapper,
                definitionMapper,
                definitionService,
                repositoryService,
                new ObjectMapper());
    }

    @Test
    void run_sampleDataMissing_usesDefaultDomainCode() {
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(definitionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            WorkflowCategory category = invocation.getArgument(0);
            category.setId(10L);
            return 1;
        }).when(categoryMapper).insert(any(WorkflowCategory.class));
        when(definitionService.create(any(SaveWorkflowDefinitionCommand.class)))
                .thenReturn(R.ok("1001"), R.ok("1002"), R.ok("1003"));
        when(definitionService.deploy(any(Long.class))).thenReturn(R.ok());

        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<WorkflowCategory> categoryCaptor = ArgumentCaptor.forClass(WorkflowCategory.class);
        verify(categoryMapper).insert(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getDomainCode()).isEqualTo("COMMON");

        ArgumentCaptor<SaveWorkflowDefinitionCommand> commandCaptor =
                ArgumentCaptor.forClass(SaveWorkflowDefinitionCommand.class);
        verify(definitionService, org.mockito.Mockito.times(3)).create(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(SaveWorkflowDefinitionCommand::getDomainCode)
                .containsOnly("COMMON");
    }
}
