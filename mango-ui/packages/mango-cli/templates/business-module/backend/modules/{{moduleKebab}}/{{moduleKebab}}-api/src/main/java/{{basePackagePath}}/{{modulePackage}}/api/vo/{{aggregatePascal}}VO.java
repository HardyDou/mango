package {{basePackage}}.{{modulePackage}}.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * {{aggregateName}}返回对象。
 */
@Schema(description = "{{aggregateName}}返回对象")
public class {{aggregatePascal}}VO implements Serializable {

    @Schema(description = "业务标识")
    private String id;

    @Schema(description = "{{aggregateName}}名称")
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
