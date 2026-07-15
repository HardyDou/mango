package io.mango.numgen.api.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "生成单个编号命令")
public class NumgenNextCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "编号规则键不能为空")
    @Schema(description = "编号规则键")
    private String genKey;

    @NotNull(message = "动态参数不能为空")
    @Size(max = 50, message = "动态参数不能超过50项")
    @Valid
    @JsonIgnore
    @Schema(description = "内部动态参数项", hidden = true)
    private List<NumgenParameterCommand> parameterEntries = new ArrayList<>();

    @JsonProperty("params")
    public Map<String, Object> getParams() {
        return NumgenParameterCommand.mutableMap(parameterEntries);
    }

    @JsonProperty("params")
    public void setParams(Map<String, Object> params) {
        parameterEntries = new ArrayList<>();
        if (params != null) {
            params.forEach((key, value) -> parameterEntries.add(
                    new NumgenParameterCommand(key, value == null ? null : String.valueOf(value))));
        }
    }
}
