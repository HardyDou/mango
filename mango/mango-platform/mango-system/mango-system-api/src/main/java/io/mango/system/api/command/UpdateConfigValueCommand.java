package io.mango.system.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateConfigValueCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "配置ID不能为空")
    private Long id;

    private String value;
}
