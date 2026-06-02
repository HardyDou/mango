package {{basePackage}}.{{modulePackage}}.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;

/**
 * {{aggregatePascal}}持久化实体。
 */
@TableName("{{moduleKebabSnake}}_{{aggregateKebabSnake}}")
public class {{aggregatePascal}}Entity extends TenantEntity {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
