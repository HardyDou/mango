package io.mango.home.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class SaveHomePageLayoutCommand implements Serializable {

    @NotNull(message = "首页ID不能为空")
    private Long id;

    @NotBlank(message = "layoutJson不能为空")
    @Size(max = 200000, message = "layoutJson长度不能超过200000")
    private String layoutJson;
}
