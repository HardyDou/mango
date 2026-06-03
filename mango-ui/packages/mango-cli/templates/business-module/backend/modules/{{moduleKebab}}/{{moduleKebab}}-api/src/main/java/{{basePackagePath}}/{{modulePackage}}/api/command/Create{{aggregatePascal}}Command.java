package {{basePackage}}.{{modulePackage}}.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 创建{{aggregatePascal}}命令。
 */
@Schema(description = "创建{{aggregatePascal}}命令")
public class Create{{aggregatePascal}}Command implements Serializable {

    @Schema(description = "{{aggregatePascal}}名称")
    @NotBlank(message = "{{aggregatePascal}}名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
