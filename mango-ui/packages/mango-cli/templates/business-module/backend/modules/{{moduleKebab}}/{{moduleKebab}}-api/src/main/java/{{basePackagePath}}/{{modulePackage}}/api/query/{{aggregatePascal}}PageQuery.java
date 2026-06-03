package {{basePackage}}.{{modulePackage}}.api.query;

import io.mango.common.po.PageQuery;
import io.mango.infra.persistence.api.crud.QueryField;
import io.mango.infra.persistence.api.crud.QueryType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {{aggregatePascal}}分页查询。
 */
@Schema(description = "{{aggregatePascal}}分页查询")
public class {{aggregatePascal}}PageQuery extends PageQuery {

    @Schema(description = "{{aggregatePascal}}名称")
    @QueryField(type = QueryType.LIKE)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
