package io.mango.workflow.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.mapper.WorkflowNodeDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowTemplateCategoryMapper;
import io.mango.workflow.core.service.IWorkflowDefinitionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowResourceHandlerIntegrationTest {

    private AnnotationConfigApplicationContext context;
    private DataSource dataSource;
    private WorkflowCategoryResourceHandler categoryHandler;
    private WorkflowTemplateCategoryResourceHandler templateCategoryHandler;
    private WorkflowNodeDefinitionResourceHandler nodeDefinitionHandler;
    private WorkflowDefinitionResourceHandler definitionHandler;
    private CapturingWorkflowDefinitionService definitionService;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.datasource.url", "jdbc:h2:mem:workflow_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username", "sa",
                "spring.datasource.password", "",
                "spring.datasource.driver-class-name", "org.h2.Driver",
                "spring.flyway.enabled", "false",
                "mango.persistence.mybatis-plus.tenant.enabled", "false"
        )));
        context.register(TestConfig.class);
        context.refresh();
        dataSource = context.getBean(DataSource.class);
        categoryHandler = context.getBean(WorkflowCategoryResourceHandler.class);
        templateCategoryHandler = context.getBean(WorkflowTemplateCategoryResourceHandler.class);
        nodeDefinitionHandler = context.getBean(WorkflowNodeDefinitionResourceHandler.class);
        definitionHandler = context.getBean(WorkflowDefinitionResourceHandler.class);
        definitionService = context.getBean(CapturingWorkflowDefinitionService.class);
        rebuildTables();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        MangoContextHolder.clear();
    }

    @Test
    void upsertCreatesCategoriesAndNodeDefinitions() throws Exception {
        categoryHandler.upsert(categoryDeclaration("通用流程", 1));
        templateCategoryHandler.upsert(templateCategoryDeclaration());
        for (ResourceDeclaration declaration : nodeDefinitionDeclarations()) {
            nodeDefinitionHandler.upsert(declaration);
        }

        assertThat(count("workflow_category")).isOne();
        assertThat(count("workflow_template_category")).isOne();
        assertThat(count("workflow_node_definition")).isEqualTo(9);
        assertThat(stringValue("workflow_category", "category_name", "id = 1")).isEqualTo("通用流程");
        assertThat(stringValue("workflow_template_category", "icon", "id = 1")).isEqualTo("CollectionTag");
        assertThat(stringValue("workflow_node_definition", "node_name", "id = 360000002")).isEqualTo("审批节点");
        assertThat(stringValue("workflow_node_definition", "default_properties", "id = 360000007"))
                .isEqualTo("{\"url\":\"\",\"method\":\"POST\",\"timeoutMillis\":5000}");
    }

    @Test
    void upsertDisableAndDeleteUpdateWorkflowCategory() throws Exception {
        ResourceDeclaration declaration = categoryDeclaration("通用流程", 1);

        categoryHandler.upsert(declaration);
        declaration.getFields().get("categoryName").setValue("通用流程-更新");
        categoryHandler.upsert(declaration);

        assertThat(count("workflow_category")).isOne();
        assertThat(stringValue("workflow_category", "category_name", "id = 1")).isEqualTo("通用流程-更新");

        categoryHandler.disable(declaration);

        assertThat(intValue("workflow_category", "status", "id = 1")).isZero();

        categoryHandler.delete(declaration);

        assertThat(count("workflow_category")).isZero();
    }

    @Test
    void upsertDisableAndDeleteUpdateWorkflowDefinition() throws Exception {
        ResourceDeclaration declaration = workflowDefinitionDeclaration("保函优惠审批");
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("2"));

        ResourceSyncResult result = definitionHandler.upsert(declaration);

        assertThat(result.getTargetId()).isNotNull();
        assertThat(count("workflow_definition")).isOne();
        assertThat(definitionService.tenantIdDuringCall()).isEqualTo("7");
        assertThat(definitionService.lastCommand().getAdminUsers()).containsExactly("admin", "risk-manager");
        assertThat(definitionService.lastCommand().getStartEntryVisible()).isFalse();
        assertThat(definitionService.lastCommand().getDesignerJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(definitionService.lastCommand().getFormJson()).isEqualTo("{\"fields\":[]}");
        assertThat(stringValue("workflow_definition", "definition_name",
                "definition_key = 'WF_GUARANTEE_ORDER_DISCOUNT'")).isEqualTo("保函优惠审批");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("2");

        declaration.getFields().get("definitionName").setValue("保函优惠审批-更新");
        definitionHandler.upsert(declaration);

        assertThat(count("workflow_definition")).isOne();
        assertThat(stringValue("workflow_definition", "definition_name",
                "definition_key = 'WF_GUARANTEE_ORDER_DISCOUNT'")).isEqualTo("保函优惠审批-更新");

        definitionHandler.disable(declaration);

        assertThat(stringValue("workflow_definition", "status",
                "definition_key = 'WF_GUARANTEE_ORDER_DISCOUNT'"))
                .isEqualTo(WorkflowDefinitionStatus.DISABLED.name());

        definitionHandler.delete(declaration);

        assertThat(count("workflow_definition")).isZero();
    }

    private ResourceDeclaration categoryDeclaration(String categoryName, int status) {
        ResourceDeclaration declaration = declaration(ResourceTypes.WORKFLOW_CATEGORY,
                "2951300000000000001", "workflow.category.common", "通用流程分类");
        field(declaration, "categoryId", ResourceFieldType.LONG, 1L);
        field(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        field(declaration, "categoryCode", ResourceFieldType.STRING, "COMMON");
        field(declaration, "categoryName", ResourceFieldType.STRING, categoryName);
        field(declaration, "domainCode", ResourceFieldType.STRING, "COMMON");
        field(declaration, "sort", ResourceFieldType.INT, 1);
        field(declaration, "status", ResourceFieldType.INT, status);
        field(declaration, "remark", ResourceFieldType.STRING, "系统默认通用流程分类");
        return declaration;
    }

    private ResourceDeclaration templateCategoryDeclaration() {
        ResourceDeclaration declaration = declaration(ResourceTypes.WORKFLOW_TEMPLATE_CATEGORY,
                "2951300000000000101", "workflow.template-category.common", "通用流程模板分类");
        field(declaration, "categoryId", ResourceFieldType.LONG, 1L);
        field(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        field(declaration, "categoryCode", ResourceFieldType.STRING, "COMMON_TEMPLATE");
        field(declaration, "categoryName", ResourceFieldType.STRING, "通用模板");
        field(declaration, "icon", ResourceFieldType.STRING, "CollectionTag");
        field(declaration, "sort", ResourceFieldType.INT, 1);
        field(declaration, "status", ResourceFieldType.INT, 1);
        field(declaration, "remark", ResourceFieldType.STRING, "系统默认通用流程模板分类");
        return declaration;
    }

    private ResourceDeclaration workflowDefinitionDeclaration(String definitionName) {
        ResourceDeclaration declaration = declaration(ResourceTypes.WORKFLOW_DEFINITION,
                "2026070100010001", "guarantee.workflow.order-discount", definitionName);
        field(declaration, "tenantId", ResourceFieldType.LONG, 7L);
        field(declaration, "domainCode", ResourceFieldType.STRING, "guarantee");
        field(declaration, "categoryCode", ResourceFieldType.STRING, "GUARANTEE_APPLICATION");
        field(declaration, "categoryName", ResourceFieldType.STRING, "保函业务流程");
        field(declaration, "categorySort", ResourceFieldType.INT, 20);
        field(declaration, "orgId", ResourceFieldType.LONG, 1L);
        field(declaration, "adminUsers", ResourceFieldType.LIST, List.of("admin", "risk-manager"));
        field(declaration, "startEntryVisible", ResourceFieldType.BOOLEAN, false);
        field(declaration, "definitionKey", ResourceFieldType.STRING, "WF_GUARANTEE_ORDER_DISCOUNT");
        field(declaration, "definitionName", ResourceFieldType.STRING, definitionName);
        field(declaration, "designerJson", ResourceFieldType.JSON, Map.of("nodes", List.of()));
        fileField(declaration, "formJson", "classpath:workflow-definition-resource-handler-form.json");
        field(declaration, "remark", ResourceFieldType.STRING, "业务员申请优惠，部门领导审批，总经理审批。");
        return declaration;
    }

    private List<ResourceDeclaration> nodeDefinitionDeclarations() {
        return List.of(
                nodeDefinition("2951300000000001001", 360000001L, "ROOT", "ROOT", "发起人", "BASIC",
                        "基础节点", "流程发起节点，由系统自动创建", "startEvent", "NONE",
                        "#64748b", "User", "{}", 1),
                nodeDefinition("2951300000000001002", 360000002L, "APPROVAL", "APPROVAL", "审批节点", "BASIC",
                        "基础节点", "人工审批、会签、或签等人工处理节点", "userTask", "USER_TASK",
                        "#2563eb", "Stamp", "{\"assigneeType\":\"USER\"}", 10),
                nodeDefinition("2951300000000001003", 360000003L, "CC", "CC", "抄送节点", "BASIC",
                        "基础节点", "流程流转到此处时通知相关人员", "serviceTask", "EVENT_PUBLISH",
                        "#7c3aed", "Send", "{\"eventName\":\"workflow.cc\"}", 20),
                nodeDefinition("2951300000000001004", 360000004L, "EXCLUSIVE_GATEWAY", "EXCLUSIVE_GATEWAY",
                        "条件分支", "BASIC", "基础节点", "按条件选择一个分支继续流转",
                        "exclusiveGateway", "NONE", "#f59e0b", "GitBranch", "{}", 30),
                nodeDefinition("2951300000000001005", 360000005L, "PARALLEL_GATEWAY", "PARALLEL_GATEWAY",
                        "并行分支", "BASIC", "基础节点", "多个分支同时流转并在结束后合并",
                        "parallelGateway", "NONE", "#0f766e", "GitFork", "{}", 40),
                nodeDefinition("2951300000000001006", 360000006L, "SERVICE_BEAN", "SERVICE", "Bean服务任务",
                        "SERVICE", "服务节点", "调用白名单 Spring Bean 执行业务动作",
                        "serviceTask", "SPRING_BEAN", "#0891b2", "Box",
                        "{\"beanName\":\"\",\"methodName\":\"\"}", 50),
                nodeDefinition("2951300000000001007", 360000007L, "SERVICE_HTTP", "SERVICE", "HTTP服务任务",
                        "SERVICE", "服务节点", "调用受控 HTTP URL 执行业务动作",
                        "serviceTask", "HTTP_URL", "#dc2626", "Webhook",
                        "{\"url\":\"\",\"method\":\"POST\",\"timeoutMillis\":5000}", 60),
                nodeDefinition("2951300000000001008", 360000008L, "SERVICE_REMOTE", "SERVICE", "远程服务任务",
                        "SERVICE", "服务节点", "调用受控远程服务执行微服务动作",
                        "serviceTask", "REMOTE_SERVICE", "#ea580c", "Cloud",
                        "{\"serviceName\":\"\",\"operation\":\"\"}", 70),
                nodeDefinition("2951300000000001009", 360000009L, "EVENT_PUBLISH", "SERVICE", "事件发布任务",
                        "SERVICE", "服务节点", "发布流程事件，支持单体事件和后续消息总线扩展",
                        "serviceTask", "EVENT_PUBLISH", "#16a34a", "Radio",
                        "{\"eventName\":\"\"}", 80)
        );
    }

    private ResourceDeclaration nodeDefinition(String resourceId, Long nodeDefinitionId, String code, String type,
                                               String name, String categoryCode, String categoryName,
                                               String description, String bpmnType, String executionType,
                                               String color, String icon, String defaultProperties, int sort) {
        ResourceDeclaration declaration = declaration(ResourceTypes.WORKFLOW_NODE_DEFINITION, resourceId,
                "workflow.node." + code.toLowerCase().replace('_', '-'), name);
        field(declaration, "nodeDefinitionId", ResourceFieldType.LONG, nodeDefinitionId);
        field(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        field(declaration, "nodeDefinitionCode", ResourceFieldType.STRING, code);
        field(declaration, "nodeType", ResourceFieldType.STRING, type);
        field(declaration, "nodeName", ResourceFieldType.STRING, name);
        field(declaration, "categoryCode", ResourceFieldType.STRING, categoryCode);
        field(declaration, "categoryName", ResourceFieldType.STRING, categoryName);
        field(declaration, "description", ResourceFieldType.STRING, description);
        field(declaration, "bpmnType", ResourceFieldType.STRING, bpmnType);
        field(declaration, "executionType", ResourceFieldType.STRING, executionType);
        field(declaration, "color", ResourceFieldType.STRING, color);
        field(declaration, "icon", ResourceFieldType.STRING, icon);
        field(declaration, "defaultProperties", ResourceFieldType.JSON, defaultProperties);
        field(declaration, "sort", ResourceFieldType.INT, sort);
        field(declaration, "status", ResourceFieldType.INT, 1);
        return declaration;
    }

    private ResourceDeclaration declaration(String resourceType, String id, String bizKey, String name) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(resourceType);
        declaration.setModuleCode("workflow");
        declaration.setModuleName("工作流");
        declaration.setBizKey(bizKey);
        declaration.setName(name);
        declaration.setTargetModule("workflow");
        declaration.setSource("WorkflowResourceHandlerIntegrationTest");
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void fileField(ResourceDeclaration declaration, String name, String location) {
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.FILE);
        field.setLocation(location);
        field.setEncoding("UTF-8");
        field.setMediaType("application/json");
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists workflow_definition_version");
        execute("drop table if exists workflow_definition");
        execute("drop table if exists workflow_node_definition");
        execute("drop table if exists workflow_template_category");
        execute("drop table if exists workflow_category");
        execute("""
                create table workflow_category (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    category_name varchar(64) not null,
                    category_code varchar(64) not null,
                    domain_code varchar(64) not null default 'COMMON',
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_workflow_category_code (tenant_id, category_code)
                )
                """);
        execute("""
                create table workflow_template_category (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    parent_id bigint,
                    category_name varchar(64) not null,
                    category_code varchar(64) not null,
                    icon varchar(128),
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_workflow_template_category_code (tenant_id, category_code)
                )
                """);
        execute("""
                create table workflow_node_definition (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    node_definition_code varchar(64) not null,
                    node_type varchar(64) not null,
                    node_name varchar(64) not null,
                    category_code varchar(64) not null,
                    category_name varchar(64) not null,
                    description varchar(255),
                    bpmn_type varchar(64) not null,
                    execution_type varchar(64) not null,
                    color varchar(32),
                    icon varchar(64),
                    property_schema text,
                    default_properties text,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_workflow_node_definition_code (tenant_id, node_definition_code)
                )
                """);
        execute("""
                create table workflow_definition (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    category_id bigint not null default 0,
                    domain_code varchar(64) not null,
                    org_id bigint,
                    admin_users text,
                    start_entry_visible boolean not null default true,
                    icon varchar(512),
                    definition_name varchar(128) not null,
                    definition_key varchar(128) not null,
                    deployment_id varchar(128),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    published_version_no int,
                    source_template_id bigint,
                    source_template_code varchar(128),
                    source_template_version int,
                    designer_json text,
                    bpmn_xml text,
                    form_code varchar(128),
                    form_json text,
                    status varchar(32) not null,
                    last_deploy_time timestamp,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_workflow_definition_key (tenant_id, definition_key)
                )
                """);
        execute("""
                create table workflow_definition_version (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    definition_id bigint not null,
                    version_no int not null,
                    category_id bigint not null default 0,
                    domain_code varchar(64) not null,
                    org_id bigint,
                    admin_users text,
                    start_entry_visible boolean not null default true,
                    icon varchar(512),
                    definition_name varchar(128) not null,
                    definition_key varchar(128) not null,
                    remark varchar(255),
                    form_code varchar(128),
                    designer_json text,
                    form_json text,
                    bpmn_xml text,
                    deployment_id varchar(128),
                    process_definition_id varchar(128),
                    process_definition_version int,
                    publish_status varchar(32),
                    publish_message varchar(500),
                    created_by bigint,
                    publish_time timestamp,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id)
                )
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from " + tableName)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String stringValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int intValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    @Configuration
    @Import({
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            WorkflowCategoryResourceHandler.class,
            WorkflowTemplateCategoryResourceHandler.class,
            WorkflowNodeDefinitionResourceHandler.class,
            WorkflowDefinitionResourceHandler.class
    })
    @MapperScan(basePackageClasses = {
            WorkflowCategoryMapper.class,
            WorkflowTemplateCategoryMapper.class,
            WorkflowNodeDefinitionMapper.class,
            WorkflowDefinitionMapper.class,
            WorkflowDefinitionVersionMapper.class
    })
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CapturingWorkflowDefinitionService workflowDefinitionService(WorkflowDefinitionMapper definitionMapper) {
            return new CapturingWorkflowDefinitionService(definitionMapper);
        }
    }

    static class CapturingWorkflowDefinitionService implements IWorkflowDefinitionService {

        private final WorkflowDefinitionMapper definitionMapper;
        private EnsureWorkflowDefinitionCommand lastCommand;
        private String tenantIdDuringCall;

        CapturingWorkflowDefinitionService(WorkflowDefinitionMapper definitionMapper) {
            this.definitionMapper = definitionMapper;
        }

        EnsureWorkflowDefinitionCommand lastCommand() {
            return lastCommand;
        }

        String tenantIdDuringCall() {
            return tenantIdDuringCall;
        }

        @Override
        public R<WorkflowDeployVO> ensurePublished(EnsureWorkflowDefinitionCommand command) {
            lastCommand = command;
            tenantIdDuringCall = MangoContextHolder.tenantId();
            Long tenantId = Long.valueOf(tenantIdDuringCall);
            WorkflowDefinition definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
                    .eq(WorkflowDefinition::getTenantId, tenantId)
                    .eq(WorkflowDefinition::getDefinitionKey, command.getDefinitionKey())
                    .last("limit 1"));
            if (definition == null) {
                definition = new WorkflowDefinition();
                definition.setTenantId(tenantId);
                definition.setCategoryId(1L);
                definition.setDefinitionKey(command.getDefinitionKey());
            }
            definition.setDomainCode(command.getDomainCode());
            definition.setOrgId(command.getOrgId());
            definition.setStartEntryVisible(command.getStartEntryVisible());
            definition.setDefinitionName(command.getDefinitionName());
            definition.setDesignerJson(command.getDesignerJson());
            definition.setFormJson(command.getFormJson());
            definition.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
            if (definition.getId() == null) {
                definitionMapper.insert(definition);
            } else {
                definitionMapper.updateById(definition);
            }
            return R.ok(new WorkflowDeployVO());
        }

        @Override
        public R<PageResult<WorkflowDefinitionVO>> page(WorkflowDefinitionPageQuery query) {
            return unused();
        }

        @Override
        public R<WorkflowDefinitionVO> get(Long id) {
            return unused();
        }

        @Override
        public R<String> create(SaveWorkflowDefinitionCommand command) {
            return unused();
        }

        @Override
        public R<Boolean> update(SaveWorkflowDefinitionCommand command) {
            return unused();
        }

        @Override
        public R<Boolean> delete(Long id) {
            return unused();
        }

        @Override
        public R<Boolean> updateStatus(UpdateWorkflowDefinitionStatusCommand command) {
            return unused();
        }

        @Override
        public R<Boolean> discardDraft(Long id) {
            return unused();
        }

        @Override
        public R<WorkflowDeployVO> deploy(Long id) {
            return unused();
        }

        @Override
        public R<WorkflowDeployVO> deployInternal(Long id) {
            return unused();
        }

        @Override
        public R<List<WorkflowDefinitionVersionVO>> versions(WorkflowDefinitionVersionQuery query) {
            return unused();
        }

        @Override
        public R<WorkflowDefinitionVersionVO> versionDetail(Long id) {
            return unused();
        }

        @Override
        public R<List<WorkflowNodeCatalogVO>> nodeCatalog() {
            return unused();
        }

        private static <T> R<T> unused() {
            return R.fail("unused test service method");
        }
    }
}
