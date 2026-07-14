package io.mango.workflow.core.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.workflow.core.entity.WorkflowEnginePropertyEntity;
import io.mango.workflow.core.mapper.WorkflowEnginePropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 在 Flowable 引擎构建前补齐其运行所需的固定元数据。
 *
 * <p>Flyway 只负责 DDL；这里仅插入缺失项，绝不覆盖引擎已经维护的值，
 * 尤其不会重置 {@code next.dbid}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@ConditionalOnProperty(prefix = "flowable", name = "database-schema-update", havingValue = "false",
        matchIfMissing = true)
public class WorkflowEngineMetadataInitializer implements ProcessEngineConfigurationConfigurer {

    private static final String FLOWABLE_SCHEMA_VERSION = "7.0.0.0";
    private static final List<EngineProperty> REQUIRED_PROPERTIES = List.of(
            new EngineProperty("common.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("next.dbid", "1"),
            new EngineProperty("identitylink.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("entitylink.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("eventsubscription.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("task.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("variable.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("job.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("batch.schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("schema.version", FLOWABLE_SCHEMA_VERSION),
            new EngineProperty("schema.history", "create(" + FLOWABLE_SCHEMA_VERSION + ")")
    );

    private final WorkflowEnginePropertyMapper propertyMapper;

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        REQUIRED_PROPERTIES.forEach(this::insertIfMissing);
    }

    private void insertIfMissing(EngineProperty property) {
        long count = propertyMapper.selectCount(new LambdaQueryWrapper<WorkflowEnginePropertyEntity>()
                .eq(WorkflowEnginePropertyEntity::getName, property.name()));
        if (count > 0) {
            return;
        }
        try {
            WorkflowEnginePropertyEntity entity = new WorkflowEnginePropertyEntity();
            entity.setName(property.name());
            entity.setValue(property.value());
            entity.setRevision(1);
            propertyMapper.insert(entity);
        } catch (DuplicateKeyException ignored) {
            log.debug("Flowable engine property was initialized concurrently: {}", property.name());
        }
    }

    private record EngineProperty(String name, String value) {
    }
}
