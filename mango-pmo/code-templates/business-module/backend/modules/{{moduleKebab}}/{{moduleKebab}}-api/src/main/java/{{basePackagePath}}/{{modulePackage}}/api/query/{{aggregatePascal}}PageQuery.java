package {{basePackage}}.{{modulePackage}}.api.query;

import io.mango.common.po.PageQuery;
import io.mango.infra.persistence.api.crud.QueryField;
import io.mango.infra.persistence.api.crud.QueryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * {{aggregateName}}分页查询。
 */
@Schema(description = "{{aggregateName}}分页查询")
public class {{aggregatePascal}}PageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "{{aggregateName}}名称")
    @QueryField(type = QueryType.LIKE)
    @Size(max = 100, message = "{{aggregateName}}名称不能超过100个字符")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof {{aggregatePascal}}PageQuery)) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }
        {{aggregatePascal}}PageQuery that = ({{aggregatePascal}}PageQuery) object;
        return Objects.equals(name, that.name);
    }

    @Override
    protected boolean canEqual(Object object) {
        return object instanceof {{aggregatePascal}}PageQuery;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
