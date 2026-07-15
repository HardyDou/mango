package io.mango.authorization.api.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * API 资源批量注册请求。
 *
 * <p>通过 Jackson 委托模式继续使用原有 JSON 数组协议，只收敛 Java API 的类型边界。</p>
 */
@Data
@NoArgsConstructor
@Schema(description = "API 资源批量注册请求")
public class ApiResourceRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    @NotEmpty
    @Size(max = 10000)
    @Schema(description = "API 资源注册命令列表")
    private List<ApiResourceRegisterCommand> resources = new ArrayList<>();

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ApiResourceRegisterRequest(List<ApiResourceRegisterCommand> resources) {
        this.resources = resources == null ? new ArrayList<>() : new ArrayList<>(resources);
    }

    @JsonValue
    public List<ApiResourceRegisterCommand> value() {
        return resources;
    }
}
