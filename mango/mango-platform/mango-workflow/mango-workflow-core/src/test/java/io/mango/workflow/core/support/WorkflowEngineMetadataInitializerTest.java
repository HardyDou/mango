package io.mango.workflow.core.support;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.mango.workflow.core.entity.WorkflowEnginePropertyEntity;
import io.mango.workflow.core.mapper.WorkflowEnginePropertyMapper;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowEngineMetadataInitializerTest {

    private WorkflowEnginePropertyMapper propertyMapper;
    private WorkflowEngineMetadataInitializer initializer;

    @BeforeEach
    void setUp() {
        propertyMapper = mock(WorkflowEnginePropertyMapper.class);
        initializer = new WorkflowEngineMetadataInitializer(propertyMapper);
    }

    @Test
    void configure_emptyPropertyTable_insertsAllRequiredMetadata() {
        initializer.configure(new SpringProcessEngineConfiguration());

        ArgumentCaptor<WorkflowEnginePropertyEntity> propertyCaptor =
                ArgumentCaptor.forClass(WorkflowEnginePropertyEntity.class);
        verify(propertyMapper, times(11)).insert(propertyCaptor.capture());
        assertThat(propertyCaptor.getAllValues())
                .extracting(WorkflowEnginePropertyEntity::getName)
                .containsExactly(
                "common.schema.version", "next.dbid", "identitylink.schema.version",
                "entitylink.schema.version", "eventsubscription.schema.version", "task.schema.version",
                "variable.schema.version", "job.schema.version", "batch.schema.version",
                "schema.version", "schema.history");
        assertThat(propertyCaptor.getAllValues())
                .extracting(WorkflowEnginePropertyEntity::getValue)
                .contains("1", "7.0.0.0", "create(7.0.0.0)");
    }

    @Test
    void configure_existingProperties_doesNotOverwriteEngineState() {
        when(propertyMapper.selectCount(any())).thenReturn(1L);

        initializer.configure(new SpringProcessEngineConfiguration());

        verify(propertyMapper, never()).insert(any(WorkflowEnginePropertyEntity.class));
    }

    @Test
    void enginePropertyMapper_bypassesTenantInterceptor() {
        InterceptorIgnore ignore = WorkflowEnginePropertyMapper.class.getAnnotation(InterceptorIgnore.class);

        assertThat(ignore).isNotNull();
        assertThat(ignore.tenantLine()).isEqualTo("true");
    }
}
