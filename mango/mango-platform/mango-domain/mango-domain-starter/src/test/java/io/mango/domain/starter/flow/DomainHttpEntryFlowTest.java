package io.mango.domain.starter.flow;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("flow")
@Tag("domain")
@SpringBootTest(
        classes = DomainHttpEntryFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:domain_http;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.enabled=false",
                "mango.persistence.mybatis-plus.tenant.enabled=false",
                "mybatis-plus.mapper-locations=classpath:/mapper/domain/*.xml"
        })
class DomainHttpEntryFlowTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void rebuildSchema() {
        jdbcTemplate.execute("drop table if exists biz_domain");
        jdbcTemplate.execute("""
                create table biz_domain (
                    id bigint primary key,
                    tenant_id varchar(64) not null default '1',
                    org_id bigint,
                    domain_code varchar(64) not null,
                    domain_short_code varchar(64) not null,
                    domain_name varchar(128) not null,
                    parent_id bigint not null default 0,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(512) not null default '',
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    deleted tinyint not null default 0
                )
                """);
        jdbcTemplate.execute("create unique index uk_biz_domain_tenant_code on biz_domain(tenant_id, domain_code)");
        jdbcTemplate.execute(
                "create unique index uk_biz_domain_tenant_short_code on biz_domain(tenant_id, domain_short_code)");
    }

    @Test
    void crudLifecycle_真实入口保持分页树详情启停删除契约() {
        String parentId = create("flow-parent", "FLOWP", "流程父域", null, 1);
        String childId = create("child", "FLOWC", "流程子域", parentId, 1);

        JsonNode detail = success(get("/domain/domains/detail?id=" + childId));
        assertThat(detail.path("domainCode").asText()).isEqualTo("FLOW_PARENT_CHILD");
        assertThat(detail.path("parentName").asText()).isEqualTo("流程父域");

        JsonNode page = success(get("/domain/domains/page?domainCode=flow-parent&page=1&size=10"));
        assertThat(page.path("list")).hasSize(2);
        assertThat(page.path("total").asLong()).isEqualTo(2);

        JsonNode tree = success(get("/domain/domains/tree"));
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).path("children")).hasSize(1);

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("id", childId);
        update.put("domainShortCode", "FLOWC2");
        update.put("domainName", "流程子域-更新");
        update.put("sort", 2);
        update.put("status", 1);
        update.put("remark", "updated");
        assertThat(success(put("/domain/domains", update)).asBoolean()).isTrue();

        assertThat(success(put("/domain/domains/status", Map.of("id", childId, "status", 0))).asBoolean())
                .isTrue();
        JsonNode enabledTree = success(get("/domain/domains/enabled-tree"));
        assertThat(enabledTree.get(0).path("children")).isEmpty();

        JsonNode parentDeleteFailure = failure(delete("/domain/domains?id=" + parentId));
        assertThat(parentDeleteFailure.path("msg").asText()).isEqualTo("存在子业务域，不能删除");
        assertThat(success(delete("/domain/domains?id=" + childId)).asBoolean()).isTrue();
        assertThat(success(delete("/domain/domains?id=" + parentId)).asBoolean()).isTrue();
        assertThat(jdbcTemplate.queryForObject("select count(*) from biz_domain where deleted = 0", Long.class))
                .isZero();
    }

    @Test
    void create_规范化编码默认字段并拒绝重复编码() {
        String id = create("sales-order", "so", "销售订单", null, null);

        JsonNode created = success(get("/domain/domains/code?domainCode=sales-order"));
        assertThat(created.path("id").asText()).isEqualTo(id);
        assertThat(created.path("domainCode").asText()).isEqualTo("SALES_ORDER");
        assertThat(created.path("domainShortCode").asText()).isEqualTo("SO");
        assertThat(created.path("status").asInt()).isEqualTo(1);
        assertThat(created.path("sort").asInt()).isZero();

        JsonNode duplicateCode = failure(post("/domain/domains",
                createPayload("SALES_ORDER", "SO2", "重复编码", null, 1)));
        assertThat(duplicateCode.path("msg").asText()).isEqualTo("业务域编码已存在");

        JsonNode duplicateShortCode = failure(post("/domain/domains",
                createPayload("OTHER", "so", "重复简写", null, 1)));
        assertThat(duplicateShortCode.path("msg").asText()).isEqualTo("业务域简写已存在");
    }

    @Test
    void validation_空名称与非法状态不写入数据库() {
        Map<String, Object> blankName = createPayload("INVALID_NAME", "IN", " ", null, 1);
        failure(post("/domain/domains", blankName));

        Map<String, Object> invalidStatus = createPayload("INVALID_STATUS", "IS", "非法状态", null, 2);
        assertThat(failure(post("/domain/domains", invalidStatus)).path("msg").asText())
                .isEqualTo("业务域状态非法");
        assertThat(jdbcTemplate.queryForObject("select count(*) from biz_domain", Long.class)).isZero();
    }

    private String create(String code, String shortCode, String name, String parentId, Integer status) {
        return success(post("/domain/domains", createPayload(code, shortCode, name, parentId, status))).asText();
    }

    private Map<String, Object> createPayload(
            String code, String shortCode, String name, String parentId, Integer status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("domainCode", code);
        payload.put("domainShortCode", shortCode);
        payload.put("domainName", name);
        payload.put("parentId", parentId == null ? 0 : parentId);
        payload.put("sort", null);
        payload.put("status", status);
        payload.put("remark", null);
        return payload;
    }

    private JsonNode get(String path) {
        return exchange(path, HttpMethod.GET, null).getBody();
    }

    private JsonNode post(String path, Object body) {
        return exchange(path, HttpMethod.POST, body).getBody();
    }

    private JsonNode put(String path, Object body) {
        return exchange(path, HttpMethod.PUT, body).getBody();
    }

    private JsonNode delete(String path) {
        return exchange(path, HttpMethod.DELETE, null).getBody();
    }

    private ResponseEntity<JsonNode> exchange(String path, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Mango-Tenant-Id", "1");
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                path, method, new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(response.getBody()).isNotNull();
        return response;
    }

    private JsonNode success(JsonNode response) {
        assertThat(response.path("success").asBoolean()).isTrue();
        assertThat(response.path("code").asInt()).isEqualTo(200);
        return response.path("data");
    }

    private JsonNode failure(JsonNode response) {
        assertThat(response.path("success").asBoolean()).isFalse();
        assertThat(response.path("code").asInt()).isEqualTo(400);
        return response;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
