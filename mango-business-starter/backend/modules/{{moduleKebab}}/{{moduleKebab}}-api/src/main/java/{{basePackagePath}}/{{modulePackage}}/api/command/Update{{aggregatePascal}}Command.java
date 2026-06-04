package {{basePackage}}.{{modulePackage}}.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 修改{{aggregateName}}命令。
 */
@Schema(description = "修改{{aggregateName}}命令")
public class Update{{aggregatePascal}}Command implements Serializable {

    @Schema(description = "业务标识")
    @NotNull(message = "业务标识不能为空")
    private Long id;

    @Schema(description = "{{aggregateName}}名称")
    @NotBlank(message = "{{aggregateName}}名称不能为空")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
